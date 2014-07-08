package com.tumanako.ui;


/************************************************************************************

Tumanako - Electric Vehicle and Motor control software <p>

Copyright (C) 2012 Jeremy Cole-Baker <jeremy@rhtech.co.nz> <p>

This file is part of Tumanako Dashboard. <p>

Tumanako is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. <p>

Tumanako is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details. <p>

You should have received a copy of the GNU Lesser General Public License
along with Tumanako.  If not, see <http://www.gnu.org/licenses/>. <p>

@author Jeremy Cole-Baker / Riverhead Technology <jeremy@rhtech.co.nz> <p>

*************************************************************************************/


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.tumanako.dash.ChargeNode;
import com.tumanako.sensors.DataService;
import com.tumanako.sensors.NmeaProcessor;



/*********************************************************
 * Main UI Activity: 
 *  -Displays the UI! 
 *   
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ********************************************************/

public class DashActivity extends UIActivity implements OnLongClickListener, OnTouchListener
    {
    
    
    // **** GPS Time / Speed / Position Data: **********
    float   gpsTime   = 0f;
    float   gpsSpeed  = 0f;
    double  gpsLat    = 0.0;
    double  gpsLon    = 0.0;
    int     gpsSats   = 0;
    boolean gpsHasFix = false;

    
    // **** Tabs and Swipes: ***************************
    private TabHost tabHost;
    private int currentTab = 0;
    private static final int MIN_TAB = 0;
    private static final int MAX_TAB = 4;
    private GestureDetector gestureDetector;
    
        
    // Intent - used to start Data Service:
    private Intent  dataIntent;     
    
    // Extra intent filters for main dashboard activity:
    private static final String intentFilters[] = 
      {
      NmeaProcessor.GPS_POSITION,
      ChargeNode.CHARGE_NODE,
      "CLICK_CHARGECONNECT",
      "CLICK_CHARGESTART",
      "CLICK_CHARGESTOP"
      };
        
    // ---------------DEMO MODE CODE -------------------------------
    private boolean isDemo = false;  // Demo mode flag!
    // ---------------DEMO MODE CODE -------------------------------  

    
    
    
    
    
    
    
    /********* Create UI: Called when the activity is first created: ***************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) 
      {
      
      // Add our extra intent filters... (must be called before super.onCreate)
      setExtraIntentFilters(intentFilters);
      
      super.onCreate(savedInstanceState);
      setContentView(com.tumanako.ui.R.layout.main);

      // ***** Set up tabs: ****
      tabHost = (TabHost)findViewById(R.id.tabhost);
      tabHost.setup();

      TabSpec tabMainData = tabHost.newTabSpec("Main Data");
      tabMainData.setContent(R.id.layoutPrimaryData);
      tabMainData.setIndicator("Main");

      TabSpec tabSecondaryData = tabHost.newTabSpec("Road Data");
      tabSecondaryData.setIndicator("Secondary");
      tabSecondaryData.setContent(R.id.layoutSecondaryData);

      TabSpec tabLapData = tabHost.newTabSpec("Track Data");
      tabLapData.setIndicator("Lap Data");
      tabLapData.setContent(R.id.layoutLapData);

      TabSpec tabSystemData = tabHost.newTabSpec("System Data");
      tabSystemData.setIndicator("System");
      tabSystemData.setContent(R.id.layoutSystemData);
      
      TabSpec tabChargeNode = tabHost.newTabSpec("Charge Node");
      tabChargeNode.setIndicator("Charging");
      tabChargeNode.setContent(R.id.layoutChaqrgeNode);

      tabHost.addTab(tabMainData);
      tabHost.addTab(tabLapData);
      tabHost.addTab(tabSecondaryData);
      tabHost.addTab(tabSystemData);
      tabHost.addTab(tabChargeNode);

      
      // --DEBUG!!--
      Log.i(APP_TAG,"DashActivity -> onCreate()");
           
      // **** Calc height of dials based on width: ****************
      //int thisWidth =   ((Dial) findViewById(R.id.dialMotorRPM)).getWidth();
      //((Dial) findViewById(R.id.dialMotorRPM)).setLayoutParams(new LayoutParams(thisWidth, (int)((double)thisWidth * 0.58)));
      

      // ---- Connect touch and gesture listeners for app: -------
      gestureDetector = new GestureDetector(new SimpleSwiper(this));
      tabHost.setOnTouchListener(this);

      
      // ---- Create a Data Service intent: ------
      dataIntent = new Intent(this, com.tumanako.sensors.DataService.class);
      

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
      Log.i(APP_TAG,"DashActivity -> startDemo()");
      dashMessages.sendData( DataService.DATA_SERVICE_DEMO, 1, null, null, null );
      isDemo = true;
      }
    private void stopDemo()
      {
      // Send 'DEMO' intent to data service:
      dashMessages.sendData( DataService.DATA_SERVICE_DEMO, 0, null, null, null );
      isDemo = false;
      }  
    // ---------------DEMO MODE CODE -------------------------------
    

    
    
    // ********** Reset UI to default: *******************************
    private void uiReset()
      {  dashMessages.sendData( UI_RESET, null, null, null, null );  }
    
    
    private void chargeNodeUIReset()
      {
      // Charge Node UI Reset:
      //--DEBUG!--
Log.i(APP_TAG,"DashActivity -> chargeNodeUIReset()");
//      ((StatusLamp)uiWidgets.get("lampChargeNodeOnline")).turnOff();
//      ((StatusLamp)uiWidgets.get("lampCharging")).turnOff();
//      ((WebView)uiWidgets.get("webChargeNodeContent")).loadData(ChargeNode.CHARGE_NODE_DEFAULT_HTML, "text/html", null);
//      ((Button)uiWidgets.get("buttonConnectToNode")).setText("Connect");
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
        case R.id.menuitemShowLapData:
          tabHost.setCurrentTab(1);
          currentTab = 1;
          return true;
        case R.id.menuitemShowSecondary:
        tabHost.setCurrentTab(2);
        currentTab = 1;
        return true;
        case R.id.menuitemShowSystem:
          tabHost.setCurrentTab(3);
          currentTab = 2;
          return true;
        case R.id.menuitemShowCharge:
          tabHost.setCurrentTab(4);
          currentTab = 3;
          return true;          
          
        // ---------------DEMO MODE CODE -------------------------------        
        case R.id.menuitemDemoMode:
          if (isDemo)
            {
            dashMessages.sendData(UIActivity.UI_TOAST_MESSAGE, null, null, "Stop Demo.", null);
            stopDemo();
            }
          else
            {
            dashMessages.sendData(UIActivity.UI_TOAST_MESSAGE, null, null, "Start Demo.", null);
            startDemo();
            }
          return true;
        // ---------------DEMO MODE CODE -------------------------------          
        case R.id.menuitemSettings:
          // Show Settings screen:  
          Intent settingsIntent = new Intent(this, SettingsActivity.class);
          startActivityForResult(settingsIntent, 0);
          return true;
        case R.id.menuitemTrackSettings:
          // Show Track Day Settings screen:  
          Intent trackSettingsIntent = new Intent(this, TrackSettingsActivity.class);
          startActivityForResult(trackSettingsIntent, 0);
          return true;
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
Log.i(APP_TAG,"DashActivity -> onResume()");
Log.i(APP_TAG,"     State: currentTab = " + currentTab + "; isDemo = " + isDemo );
      super.onResume();
      
      // Start the data server (in case it's not already going; doesn't matter if it is). 
      startService(dataIntent);
      uiReset();
      tabHost.setCurrentTab(currentTab);
      // ---------------DEMO MODE CODE -------------------------------
      if (isDemo) startDemo();
      else        stopDemo();
      // ---------------DEMO MODE CODE -------------------------------
      }

    
    
    // ********** Restore Instance State: ************************
    // Called when UI is recovered after being in background, but
    // NOT recreated from scratch. 
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
      {
Log.i(APP_TAG,"DashActivity -> onRestoreInstanceState()");
      super.onRestoreInstanceState(savedInstanceState);
      // Restore UI state from the savedInstanceState.
      currentTab = savedInstanceState.getInt( "currentTab" );
      // ---------------DEMO MODE CODE -------------------------------
      isDemo     = savedInstanceState.getBoolean( "isDemo" );
      // ---------------DEMO MODE CODE -------------------------------         
Log.i(APP_TAG,"DashActivity -> Restore State: currentTab = " + currentTab + "; isDemo = " + isDemo );
      
      }
    
    
    
    // ********** UI Pause Event *************************
    // Activity has gone into the background. 
    @Override
    protected void onPause() 
      {
Log.i(APP_TAG,"DashActivity -> onPause()");      
      super.onPause();
      }

    // ******** Save State Event: **********************************
    // Save state info before the application is hidden / rotated / etc (or otherwise trashed by the OS):    
    public void onSaveInstanceState(Bundle outState)
      {
Log.i(APP_TAG,"DashActivity -> onSaveInstanceState()");
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
Log.i(APP_TAG,"DashActivity -> onStop()");
      // ---------------DEMO MODE CODE -------------------------------
      stopDemo();
      // ---------------DEMO MODE CODE -------------------------------      
      super.onStop();
      SaveSettings();
      }

    /************************************************************************************/
    
    
    
    
    
    
       
     
 


    public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
      {
      // --DEBUG!-- Log.i(APP_TAG, String.format( "DashActivity -> Intent: %s; ", action) );
      
      super.messageReceived(action, intData, floatData, stringData, bundleData);

      if ( (action.equals(NmeaProcessor.GPS_POSITION)) && (null != bundleData) )
        {
        // GPS position received:
        gpsLat    = bundleData.getDouble  ( "LAT",    0.0   );
        gpsLon    = bundleData.getDouble  ( "LON",    0.0   );
        gpsTime   = bundleData.getFloat   ( "TIME",   0.0f  );
        gpsSpeed  = bundleData.getFloat   ( "SPEED",  0.0f  );
        gpsSats   = bundleData.getInt     ( "NSATS",  0     );
        gpsHasFix = bundleData.getBoolean ( "FIX",    false );
        
        // Re-send GPS data as individual intents to update the UI:
        dashMessages.sendData( "DATA_GPS_HAS_LOCK", null,    gpsHasFix ? 1f : 0f,  null,     null );
        dashMessages.sendData( "DATA_GPS_TIME",     null,    gpsTime,              "%06.0f", null );
        dashMessages.sendData( "DATA_GPS_SPEED",    null,    gpsSpeed,             null,     null );
        dashMessages.sendData( "DATA_GPS_NSATS",    gpsSats, null,                 null,     null );
        dashMessages.sendData( "DATA_GPS_LAT",      null,    (float)gpsLat,        "%.7f",   null );
        dashMessages.sendData( "DATA_GPS_LON",      null,    (float)gpsLon,        "%.7f",   null );

        }
        

      // *** Click messages from buttons on the "Charge Node" page: ***
      // Send intent to ChargeNode class, including the login details.
      if (action.equals("CLICK_CHARGECONNECT"))  
        {  dashMessages.sendData( ChargeNode.CHARGE_NODE_CONNECT,     null, null, null, getChargeNodeData() );  }
      
      if (action.equals("CLICK_CHARGESTART")) 
        {  dashMessages.sendData( ChargeNode.CHARGE_NODE_CHARGESTART, null, null, null, getChargeNodeData() );  }

      if (action.equals("CLICK_CHARGESTOP")) 
        {  dashMessages.sendData( ChargeNode.CHARGE_NODE_CHARGESTOP,  null, null, null, getChargeNodeData() );  }
      
      
      // *** Data recevied from charge node class: ***     
      if (action.equals(ChargeNode.CHARGE_NODE))
        {
        // Data Message from Charge Node... 
        // If string data is included in the Intent, assume it's some HTML for the webview control to display: 
        //if (stringData != null) ((WebView)uiWidgets.get("webChargeNodeContent")).loadUrl(stringData);
       if (stringData != null) ((WebView)findViewById(R.id.webChargeNodeContent)).loadData(stringData, "text/html", null);        
        // Get the Current and AH values from the data: 
        //Float chargeCurrent = data.getFloat(ChargeNode.CHARGE_CURRENT, 0.0f);
        //Float chargeAH      = data.getFloat(ChargeNode.CHARGE_AH, 0.0f);
        // Get the status lamp values: 
        if (bundleData.getFloat(ChargeNode.CONNECTION_STATUS, 0.0f) == 0.0f ) 
          {
          ((StatusLamp)findViewById(R.id.lampChargeNodeOnline)).turnOff();
          ((ExtButton)findViewById(R.id.buttonConnectToNode)).setText("Connect");
          }
        else                                                            
          {
          ((StatusLamp)findViewById(R.id.lampChargeNodeOnline)).turnOn();
          ((ExtButton)findViewById(R.id.buttonConnectToNode)).setText("Disconnect");
          }
       if (bundleData.getFloat(ChargeNode.CHARGE_STATUS, 0.0f) == 0.0f ) ((StatusLamp)findViewById(R.id.lampCharging)).turnOff();
       else                                                              ((StatusLamp)findViewById(R.id.lampCharging)).turnOn();
        //((TextWithLabel)uiWidgets.get("textChargeCurrent"))      .setText   ( String.format("%.0f",chargeCurrent) );
        //((TextWithLabel)uiWidgets.get("textChargeAH"))           .setText   ( String.format("%.1f",chargeAH)      );

        }  // [if (message == VehicleData.VEHICLE_DATA_ID)]
      }  // [function]
   
     
     
    
    
     
 
    
  /**
   Read the text from the "Token" and "Password" boxes on the Chagre Node screen, 
   and build a bundle containing the values. 
      
   @return Bundle containing the token and password values as strings (tags "j_token" and "j_password").
   */
  private Bundle getChargeNodeData()
    {
    Bundle chargeUIData = new Bundle();
    chargeUIData.putString( "j_token",    ((EditText)findViewById(R.id.editTextToken)).getText().toString()    );
    chargeUIData.putString( "j_password", ((EditText)findViewById(R.id.editTextPassword)).getText().toString() );   
    return chargeUIData;
    }






    
    
    
}  // [class]

