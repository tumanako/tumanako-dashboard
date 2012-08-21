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

import com.tumanako.sensors.DataService;
import com.tumanako.sensors.IDroidSensor;
import com.tumanako.sensors.VehicleData;
import com.tumanako.sensors.NmeaProcessor;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

/*********************************************************
 * Main UI Activity: 
 *  -Displays the UI! 
 *   
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ********************************************************/

public class UIActivity extends Activity implements OnClickListener, OnLongClickListener   //, ServiceConnection
    {
    
    public static final String APP_TAG = "TumanakoDash";

    // HashMap for list of UI Widgets: 
    private HashMap<String,View> uiWidgets = new HashMap<String,View>();
    
    // *** Things for Data Input Service: ***
    private Intent       dataIntent;

    // Message Broadcaster to send Tntents to data service: 
    private LocalBroadcastManager messageBroadcaster;    


    
    // UI Timer Handler: 
    // We'll create a timer to update the UI occasionally:
    private Handler uiTimer = new Handler(); 
    
       
    // Persistent Details: 
    // These get saved when the application goes to the background, 
    // so that they can be reused on resume: 
    private double totalEnergy = 0.0;       // Energy Used kWh          } Since last reset
    private double totalDistance = 0.0;     // Distance Travelled (km)  } 


    private static final int UI_UPDATE_EVERY = 500;   // Update the UI every n mSeconds.
      
    public static final int UI_TOAST_MESSAGE = 1;     // Sent by another class when they have a brief message they would like displayed.  

    private static final String PREFS_NAME = "TumanakoDashPrefs";
    
    
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

      
      
      // ---- Create a Data Service intent: ------
      dataIntent = new Intent(this, com.tumanako.sensors.DataService.class);
      
      messageBroadcaster = LocalBroadcastManager.getInstance(this);  
      // Get a Broadcast Manager so we can send out messages to other parts of the app.

      
      // -------- Restore Saved Preferences (if any): -------------------
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      totalEnergy    = 25.0;  //settings.getFloat( "totalEnergy", 0 );
      totalDistance  = settings.getFloat( "totalDistance", 0 );
      
      // ---------------DEMO MODE CODE -------------------------------
      isDemo = false;  // settings.getBoolean("isDemo", false);
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
      // Send 'DEMO' intent to data service: 
      Intent intent = new Intent(DataService.DATA_SERVICE_DEMO);
      intent.putExtra(DataService.SERVICE_DEMO_SETTO,true);
      messageBroadcaster.sendBroadcast(intent);
      isDemo = true;
      }
    private void stopDemo()
      {
      // Send 'DEMO' intent to data service: 
      Intent intent = new Intent(DataService.DATA_SERVICE_DEMO);
      intent.putExtra(DataService.SERVICE_DEMO_SETTO,false);
      messageBroadcaster.sendBroadcast(intent);
      isDemo = false;
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
          if (isDemo)
            {
            ShowMessage("Stop Demo.");
            stopDemo();
            }
          else
            {
            ShowMessage("Start Demo!");
            startDemo();
            }
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
    
    
    
    

    
    /*******************************************************************************
     * Declare a Broadcast Receiver to catch intents sent from the data service:
     * Calle whenever an intent is sent with action IDroidSensor.SENSOR_INTENT_ACTION
     * (actual constant value defined in IDroidSensor.java)
     *******************************************************************************/
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() 
      {
      @Override
      public void onReceive(Context context, Intent intent) 
        {
        
        int whatSensor = intent.getIntExtra( IDroidSensor.SENSOR_INTENT_FROMID, IDroidSensor.IDROIDSENSOR_UNKNOWN_ID);  
           // Get the 'From ID' out of the intent.
           // If the intent was generated by a sensor or data source implimenting 'IDroidSensor', this should
           // be one of the integers representing a sensor data element, using a constant from the particular 
           // sensor class or from IDroidSensor. A value of IDroidSensor.IDROIDSENSOR_UNKNOWN_ID indicates that 
           // no IDroidSensor.SENSOR_INTENT_FROMID was specified in the Intent extra data. That shouldn't happen. 

        int dataType = intent.getIntExtra( IDroidSensor.SENSOR_INTENT_DATATYPE, IDroidSensor.SENSOR_UNKNOWN_DATATYPE);
           // Get the data trype for this sensor. See constants in IDroidSensor.

        // If this is a float data value, get the data. If no data element was set as
        // an extra field named IDroidSensor.SENSOR_INTENT_VALUE, this just gets a value of 0.
        float valueFloat = 0f;
        if (dataType == IDroidSensor.SENSOR_FLOAT_DATA) valueFloat = intent.getFloatExtra( IDroidSensor.SENSOR_INTENT_VALUE, 0f );
        
        //Log.i(APP_TAG, String.format( "UIActivity -> Intent Rec... What = %d; Value = %.1f", whatIntent, valueFloat) );
        
        switch (whatSensor)
          {
          
          //****** Data Messages from vehicle data input: **********************************************************            
          case VehicleData.DATA_MOTOR_RPM:         ((Dial)uiWidgets.get("dialMotorRPM"))                .setNeedle ( valueFloat / 1000                  );   break;
          case VehicleData.DATA_MAIN_BATTERY_KWH:  ((Dial)uiWidgets.get("dialMainBatteryKWh"))          .setNeedle ( valueFloat                         );   break;
          case VehicleData.DATA_ACC_BATTERY_VLT:   ((TextWithLabel)uiWidgets.get("textAccBatteryVlts")) .setText   (  String.format("%.1f", valueFloat) );   break;
          case VehicleData.DATA_MOTOR_TEMP:        ((TextWithLabel)uiWidgets.get("textTMotor"))         .setText   (  String.format("%.1f", valueFloat) );   break;
          case VehicleData.DATA_CONTROLLER_TEMP:   ((TextWithLabel)uiWidgets.get("textTController"))    .setText   (  String.format("%.1f", valueFloat) );   break;
          case VehicleData.DATA_MAIN_BATTERY_TEMP: ((TextWithLabel)uiWidgets.get("textTBattery"))       .setText   (  String.format("%.1f", valueFloat) );   break;

          case VehicleData.DATA_DATA_OK:
            if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampData")).turnOn();
            else                  ((StatusLamp)uiWidgets.get("lampData")).turnOff();
            break;
          case NmeaProcessor.DATA_GPS_HAS_LOCK:
            if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampGPS")).turnOn();
            else                  ((StatusLamp)uiWidgets.get("lampGPS")).turnOff();
            break;  
          case VehicleData.DATA_CONTACTOR_ON:
            if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampContactor")).turnOn();
            else                  ((StatusLamp)uiWidgets.get("lampContactor")).turnOff();
            break;  
          case VehicleData.DATA_FAULT:
            if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampFault")).turnOn();
            else                  ((StatusLamp)uiWidgets.get("lampFault")).turnOff();
            break;            
          case VehicleData.DATA_PRECHARGE:
            if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampGreenGlobe")).turnOn();
            else                  ((StatusLamp)uiWidgets.get("lampGreenGlobe")).turnOff();
            break;  
          //********************************************************************************************************
                  
          case VehicleData.VEHICLE_DATA_ERROR:
            // Vehicle Data Connection Error. For now, just ignore...
            //ShowMessage( msg.obj.toString() );
            break;
            
        }  // [switch]
      }  // [onReceive()...]
    };  // [messageReceiver Inner Class]
    
    
       

    /**** UI Timer Handling Runnable: *******************
     * This runnable creates a timer to update the UI.
     * Note that this is a low priority UI update for 
     * triggering a keep-alive signal to data service. 
     ****************************************************/ 
    private Runnable uiTimerTask = new Runnable() 
     {
     // Creates a Runnable which will be called after a delay, to 
     // update the UI:
     public void run()  
       {
       
       // Send Keep Alive to data service: 
       Intent intent = new Intent(DataService.DATA_SERVICE_KEEPALIVE);
       messageBroadcaster.sendBroadcast(intent);
       // Start the timer for next UI Uodate:     
       uiTimer.removeCallbacks(uiTimerTask);               // ...Make sure there is no active callback already....
       uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);  // ...Callback later!
       } 
     };
   
     
     
     
    // ********** UI Pause / Resume Events: ****************************************************
    @Override
    protected void onResume() 
      {
      super.onResume();
      // Register to receive messages via Intents:
      LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,  new IntentFilter(IDroidSensor.SENSOR_INTENT_ACTION));
        // We are registering an observer (messageReceiver) to receive Intents
        // with actions named IDroidSensor.SENSOR_INTENT_ACTION (see IDroidSensor.java for constant defn.).
      // Start the data server (in case it's not already going; doesn't matter if it is). 
      startService(dataIntent);
      uiReset();
      uiTimer.removeCallbacks(uiTimerTask);                // ...Make sure there is no active callback already....
      uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);   // ...Callback in n seconds!
      }

    
    
    
    @Override
    protected void onPause() 
      {
      super.onPause();
      // Unregister listener since the activity is about to be closed.
      LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
      uiTimer.removeCallbacks(uiTimerTask);      // ...Make sure there is no active callback already....
      }

    
    
    @Override
    protected void onStop()
      {
      super.onStop();
      SaveSettings();
      }

    


    // ******** Toast Message Method: ********************
    private void ShowMessage(String ThisMessage)
      {
      // *** Displays pop-up TOAST message: **
      Toast.makeText(getApplicationContext(), ThisMessage, Toast.LENGTH_SHORT).show();
      }
    
    
    
    
    
    
    // ************ Reset UI to default: ************************************
    private void uiReset()
      {
      //****** Data Messages from vehicle data input: **********************************************************            
      ((Dial)uiWidgets.get("dialMotorRPM"))                .setNeedle ( 0f );
      ((Dial)uiWidgets.get("dialMainBatteryKWh"))          .setNeedle ( 0f );

      ((TextWithLabel)uiWidgets.get("textAccBatteryVlts")) .setText   (  String.format("%.1f", 0f ) );
      ((TextWithLabel)uiWidgets.get("textTMotor"))         .setText   (  String.format("%.1f", 0f ) );
      ((TextWithLabel)uiWidgets.get("textTController"))    .setText   (  String.format("%.1f", 0f ) );
      ((TextWithLabel)uiWidgets.get("textTBattery"))       .setText   (  String.format("%.1f", 0f ) );
 
      ((StatusLamp)uiWidgets.get("lampData")).turnOff();
      ((StatusLamp)uiWidgets.get("lampGPS")).turnOff();
      ((StatusLamp)uiWidgets.get("lampContactor")).turnOff();
      ((StatusLamp)uiWidgets.get("lampFault")).turnOff();
      ((StatusLamp)uiWidgets.get("lampGreenGlobe")).turnOff();
      }
    
    
    
    

}  // [class]

