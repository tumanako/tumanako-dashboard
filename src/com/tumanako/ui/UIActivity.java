package com.tumanako.ui;

/************************************************************************************
 Tumanako - Electric Vehicle and Motor control software
 
 Copyright (C) 2012 Jeremy Cole-Baker <jeremy@rhtech.co.nz>

 This file is part of Tumanako Dashboard.

 Tumanako is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Tumanako is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Tumanako.  If not, see <http://www.gnu.org/licenses/>.
 
*************************************************************************************/


import java.util.HashMap;
import com.tumanako.dash.R;
import com.tumanako.sensors.VehicleDataBt;
import com.tumanako.sensors.NmeaGPS;
import com.tumanako.sensors.NmeaProcessor;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;



public class UIActivity extends Activity implements OnClickListener, OnLongClickListener
    {
    
    public static final String APP_TAG = "TumanakoDash";

    // HashMap for list of UI Widgets: 
    private HashMap<String,View> uiWidgets = new HashMap<String,View>();
    
    // Sensors:  
    private NmeaGPS        mGPS;
    private VehicleDataBt  mVehicleDataSensor;

    // UI Timer Handler: 
    private Handler uiTimer = new Handler(); 
    
    
    //*** Vehicle Data Store: ***
    //private DashData vehicleData = new DashData();   // Create a DashData object to hold data from the vehicle. 
    
    // Persistent Details: 
    // These get saved when the application goes to the background, 
    // so that they can be reused on resume: 
    private double totalEnergy = 0.0;       // Energy Used kWh          } Since last reset
    private double totalDistance = 0.0;     // Distance Travelled (km)  } 


    private static final int UI_UPDATE_EVERY = 500;   // Update the UI every n mSeconds.
      
    public static final int UI_TOAST_MESSAGE = 1;     // Sent by another class when they have a brief message they would like displayed.  

    private static final String PREFS_NAME = "EnergyLoggerPrefs";
    
    
    // ---------------DEMO MODE CODE -------------------------------
    private boolean isDemo = false;  // Demo mode flag!
    // ---------------DEMO MODE CODE -------------------------------  

    
    
    
    // *** Create: Called when the activity is first created: ****
    @Override
    public void onCreate(Bundle savedInstanceState) 
      {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      // --DEBUG!!--
      Log.i(APP_TAG,"UIActivity -> onCreate()");
           
      // Make a list of available UI widgets:
      uiWidgets.put( "lampData",           findViewById(R.id.lampData)           );
      uiWidgets.put( "lampGPS",            findViewById(R.id.lampGPS)            );
      uiWidgets.put( "lampContactor",      findViewById(R.id.lampContactor)      );
      uiWidgets.put( "lampFault",          findViewById(R.id.lampFault)          );
      uiWidgets.put( "lampGreenGlobe",     findViewById(R.id.lampGreenGlobe)     );
      uiWidgets.put( "dialMotorRPM",       findViewById(R.id.dialMotorRPM)       );
      uiWidgets.put( "dialMainBatteryKWh", findViewById(R.id.dialMainBatteryKWh) );
      uiWidgets.put( "textAccBatteryVlts", findViewById(R.id.textAccBatteryVlts) );
      uiWidgets.put( "textTMotor",         findViewById(R.id.textTMotor)         );
      uiWidgets.put( "textTController",    findViewById(R.id.textTController)    );
      uiWidgets.put( "textTBattery",       findViewById(R.id.textTBattery)       );

      
      // --------- Create Sensors: ---------------------------------- 
      mGPS               = new NmeaGPS(this, uiMessageHandler);
      mVehicleDataSensor = new VehicleDataBt(uiMessageHandler);
      
      
      // -------- Restore Saved Preferences (if any): -------------------
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      totalEnergy    = 25.0;  //settings.getFloat( "totalEnergy", 0 );
      totalDistance  = settings.getFloat( "totalDistance", 0 );
      
      
      // ---------------DEMO MODE CODE -------------------------------
      isDemo = settings.getBoolean("isDemo", false);
      // ---------------DEMO MODE CODE -------------------------------
      
      
      // -- Start Sensor Measurements: --
      StartSensors();

      
      // ---------------DEMO MODE CODE -------------------------------
      if (isDemo) startDemo();
      // ---------------DEMO MODE CODE -------------------------------
      
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
      
      // ---------------DEMO MODE CODE -------------------------------
      editor.putBoolean("isDemo", isDemo);
      // ---------------DEMO MODE CODE -------------------------------
      
      editor.commit();        // Commit the edits!
      }
    
    
    // ---------------DEMO MODE CODE -------------------------------
    private void startDemo()
      {
      isDemo = true;
      mVehicleDataSensor.setDemo(true);
      mGPS.NMEAData.setDemo(true);
      }
    private void stopDemo()
      {
      isDemo = false;
      mVehicleDataSensor.setDemo(false);
      mGPS.NMEAData.setDemo(false);
      }
    // ---------------DEMO MODE CODE -------------------------------
    
    
    
    
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
        
        // ---------------DEMO MODE CODE -------------------------------        
        case R.id.menuitemDemoMode:
          ShowMessage("Demo!");
          startDemo();
          return true;
        case R.id.menuitemStopDemo:
          ShowMessage("Stop Demo!");
          stopDemo();
          return true;
        // ---------------DEMO MODE CODE -------------------------------          
          
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
            //vehicleData.setField( "Speed", mGPS.NMEAData.getSpeed() );
            break;

          /****** Data Messages from vehicle data input: **********************************************************/            
          case VehicleDataBt.DATA_MOTOR_RPM:         ((Dial)uiWidgets.get("dialMotorRPM")).setNeedle((Float)msg.obj / 1000);                             break;
          case VehicleDataBt.DATA_MAIN_BATTERY_KWH:  ((Dial)uiWidgets.get("dialMainBatteryKWh") ).setNeedle((Float)msg.obj);                             break;
          case VehicleDataBt.DATA_ACC_BATTERY_VLT:   ((TextWithLabel)uiWidgets.get("textAccBatteryVlts") ).setText(  String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_MOTOR_TEMP:        ((TextWithLabel)uiWidgets.get("textTMotor")         ).setText(  String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_CONTROLLER_TEMP:   ((TextWithLabel)uiWidgets.get("textTController")    ).setText(  String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_MAIN_BATTERY_TEMP: ((TextWithLabel)uiWidgets.get("textTBattery")       ).setText(  String.format("%.1f", msg.obj) );   break;
          case VehicleDataBt.DATA_CONTACTOR_ON:
            if (msg.obj.equals(true)) ((StatusLamp)uiWidgets.get("lampContactor")).turnOn();
            else                      ((StatusLamp)uiWidgets.get("lampContactor")).turnOff();
            break;  
          case VehicleDataBt.DATA_FAULT:
            if (msg.obj.equals(true)) ((StatusLamp)uiWidgets.get("lampFault")).turnOn();
            else                      ((StatusLamp)uiWidgets.get("lampFault")).turnOff();
            break;            
          case VehicleDataBt.DATA_PRECHARGE:
            if (msg.obj.equals(true)) ((StatusLamp)uiWidgets.get("lampGreenGlobe")).turnOn();
            else                      ((StatusLamp)uiWidgets.get("lampGreenGlobe")).turnOff();
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
       if (mGPS.NMEAData.isFixGood())    ((StatusLamp)uiWidgets.get("lampGPS")).turnOn();
       else                              ((StatusLamp)uiWidgets.get("lampGPS")).turnOff();
       
       if (mVehicleDataSensor.isRunning()) ((StatusLamp)uiWidgets.get("lampData")).turnOn();
       else
           {
           ((StatusLamp)uiWidgets.get("lampData")).turnOff();
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

