package com.tumanako.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;

import com.tumanako.dash.DashData;
import com.tumanako.dash.R;
import com.tumanako.sensors.Accelerometer;
import com.tumanako.sensors.VehicleDataBt;
import com.tumanako.sensors.NmeaGPS;
import com.tumanako.sensors.NmeaProcessor;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



public class UIActivity extends Activity implements OnClickListener, OnLongClickListener
    {
    
    
    // UI Widgets: 
    private StatusLamp     lampData;
    private StatusLamp     lampGPS;
    private StatusLamp     lampContactor;
    private StatusLamp     lampFault;
    private StatusLamp     lampGreenGlobe;

    private Dial           dialMotorRPM;
    
    //private TextWithLabel  textMotorRPM;
    private TextWithLabel  textMainBatteryKWh;
    private TextWithLabel  textAccBatteryVlts;
    private TextWithLabel  textTMotor;
    private TextWithLabel  textTController;
    private TextWithLabel  textTBattery;
   
    
    // Sensors:  
    //private Accelerometer  mAcceleration;
    private NmeaGPS        mGPS;
    private VehicleDataBt  mVehicleDataSensor;

    // UI Timer Handler: 
    private Handler uiTimer = new Handler(); 
    
    
    //*** Vehicle Data Store: ***
    private DashData vehicleData = new DashData();   // Create a DashData object to hold data from the vehicle. 
    
    // Persistent Details: 
    // These get saved when the application goes to the background, 
    // so that they can be reused on resume: 
    private double totalEnergy = 0.0;       // Energy Used kWh          } Since last reset
    private double totalDistance = 0.0;     // Distance Travelled (km)  } 

    //private long   lastRecordTime = 0;
    //private String outputFileName = "";

    private static final int UI_UPDATE_EVERY = 500;   // Update the UI every n mSeconds.
      
    //*** Message types recognised by this class: ***
    public static final int UI_TOAST_MESSAGE = 1;     // Sent by another class when they have a brief message they would like displayed.  

    
    // ******** Constants for Log File - Not used at the moment. **************************
    //private static final String SAVE_PATH = "/mnt/sdcard/EnergyLogger/";
    //private static final long CLOSE_TRIP_AFTER = 300000;   // Start a new trip file if the last record we had was more than this many milliseconds ago
    //                                                       // (NOTE: Only applies when the app is closed and reopened). 
    
    private static final String PREFS_NAME = "EnergyLoggerPrefs";
    
    
    // *** Create: Called when the activity is first created: ****
    @Override
    public void onCreate(Bundle savedInstanceState) 
      {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      // Obtain handles to UI objects:
      lampData             = (StatusLamp) findViewById(R.id.lampData);
      lampGPS              = (StatusLamp) findViewById(R.id.lampGPS);
      lampContactor        = (StatusLamp) findViewById(R.id.lampContactor);
      lampFault            = (StatusLamp) findViewById(R.id.lampFault);
      lampGreenGlobe = (StatusLamp) findViewById(R.id.lampGreenGlobe);
      
      dialMotorRPM         = (Dial) findViewById(R.id.demoDial);
      //textMotorRPM         = (TextWithLabel) findViewById(R.id.textMotorRPM);
      textMainBatteryKWh   = (TextWithLabel) findViewById(R.id.textMainBatteryKWh);
      textAccBatteryVlts   = (TextWithLabel) findViewById(R.id.textAccBatteryVlts);
      textTMotor           = (TextWithLabel) findViewById(R.id.textTMotor);
      textTController      = (TextWithLabel) findViewById(R.id.textTController);
      textTBattery         = (TextWithLabel) findViewById(R.id.textTBattery);

      //textMotorRPM.setLabel("RPM");
      
      //dialMotorRPM.setupDial( 0, 1000, 6, -0.698f, 0.698f,0.5f,0.9f,0.4f );
      dialMotorRPM.setupDial( 0, 1000, 5, -1.57f, 0.89f, 0.5f, 0.85f, 0.4f, "RPM", 0.6f, 0.6f );
      
      textMainBatteryKWh.setLabel("kWh");
      textAccBatteryVlts.setLabel("Vlt");
      textTMotor.setLabel("°C");
      textTController.setLabel("°C");
      textTBattery.setLabel("°C");
      
      // Attach buttons to general onClick event handler (see below):
      //( (Button) findViewById(R.id.buttonClose ) ).setOnClickListener(this);      
        
      // Add Long Press events to the Total Distance and Total Fuel boxes. This will be used to reset the values. 
      //textDistanceIndicator.setLongClickable(true);

      // -- Set initial state of status lamps: ---
      // Note: Turn ON fault to mimic behaviour of traditional cars (i.e. bulb check)... 
      lampFault.turnOn();
      
      // -- Set some default values in text boxes: --
      //textMotorRPM.setText("0");
      textMainBatteryKWh.setText("0.0");
      textAccBatteryVlts.setText("0.0");
      textTMotor.setText("0.0");
      textTController.setText("0.0");
      textTBattery.setText("0.0");
      
      
      // -- Create Sensors: -- 
      //mAcceleration = new Accelerometer(this, uiMessageHandler);
      mGPS               = new NmeaGPS(this, uiMessageHandler);
      mVehicleDataSensor = new VehicleDataBt(uiMessageHandler);
      
      // -------- Restore Saved Preferences (if any): -----------------------------
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      totalEnergy    = 25.0;  //settings.getFloat( "totalEnergy", 0 );
      totalDistance  = settings.getFloat( "totalDistance", 0 );
      /***** Older code not used right now:
      lastRecordTime = settings.getLong( "lastRecordTime", 0 );
      float gravityX = settings.getFloat( "gravityX", 0 );
      float gravityY = settings.getFloat( "gravityY", 0 );
      float gravityZ = settings.getFloat( "gravityZ", 0 );
      mAcceleration.setGravityXYZ(new float[] {gravityX,gravityY,gravityZ});
      ***/
      // --------------------------------------------------------------------------
      

      /****** Log File Code: Not used right now... ***************
      // Check file name for output file. If we don't have one, (i.e. just started), or it's
      // out of date (i.e. new trip), create a new one: 
      if (  (outputFileName.length() == 0) || 
            ( (lastRecordTime + CLOSE_TRIP_AFTER) < System.currentTimeMillis() )   )
        {
        // Need to generate a new file name:
        SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
        outputFileName = SAVE_PATH + 
                         "TripData_" + 
                         myDateFormat.format(new Date(System.currentTimeMillis())) + 
                         ".csv";
        File testFile = new File(outputFileName);
        if(!testFile.exists()) WriteHeader("");    // If this is a new output file, add a header!
        }
      ***********************************************************/
      
      // -- Start Sensor Measurements: --
      StartSensors();
      }

    
    
    //------------- Create the options menu: -------------------------------------
    // This creates an Options menu from an XML file: 
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
      {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.options_menu, menu);
      return true;
      }
    //---------------------------------------------------------------------------    
    
    
    
    
    public void onStop()
      {
      super.onStop();
      SaveSettings();
      }
        

    
    private void SaveSettings()
      {
      // Save a copy of some data to the Preferences:
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putFloat( "totalEnergy", (float)totalEnergy );
      editor.putFloat( "totalDistance", (float)totalDistance );
      /******** Not Used: *******
      editor.putLong( "lastRecordTime", lastRecordTime );
      float[] thisGravity = mAcceleration.getGravityXYZ(); 
      editor.putFloat( "gravityX", thisGravity[0] );
      editor.putFloat( "gravityY", thisGravity[1] );
      editor.putFloat( "gravityZ", thisGravity[2] );
      ***************************/
      editor.commit();        // Commit the edits!
      }
    
    
    
    /***** Config Change: *********************************
     * This would be called on config change such as screen rotation. 
     * However in the manifest, we've specified that the screen rotation
     * is locked to 'Portrait' so that doesn't happen anyway. 
     * TO DO: Design UI to handle different screen rotations in a 
     * nice way! 
     *****************************************************/
    public void onConfigurationChanged(Configuration newConfig)
      {  super.onConfigurationChanged(newConfig);  } 
    

    
    
    /*************** Click Action Handler: *****************************/  
    public void onClick(View MyView) 
      { 
      // Process button click events for this activity: 
      //  This general OnClick handler is called for all the buttons. 
      //  The code checks the ID of the view which generated the event
      //  (i.e. the button) and takes the appropriate action.  
      switch (MyView.getId())
        {
        //case R.id.buttonCal:
        //  this.ShowMessage("Calibrating...");
        //  mAcceleration.measureGravity();
        //  break;
        //case R.id.buttonClose:
        //  finish();
        //  break;
        }
      } 
    /*************** LOOOONG Click Action Handler: ***************************** 
     * @return 
     ***************************************************************************/
    public boolean onLongClick(View MyView)
      {
      // Process long press events for this activity:
      // (Reset trip data) 
      switch (MyView.getId())
        {
      /****
        case R.id.textviewDistIndicator:
        case R.id.textviewFuelIndicator:
          totalEnergy    = 0;
          totalDistance  = 0;
          break;
       ***/          
        }
      return true;      
      }
    /*************** Menu Click Action Handler: ***************************** 
     * @return 
     ***************************************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
      {
      // Handle item selection
      switch (item.getItemId()) 
        {
        case R.id.menuitemDemoMode:
          ShowMessage("Demo!");
          mVehicleDataSensor.setDemo(true);
          mGPS.NMEAData.setDemo(true);
          return true;
        case R.id.menuitemClose:
          finish();
          return true;
        default:
          return super.onOptionsItemSelected(item);
        }
      }
    //---------------------------------------------------------------------------
    
    
    
    

    
    
        
    
    // ****************** Message Handler: **********************************************************
    // This function receives and processes messages from other classes. 
    private final Handler uiMessageHandler = new Handler() 
      {
      @Override
      public void handleMessage(Message msg) 
        {
        switch (msg.what) 
          {
          case UI_TOAST_MESSAGE:
            // General request to display a pop up toast message for the user:
            // UI Messages now display with Toast!
            ShowMessage( msg.obj.toString() );
            break;
        
          case NmeaProcessor.NMEA_PROCESSOR_DATA_UPDATED:
            // Called by the NmeaGPS object mGPS when updated NMEAData data are available.
            // Get the latest gps speed:  
            vehicleData.setField( "Speed", mGPS.NMEAData.getSpeed() );
            break;

          /****** Data Messages from vehicle data input: **********************************************************/            
          //case VehicleDataBt.DATA_MOTOR_RPM:         textMotorRPM.setText(        String.format("%.0f", msg.obj) );   break;
          case VehicleDataBt.DATA_MOTOR_RPM:         dialMotorRPM.setNeedle((Float)msg.obj);                          break;
          case VehicleDataBt.DATA_MAIN_BATTERY_KWH:  textMainBatteryKWh.setText(  String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_ACC_BATTERY_VLT:   textAccBatteryVlts.setText(  String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_MOTOR_TEMP:        textTMotor.setText(          String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_CONTROLLER_TEMP:   textTController.setText(     String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_MAIN_BATTERY_TEMP: textTBattery.setText(        String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_CONTACTOR_ON:
            if (msg.obj.equals(true)) lampContactor.turnOn();
            else                   lampContactor.turnOff();
            break;  
          case VehicleDataBt.DATA_FAULT:
            if (msg.obj.equals(true)) lampFault.turnOn();
            else                   lampFault.turnOff();
            break;            
          case VehicleDataBt.DATA_PRECHARGE:
            if (msg.obj.equals(true)) lampGreenGlobe.turnOn();
            else                   lampGreenGlobe.turnOff();
            break;  
          /********************************************************************************************************/
          
          
          case VehicleDataBt.VEHICLE_DATA_UPDATED:
            // New data on vehicle connection:
            // Update running summary:
            //totalEnergy = totalEnergy + (thisEnergy / 1000);         // Total energy used (kWh)
            //totalDistance = totalDistance + (currentV / 1000);       // Total distance in km
            //if (totalEnergy > 0) avgEconomy = (totalDistance / totalEnergy);
            break;
            
          case VehicleDataBt.VEHICLE_DATA_ERROR:
            // Vehicle Data Connection Error. For now, just ignore...
            //ShowMessage( msg.obj.toString() );
            break;

         /************ Not used: 
            case Accelerometer.ACCEL_GRAVITY_DONE:
              // Gravity calibration finished. 
              ShowMessage("Calibration Finished!");
              break;
              
            case Accelerometer.ACCEL_DATA_UPDATED:
              //AddMessage("Accel Data Rec!");
              break;
          **************************************/
          
            
          }  // [switch]
        }  // [handleMessage(...)] 
      };
   // **********************************************************************************************
   
      
      
      
      

    /**** UI Timer Handling Runnable: *******************
     * This runnable creates a timer to update the UI.
     * Note that this is a low priority UI update for GPS 
     * and connection status. UI is also updated when
     * messages are received from vehicle connection!
     ****************************************************/ 
    private Runnable uiTimerTask = new Runnable() 
     {
     // Creates a Runnable which will be called after a delay, to 
     // update the UI:
     public void run()  
       {
       // Set status indicators for NMEAData and Vehicle Connection:
       if (mGPS.NMEAData.isFixGood())    lampGPS.turnOn();
       else                              lampGPS.turnOff();
       if (mVehicleDataSensor.isRunning()) lampData.turnOn();
       else
           {
           lampData.turnOff();
           mVehicleDataSensor.resume();                           // Attempt to restart the connection to the vehicle. 
           }
       
       // Start the timer for next UI Uodate:     
       uiTimer.removeCallbacks(uiTimerTask);               // ...Make sure there is no active callback already....
       uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);  // ...Callback later!
       } 
     };
   
     
     
     
     private void StopSensors()
       {
       // Stop the sensors from generating data: 
       //if (mAcceleration != null) mAcceleration.suspend();
       if (mGPS != null) mGPS.suspend();      
       if (mVehicleDataSensor != null) mVehicleDataSensor.suspend();
       }
     
     private void StartSensors()
       {
       // Start generating sensor data: 
       //if (mAcceleration != null) mAcceleration.resume();
       if (mGPS != null) mGPS.resume();
       if (mVehicleDataSensor != null) mVehicleDataSensor.resume();       
       }
     
     
     
    // ********** UI Pause / Resume Events: ****************************************************
    @Override
    protected void onResume() 
      {
      super.onResume();
      StartSensors();
      uiTimer.removeCallbacks(uiTimerTask);                // ...Make sure there is no active callback already....
      uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);   // ...Callback in 0.2 seconds!
      }

    @Override
    protected void onPause() 
      {
      super.onPause();
      StopSensors();
      uiTimer.removeCallbacks(uiTimerTask);      // ...Make sure there is no active callback already....
      }



    // ******** Toast Message Method: ********************
    private void ShowMessage(String ThisMessage)
      {
      // *** Displays pop-up TOAST message: **
      Toast.makeText(getApplicationContext(), ThisMessage, Toast.LENGTH_SHORT).show();
      }
    
    
    
    

    

}  // [class]

