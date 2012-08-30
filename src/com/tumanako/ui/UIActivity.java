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
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

/*********************************************************
 * Main UI Activity: 
 *  -Displays the UI! 
 *   
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ********************************************************/

public class UIActivity extends Activity implements OnClickListener, OnLongClickListener, OnTouchListener   //, ServiceConnection
    {
    
    public static final String APP_TAG = "TumanakoDash";

    // **** Tabs and Swipes: ***************************
    private TabHost tabHost;
    private int currentTab = 0;
    private static final int MIN_TAB = 0;
    private static final int MAX_TAB = 2;
    private GestureDetector gestureDetector;
    
    
    // HashMap for list of UI Widgets: 
    private HashMap<String,View> uiWidgets = new HashMap<String,View>();
    
    // Intent - used to start Data Service:
    private Intent  dataIntent;

    // Message Broadcaster to send Tntents to data service: 
    private LocalBroadcastManager messageBroadcaster;    

    
    // UI Timer Handler: 
    // We'll create a timer to update the UI occasionally:
    private Handler uiTimer = new Handler(); 
    
    private static final int UI_UPDATE_EVERY = 500;   // Update the UI every n mSeconds.
      
    public static final int UI_TOAST_MESSAGE = 1;     // Sent by another class when they have a brief message they would like displayed.  

    private static final String PREFS_NAME = "TumanakoDashPrefs";
    
    
    // ---------------DEMO MODE CODE -------------------------------
    private boolean isDemo = false;  // Demo mode flag!
    // ---------------DEMO MODE CODE -------------------------------  

    
    
    
    
    
    
    /********* Create UI: Called when the activity is first created: ***************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) 
      {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      // ***** Set up tabs: ****
      tabHost = (TabHost)findViewById(R.id.tabhost);
      tabHost.setup();

      TabSpec spec1=tabHost.newTabSpec("Main Data");
      spec1.setContent(R.id.layoutPrimaryData);
      spec1.setIndicator("Main");

      TabSpec spec2=tabHost.newTabSpec("Secondary Data");
      spec2.setIndicator("Secondary");
      spec2.setContent(R.id.layoutSecondaryData);

      TabSpec spec3=tabHost.newTabSpec("System Data");
      spec3.setIndicator("System");
      spec3.setContent(R.id.layoutSystemData);
      
      tabHost.addTab(spec1);
      tabHost.addTab(spec2);
      tabHost.addTab(spec3);

      
      // --DEBUG!!--
      Log.i(APP_TAG,"UIActivity -> onCreate()");
           
      // ********** Make a list of available UI widgets: *****************************
      // Primary Data:
      uiWidgets.put( "lampData",           findViewById(R.id.lampData)           );
      uiWidgets.put( "lampGPS",            findViewById(R.id.lampGPS)            );
      uiWidgets.put( "lampContactor",      findViewById(R.id.lampContactor)      );
      uiWidgets.put( "lampFault",          findViewById(R.id.lampFault)          );
      uiWidgets.put( "dialMotorRPM",       findViewById(R.id.dialMotorRPM)       );
      uiWidgets.put( "dialMainBatteryKWh", findViewById(R.id.dialMainBatteryKWh) );
      uiWidgets.put( "barTMotor",          findViewById(R.id.barTMotor)          );
      uiWidgets.put( "barTController",     findViewById(R.id.barTController)     );
      uiWidgets.put( "barTBattery",        findViewById(R.id.barTBattery)        );
      //uiWidgets.put( "textTController",    findViewById(R.id.textTController)    );
      //uiWidgets.put( "textTBattery",       findViewById(R.id.textTBattery)       );
      // Secondary Data: 
      uiWidgets.put( "textDriveTime",      findViewById(R.id.textDriveTime)      );
      uiWidgets.put( "textDriveRange",     findViewById(R.id.textDriveRange)     );
      uiWidgets.put( "textAccBatteryVlts", findViewById(R.id.textAccBatteryVlts) );      
      // System Data:
      uiWidgets.put( "lampPreCharge",      findViewById(R.id.lampPreCharge)      );      
      uiWidgets.put( "textMainBattVlts",   findViewById(R.id.textMainBattVlts)   );
      uiWidgets.put( "textMainBattAH",     findViewById(R.id.textMainBattAH)     );
      
      
      // ---- Connect click and gesture listeners: -------
      gestureDetector = new GestureDetector(new SimpleSwiper(this));
      tabHost.setOnTouchListener(this);

      // ---- Create a Data Service intent: ------
      dataIntent = new Intent(this, com.tumanako.sensors.DataService.class);
      
      messageBroadcaster = LocalBroadcastManager.getInstance(this);  
      // Get a Broadcast Manager so we can send out messages to other parts of the app.

      
      // -------- Restore Saved Preferences (if any): -------------------
      /***
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      totalEnergy    = 25.0;  //settings.getFloat( "totalEnergy", 0 );
      totalDistance  = settings.getFloat( "totalDistance", 0 );
      ***/
      
      
      // -------- Restore saved instence state if one exists: ---------------------
      if (savedInstanceState != null)
        {
        currentTab = savedInstanceState.getInt( "currentTab" );
        // ---------------DEMO MODE CODE -------------------------------
        isDemo     = savedInstanceState.getBoolean( "isDemo" );
        // ---------------DEMO MODE CODE -------------------------------         
        }
      // --------------------------------------------------------------------------

      
      
      }


    
    
    
    

    
    
    
    /********** Other useful Private and Public methods: ***********************************************/
    

    /*********************************
     * Next Screen and Prevous Screen: 
     * 
     * These methods switch between the tabs for the various 
     * UI screens (Primary Data, Secondary data, System Data). 
     * 
     * currentTab is the index of the currently visible tab. 
     * 
     ********************************/
    public void nextScreen()
      {
      currentTab++;
      if (currentTab > MAX_TAB) currentTab = MIN_TAB;
      tabHost.setCurrentTab(currentTab);
      }
    
    public void prevScreen()
      {
      currentTab--;
      if (currentTab < MIN_TAB) currentTab = MAX_TAB;
      tabHost.setCurrentTab(currentTab);
      }

    
    // **** Save a copy of some data to the Preferences ********
    private void SaveSettings()
      {
      /***
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putInteger("isDemo", false);
      editor.commit();        // Commit the edits!
      ***/
      }
    
    
    // ---------------DEMO MODE CODE -------------------------------
    private void startDemo()
      {
      // Send 'DEMO' intent to data service:
      Log.i(APP_TAG,"UIActivity -> startDemo()");
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
    


    /*************************
     * Get Hours: 
     * Given a decimal number of hours (e.g. 1.5), return the
     * number of hours as an integer (i.e. truncate the fractional part). 
     * @param decimalHours - Decimal Hours (i.e. 1.5 = 1 Hr 30 Mins).
     * @return
     ************************/
    private int getHours(float decimalHours)
      {
      return (int)(decimalHours);
      }
    /*************************
     * Get Minutes: 
     * Given a decimal number of hours (e.g. 1.5), return the
     * number of minutes as an integer (i.e. fractional part multiplied by 60) 
     * @param decimalHours - Decimal Hours (i.e. 1.5 = 1 Hr 30 Mins).
     * @return
     ************************/
    private int getMinutes(float decimalHours)
      {
      return (int)((decimalHours - (float)((int)(decimalHours))) * 60);
      }
    

    
    // ***** Toast Message Method: ************
    private void ShowMessage(String ThisMessage)
      {
      // *** Displays pop-up TOAST message: **
      Toast.makeText(getApplicationContext(), ThisMessage, Toast.LENGTH_SHORT).show();
      }
    
    
    // ********** Reset UI to default: *******************************
    private void uiReset()
      {
Log.i(APP_TAG,"UIActivity -> uiReset()");      
      // Primary Data: 
      ((Dial)uiWidgets.get("dialMotorRPM"))                .setValue (  0f );
      ((Dial)uiWidgets.get("dialMainBatteryKWh"))          .setValue (  0f );
      ((BarGauge)uiWidgets.get("barTMotor"))               .setValue (  0f );
      ((BarGauge)uiWidgets.get("barTController"))          .setValue (  0f );
      ((BarGauge)uiWidgets.get("barTBattery"))             .setValue (  0f );
      //((TextWithLabel)uiWidgets.get("textTController"))    .setText   (  "0.0"  );
      //((TextWithLabel)uiWidgets.get("textTBattery"))       .setText   (  "0.0"  );
      ((StatusLamp)uiWidgets.get("lampData")).turnOff();
      ((StatusLamp)uiWidgets.get("lampGPS")).turnOff();
      ((StatusLamp)uiWidgets.get("lampContactor")).turnOff();
      ((StatusLamp)uiWidgets.get("lampFault")).turnOff();
      // Secondary Data: 
      ((TextWithLabel)uiWidgets.get("textDriveTime"))     .setText   (  "00:00" );
      ((TextWithLabel)uiWidgets.get("textDriveRange"))    .setText   (  "0"     );
      ((TextWithLabel)uiWidgets.get("textAccBatteryVlts")) .setText  (  "0.0"   );
      // System Data:      
      ((TextWithLabel)uiWidgets.get("textMainBattVlts"))  .setText   (  "0.0"   );
      ((TextWithLabel)uiWidgets.get("textMainBattAH"))    .setText   (  "0.0"   );
      ((StatusLamp)uiWidgets.get("lampPreCharge")).turnOff();
      }

    
    
    
    
    
    
    
    
    
    
    
    /***************** UI Event Handlers: ****************************************************************/

    // ***** Touch Event: Used to detect 'swipes' ********* 
    public boolean onTouch(View v, MotionEvent event)
      {
      // When a Touch event is detected, we pass the event to the
      // gestureDetector (created earlier). We've previously told
      // the gestureDetector to use our custom gesture detector
      // defined in SimpleSwiper.java (see  constructor). 
      return gestureDetector.onTouchEvent(event);  
      }

    // ***** Click Event: (Can be used with buttons and other controls) ************   
    public void onClick(View MyView) 
      { 
      // Process button click events for this activity: 
      //  This general OnClick handler is called for all the buttons. 
      //  The code checks the ID of the view which generated the event
      //  (i.e. the button) and takes the appropriate action.  
      switch (MyView.getId())
        {
        // Now done with the menu, but we might want to use buttons later...
        //case R.id.buttonClose: 
        //  finish();
        //  break;
        }
      } 
    
    // **** LOOOONG Click Action Handler: ************** 
    public boolean onLongClick(View MyView)
      {
      // Process long press events for this activity:
      // (Could use to reset trip data) 
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
    
    // ****** Menu Click Action Handler: *************************** 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
      {
      // Check to see which menu item was selected: 
      switch (item.getItemId()) 
        {
        
        case R.id.menuitemShowPrimary:
          tabHost.setCurrentTab(0);
          currentTab = 0;
          return true;
        case R.id.menuitemShowSecondary:
          tabHost.setCurrentTab(1);
          currentTab = 1;
          return true;
        case R.id.menuitemShowSystem:
          tabHost.setCurrentTab(2);
          currentTab = 2;
          return true;
          
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
    
    /*****************************************************************************************************/
    
    
    
    
    
    
    
    
    
    
    /***************** Activity-Level Event Handlers: ***********************************/

    
    // ****** Menu Create Event ***********************
    // We've been told that this is a good time to draw the menu. 
    // Create it from the XML file: 
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
      {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.options_menu, menu);
      return true;
      }

    
    
    // ********** UI Resume Event *************************
    // UI has restarted after being in background 
    // (also called when UI started for the first time). 
    @Override
    protected void onResume() 
      {
Log.i(APP_TAG,"UIActivity -> onResume()");
Log.i(APP_TAG,"     State: currentTab = " + currentTab + "; isDemo = " + isDemo );
      super.onResume();
      // Register to receive messages via Intents:
      LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,  new IntentFilter(IDroidSensor.SENSOR_INTENT_ACTION));
        // We are registering an observer (messageReceiver) to receive Intents
        // with actions named IDroidSensor.SENSOR_INTENT_ACTION (see IDroidSensor.java for constant defn.).
      // Start the data server (in case it's not already going; doesn't matter if it is). 
      startService(dataIntent);
      uiReset();
      tabHost.setCurrentTab(currentTab);
      // ---------------DEMO MODE CODE -------------------------------
      if (isDemo) startDemo();
      else        stopDemo();
      // ---------------DEMO MODE CODE -------------------------------
      uiTimer.removeCallbacks(uiTimerTask);                // ...Make sure there is no active callback already....
      uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);   // ...Callback in n seconds!
      }

    
    
    // ********** Restore Instance State: ************************
    // Called when UI is recovered after being in background, but
    // NOT recreated from scratch. 
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
      {
Log.i(APP_TAG,"UIActivity -> onRestoreInstanceState()");
      super.onRestoreInstanceState(savedInstanceState);
      // Restore UI state from the savedInstanceState.
      currentTab = savedInstanceState.getInt( "currentTab" );
      // ---------------DEMO MODE CODE -------------------------------
      isDemo     = savedInstanceState.getBoolean( "isDemo" );
      // ---------------DEMO MODE CODE -------------------------------         
Log.i(APP_TAG,"UIActivity -> Restore State: currentTab = " + currentTab + "; isDemo = " + isDemo );
      
      }
    
    
    
    // ********** UI Pause Event *************************
    // Activity has gone into the background. 
    @Override
    protected void onPause() 
      {
Log.i(APP_TAG,"UIActivity -> onPause()");      
      super.onPause();
      // Unregister listener since the activity is about to be closed or stopped.
      LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
      uiTimer.removeCallbacks(uiTimerTask);      // ...Make sure there is no active callback already....
      }

    // ******** Save State Event: **********************************
    // Save state info before the application is hidden / rotated / etc (or otherwise trashed by the OS):    
    public void onSaveInstanceState(Bundle outState)
      {
Log.i(APP_TAG,"UIActivity -> onSaveInstanceState()");
      super.onSaveInstanceState(outState);
      outState.putInt( "currentTab",  currentTab );
      // ---------------DEMO MODE CODE -------------------------------
      outState.putBoolean( "isDemo", isDemo );
      // ---------------DEMO MODE CODE -------------------------------
      }
    

    // ****** UI Stop Event *********************
    // Activity is about to be destroyed. Save Prefs:  
    @Override
    protected void onStop()
      {
Log.i(APP_TAG,"UIActivity -> onStop()");
      // ---------------DEMO MODE CODE -------------------------------
      stopDemo();
      // ---------------DEMO MODE CODE -------------------------------      
      super.onStop();
      SaveSettings();
      }

    /************************************************************************************/
    
    
    
    
    
    
    
    
    
    
    
    

    
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

          // case VehicleData.DATA_MOTOR_TEMP:        ((TextWithLabel)uiWidgets.get("textTMotor"))         .setText   (  String.format("%.1f", valueFloat) );   break;
          // case VehicleData.DATA_CONTROLLER_TEMP:   ((TextWithLabel)uiWidgets.get("textTController"))    .setText   (  String.format("%.1f", valueFloat) );   break;
          // case VehicleData.DATA_MAIN_BATTERY_TEMP: ((TextWithLabel)uiWidgets.get("textTBattery"))       .setText   (  String.format("%.1f", valueFloat) );   break;
          
          //****** Data Messages from vehicle data input: **********************************************************            
          case VehicleData.DATA_MOTOR_RPM:         ((Dial)uiWidgets.get("dialMotorRPM"))                .setValue  ( valueFloat / 1000                  );   break;
          case VehicleData.DATA_MAIN_BATTERY_KWH:  ((Dial)uiWidgets.get("dialMainBatteryKWh"))          .setValue  ( valueFloat                         );   break;

          case VehicleData.DATA_MOTOR_TEMP:        ((BarGauge)uiWidgets.get("barTMotor"))               .setValue  ( valueFloat );                           break;
          case VehicleData.DATA_CONTROLLER_TEMP:   ((BarGauge)uiWidgets.get("barTController"))          .setValue  ( valueFloat );                           break;
          case VehicleData.DATA_MAIN_BATTERY_TEMP: ((BarGauge)uiWidgets.get("barTBattery"))             .setValue  ( valueFloat );                           break;
          
          case VehicleData.DATA_ACC_BATTERY_VLT:   ((TextWithLabel)uiWidgets.get("textAccBatteryVlts")) .setText   (  String.format("%.1f", valueFloat) );   break;

          case VehicleData.DATA_DRIVE_TIME:        ((TextWithLabel)uiWidgets.get("textDriveTime"))      .setText   (  String.format("%1d:%02d", getHours(valueFloat), getMinutes(valueFloat) ) ); break;
          case VehicleData.DATA_DRIVE_RANGE:       ((TextWithLabel)uiWidgets.get("textDriveRange"))     .setText   (  String.format("%.0f", valueFloat) );   break;
          
          case VehicleData.DATA_MAIN_BATTERY_VLT:  ((TextWithLabel)uiWidgets.get("textMainBattVlts"))   .setText   (  String.format("%.1f", valueFloat) );   break;
          case VehicleData.DATA_MAIN_BATTERY_AH:   ((TextWithLabel)uiWidgets.get("textMainBattAH"))     .setText   (  String.format("%.1f", valueFloat) );   break;
          
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
            if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampPreCharge")).turnOn();
            else                  ((StatusLamp)uiWidgets.get("lampPreCharge")).turnOff();
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
   
     
     
     
 
    






    
    
    
}  // [class]

