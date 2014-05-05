/*
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
 */

package com.tumanako.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;
import com.tumanako.dash.ChargeNode;
import com.tumanako.dash.DashMessages;
import com.tumanako.dash.DashMessageListener;
import com.tumanako.sensors.DataService;
import com.tumanako.sensors.NmeaProcessor;
import com.tumanako.sensors.VehicleData;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Main UI Activity.
 * - Displays the UI!
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class UIActivity extends Activity implements OnClickListener, OnLongClickListener, OnTouchListener, DashMessageListener //, ServiceConnection
{

  public static final String APP_TAG = "TumanakoDash";

  // Tabs and Swipes
  private TabHost tabHost;
  private int currentTab = 0;
  private static final int MIN_TAB = 0;
  private static final int MAX_TAB = 3;
  private GestureDetector gestureDetector;

  private final HashMap<String, View> uiWidgets;

  /** Intent, used to start Data Service */
  private Intent dataIntent;

//  /** Message Broadcaster to send intents to data service */
//  private LocalBroadcastManager messageBroadcaster;
  private DashMessages dashMessages;

  /** A timer to update the UI occasionally. */
  private Handler uiTimer;
  /** Update the UI every n mSeconds. */
  private static final int UI_UPDATE_EVERY = 500;

  /** Use this to count uiTimer intervals during which no data have been received. */
  private int uiResetCounter = 0;
  /**
   * If the counter exceeds UI_RESET_AFTER, reset the UI
   * (i.e. clear old data which we assume is no longer valid).
   */
  private static final int UI_RESET_AFTER = 6;
  private int chargeNodeResetCounter = 0;
  private static final int CHARGENODE_RESET_AFTER = 20;

  /** Sent by another class when they have a brief message they would like displayed. */
  public static final int UI_TOAST_MESSAGE = 1;

  public static final String PREFS_NAME = "TumanakoDashPrefs";

  public static final int TUMANAKO_UI = 1;
  public static final String UI_INTENT_IN  = "com.tumanako.ui.intentin";
  public static final String UI_INTENT_OUT  = "com.tumanako.ui.intentout";

  // ---------------DEMO MODE CODE -------------------------------
  private boolean isDemo = false;  // Demo mode flag!
  // ---------------DEMO MODE CODE -------------------------------

  public UIActivity() {
    this.uiTimer = new Handler();
    this.uiWidgets = new HashMap<String, View>();
  }

  /**
   * Create UI.
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // ***** Set up tabs ****
    tabHost = (TabHost)findViewById(R.id.tabhost);
    tabHost.setup();

    TabSpec tabMainData = tabHost.newTabSpec("Main Data");
    tabMainData.setContent(R.id.layoutPrimaryData);
    tabMainData.setIndicator("Main");

    TabSpec tabSecondaryData = tabHost.newTabSpec("Secondary Data");
    tabSecondaryData.setIndicator("Secondary");
    tabSecondaryData.setContent(R.id.layoutSecondaryData);

    TabSpec tabSystemData = tabHost.newTabSpec("System Data");
    tabSystemData.setIndicator("System");
    tabSystemData.setContent(R.id.layoutSystemData);

    TabSpec tabChargeNode = tabHost.newTabSpec("Charge Node");
    tabChargeNode.setIndicator("Charging");
    tabChargeNode.setContent(R.id.layoutChaqrgeNode);

    tabHost.addTab(tabMainData);
    tabHost.addTab(tabSecondaryData);
    tabHost.addTab(tabSystemData);
    tabHost.addTab(tabChargeNode);

    // --DEBUG!!--
    Log.i(APP_TAG,"UIActivity -> onCreate()");

    // **** Calc height of dials based on width: ****************
    //int thisWidth =   ((Dial) findViewById(R.id.dialMotorRPM)).getWidth();
    //((Dial) findViewById(R.id.dialMotorRPM)).setLayoutParams(new LayoutParams(thisWidth, (int)((double)thisWidth * 0.58)));

    // ********** Make a list of available UI widgets: *****************************
    // Primary Data:
    uiWidgets.put( "lampData",           findViewById(R.id.lampData)           );
    uiWidgets.put( "lampGPS",            findViewById(R.id.lampGPS)            );
    uiWidgets.put( "lampContactor",      findViewById(R.id.lampContactor)      );
    uiWidgets.put( "lampFault",          findViewById(R.id.lampFault)          );
    uiWidgets.put( "lampReverse",        findViewById(R.id.lampReverse)        );
    uiWidgets.put( "dialMotorRPM",       findViewById(R.id.dialMotorRPM)       );
    uiWidgets.put( "dialMainBatteryKWh", findViewById(R.id.dialMainBatteryKWh) );
    uiWidgets.put( "barTMotor",          findViewById(R.id.barTMotor)          );
    uiWidgets.put( "barTController",     findViewById(R.id.barTController)     );
    uiWidgets.put( "barTBattery",        findViewById(R.id.barTBattery)        );
    // Secondary Data:
    uiWidgets.put( "textDriveTime",      findViewById(R.id.textDriveTime)      );
    uiWidgets.put( "textDriveRange",     findViewById(R.id.textDriveRange)     );
    uiWidgets.put( "textAccBatteryVlts", findViewById(R.id.textAccBatteryVlts) );
    // System Data:
    uiWidgets.put( "lampPreCharge",      findViewById(R.id.lampPreCharge)      );
    uiWidgets.put( "textMainBattVlts",   findViewById(R.id.textMainBattVlts)   );
    uiWidgets.put( "textMainBattAH",     findViewById(R.id.textMainBattAH)     );
    // Charge Node:
    uiWidgets.put( "editTextUser",         findViewById(R.id.editTextUser)         );
    uiWidgets.put( "editTextPassword",     findViewById(R.id.editTextPassword)     );
    uiWidgets.put( "webChargeNodeContent", findViewById(R.id.webChargeNodeContent) );
    uiWidgets.put( "lampChargeNodeOnline", findViewById(R.id.lampChargeNodeOnline) );
    uiWidgets.put( "lampCharging",         findViewById(R.id.lampCharging)         );
    uiWidgets.put( "buttonConnectToNode",  findViewById(R.id.buttonConnectToNode)  );
    uiWidgets.put( "buttonChargeStart",    findViewById(R.id.buttonChargeStart)    );
    uiWidgets.put( "buttonChargeStop",     findViewById(R.id.buttonChargeStop)     );
    // ---- Connect click and gesture listeners: -------
    gestureDetector = new GestureDetector(new SimpleSwiper(this));
    tabHost.setOnTouchListener(this);

    ((Button)uiWidgets.get("buttonConnectToNode")).setOnClickListener(this);
    ((Button)uiWidgets.get("buttonChargeStart")).setOnClickListener(this);
    ((Button)uiWidgets.get("buttonChargeStop")).setOnClickListener(this);


    // ---- Create a Data Service intent: ------
    dataIntent = new Intent(this, DataService.class);

    // Get a Broadcast Manager so we can send out messages to other parts of the app.
    dashMessages = new DashMessages( this, this, UI_INTENT_IN );

    // -------- Restore Saved Preferences (if any): -------------------
    /*
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    totalEnergy    = 25.0;  //settings.getFloat( "totalEnergy", 0 );
    totalDistance  = settings.getFloat( "totalDistance", 0 );
    */


    // -------- Restore saved instence state if one exists: ---------------------
    if (savedInstanceState != null) {
      currentTab = savedInstanceState.getInt( "currentTab" );
      // ---------------DEMO MODE CODE -------------------------------
      isDemo     = savedInstanceState.getBoolean( "isDemo" );
      // ---------------DEMO MODE CODE -------------------------------
    }
    // --------------------------------------------------------------------------
  }

  /*
   * Next Screen and Previous Screen.
   *
   * These methods switch between the tabs for the various
   * UI screens (Primary Data, Secondary data, System Data).
   *
   * currentTab is the index of the currently visible tab.
   */
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

  /** Save a copy of some data to the Preferences */
  private void saveSettings()
  {
    /*
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putInteger("isDemo", false);
    editor.commit(); // Commit the edits!
    */
  }

  // ---------------DEMO MODE CODE -------------------------------
  private void startDemo()
  {
    // Send 'DEMO' intent to data service:
    Log.i(APP_TAG,"UIActivity -> startDemo()");
    dashMessages.sendData( DataService.DATA_SERVICE, DataService.DATA_SERVICE_DEMO,1f, null, null );
    isDemo = true;
  }

  private void stopDemo()
  {
    // Send 'DEMO' intent to data service:
    dashMessages.sendData( DataService.DATA_SERVICE, DataService.DATA_SERVICE_DEMO,0f, null, null );
    isDemo = false;
  }
  // ---------------DEMO MODE CODE -------------------------------

  /**
   * Returns Hours.
   * Given a decimal number of hours (e.g. 1.5), return the
   * number of hours as an integer (i.e. truncate the fractional part).
   * @param decimalHours - Decimal Hours (i.e. 1.5 = 1 Hr 30 Mins).
   * @return
   */
  private int getHours(float decimalHours)
  {
    return (int)(decimalHours);
  }

  /**
   * Returns Minutes.
   * Given a decimal number of hours (e.g. 1.5), return the
   * number of minutes as an integer (i.e. fractional part multiplied by 60)
   * @param decimalHours - Decimal Hours (i.e. 1.5 = 1 Hr 30 Mins).
   * @return
   */
  private int getMinutes(float decimalHours)
  {
    return (int)((decimalHours - (float)((int)(decimalHours))) * 60);
  }

  /** Toast Message */
  private void showMessage(String ThisMessage)
  {
    // Displays pop-up TOAST message
    Toast.makeText(getApplicationContext(), ThisMessage, Toast.LENGTH_SHORT).show();
  }

  /** Reset UI to default */
  private void uiReset()
  {
    // Primary Data:
    ((Dial)uiWidgets.get("dialMotorRPM"))        .setValue(0f);
    ((Dial)uiWidgets.get("dialMainBatteryKWh"))  .setValue(0f);
    ((BarGauge)uiWidgets.get("barTMotor"))       .setValue(0f);
    ((BarGauge)uiWidgets.get("barTController"))  .setValue(0f);
    ((BarGauge)uiWidgets.get("barTBattery"))     .setValue(0f);
    ((StatusLamp)uiWidgets.get("lampData")).turnOff();
    ((StatusLamp)uiWidgets.get("lampGPS")).turnOff();
    ((StatusLamp)uiWidgets.get("lampContactor")).turnOff();
    ((StatusLamp)uiWidgets.get("lampFault")).turnOff();
    ((StatusLamp)uiWidgets.get("lampReverse")).turnOff();
    // Secondary Data:
    ((TextWithLabel)uiWidgets.get("textDriveTime"))      .setText("00:00");
    ((TextWithLabel)uiWidgets.get("textDriveRange"))     .setText("0");
    ((TextWithLabel)uiWidgets.get("textAccBatteryVlts")) .setText("0.0");
    // System Data:
    ((TextWithLabel)uiWidgets.get("textMainBattVlts"))   .setText("0.0");
    ((TextWithLabel)uiWidgets.get("textMainBattAH"))     .setText("0.0");
    ((StatusLamp)uiWidgets.get("lampPreCharge")).turnOff();
  }

  private void chargeNodeUIReset()
  {
    // Charge Node UI Reset
    //--DEBUG!--
    Log.i(APP_TAG,"UIActivity -> chargeNodeUIReset()");
    ((StatusLamp)uiWidgets.get("lampChargeNodeOnline")).turnOff();
    ((StatusLamp)uiWidgets.get("lampCharging")).turnOff();
    ((WebView)uiWidgets.get("webChargeNodeContent")).loadData(ChargeNode.CHARGE_NODE_DEFAULT_HTML, "text/html", null);
    ((Button)uiWidgets.get("buttonConnectToNode")).setText("Connect");
  }

  /** Touch Event: Used to detect 'swipes' */
  @Override
  public boolean onTouch(View v, MotionEvent event)
  {
    // When a Touch event is detected, we pass the event to the
    // gestureDetector (created earlier). We've previously told
    // the gestureDetector to use our custom gesture detector
    // defined in SimpleSwiper.java (see  constructor).
    return gestureDetector.onTouchEvent(event);
  }

  /**
   * Click Event.
   * Can be used with buttons and other controls.
   */
  @Override
  public void onClick(View viewClicked)
  {
    // Process button click events for this activity:
    //  This general OnClick handler is called for all the buttons.
    //  The code checks the ID of the view which generated the event
    //  (i.e. the button) and takes the appropriate action.

    //Get the User and Password values from the Charge Node form and place in a bundle
    Bundle chargeUIData = new Bundle();
    chargeUIData.putString("j_username", ((EditText)uiWidgets.get("editTextUser")).getText().toString() );
    chargeUIData.putString("j_password", ((EditText)uiWidgets.get("editTextPassword")).getText().toString() );

    switch (viewClicked.getId()) {
      case R.id.buttonConnectToNode:
        // Send intent to ChargeNode class, including the login details.
        dashMessages.sendData( ChargeNode.CHARGE_NODE_INTENT, ChargeNode.CHARGE_NODE_CONNECT, null, null, chargeUIData );
        break;
      case R.id.buttonChargeStart:
        dashMessages.sendData( ChargeNode.CHARGE_NODE_INTENT, ChargeNode.CHARGE_NODE_CHARGESTART, null, null, chargeUIData );
        break;
      case R.id.buttonChargeStop:
        dashMessages.sendData( ChargeNode.CHARGE_NODE_INTENT, ChargeNode.CHARGE_NODE_CHARGESTOP, null, null, chargeUIData );
        break;
    }
  }


  /** LOOOONG Click Action Handler */
  public boolean onLongClick(View MyView)
  {
    // Process long press events for this activity:
    // (Could use to reset trip data)
    switch (MyView.getId()) {
    /*
      case R.id.textviewDistIndicator:
        totalEnergy    = 0;
        totalDistance  = 0;
        break;
     */
    }
    return true;
  }

  /** Menu Click Action Handler */
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Check to see which menu item was selected
    switch (item.getItemId()) {
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
      case R.id.menuitemShowCharge:
        tabHost.setCurrentTab(3);
        currentTab = 3;
        return true;

      // ---------------DEMO MODE CODE -------------------------------
      case R.id.menuitemDemoMode:
        if (isDemo) {
          showMessage("Stop Demo.");
          stopDemo();
        } else {
          showMessage("Start Demo!");
          startDemo();
        }
        return true;
      // ---------------DEMO MODE CODE -------------------------------
      case R.id.menuitemSettings:
        // Show Settings screen:
        Intent myIntent = new Intent(this, SettingsActivity.class);
        startActivityForResult(myIntent, 0);
        return true;
      case R.id.menuitemClose:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }



  /**
   * Menu Create Event.
   * We've been told that this is a good time to draw the menu.
   * Create it from the XML file
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.options_menu, menu);
  return true;
  }

  /**
   * UI Resume Event.
   * UI has restarted after being in background
   * (also called when UI started for the first time).
   */
  @Override
  protected void onResume()
  {
    Log.d(APP_TAG,"UIActivity -> onResume()");
    Log.d(APP_TAG,"     State: currentTab = " + currentTab + "; isDemo = " + isDemo );
    super.onResume();
    dashMessages.resume();

    // Register to receive messages via Intents
    //LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(IDroidSensor.SENSOR_INTENT_ACTION));
    // We are registering an observer (messageReceiver) to receive Intents
    // with actions named {@link IDroidSensor#SENSOR_INTENT_ACTION}
    // (see {@link IDroidSensor} for constant definitions).

    // Start the data server (in case it's not already going; doesn't matter if it is).
    startService(dataIntent);
    uiReset();
    tabHost.setCurrentTab(currentTab);
    // ---------------DEMO MODE CODE -------------------------------
    if (isDemo) startDemo();
    else        stopDemo();
    // ---------------DEMO MODE CODE -------------------------------
    // Make sure there is no active callback already
    uiTimer.removeCallbacks(uiTimerTask);
    // Callback in n seconds
    uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);
  }

  /**
   * Restore Instance State.
   * Called when UI is recovered after being in background, but
   * NOT recreated from scratch.
   */
  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState)
  {
    Log.d(APP_TAG,"UIActivity -> onRestoreInstanceState()");
    super.onRestoreInstanceState(savedInstanceState);
    // Restore UI state from the savedInstanceState.
    currentTab = savedInstanceState.getInt( "currentTab" );
    // ---------------DEMO MODE CODE -------------------------------
    isDemo     = savedInstanceState.getBoolean( "isDemo" );
    // ---------------DEMO MODE CODE -------------------------------
    Log.d(APP_TAG,"UIActivity -> Restore State: currentTab = " + currentTab + "; isDemo = " + isDemo );
  }

  /**
   * UI Pause Event.
   * Activity has gone into the background.
   */
  @Override
  protected void onPause()
  {
    Log.d(APP_TAG,"UIActivity -> onPause()");
    super.onPause();
    dashMessages.suspend();
    uiTimer.removeCallbacks(uiTimerTask); // Make sure there is no active callback already
  }

  /**
   * Save State Event.
   * Save state info before the application is hidden / rotated / etc
   * (or otherwise trashed by the OS).
   */
  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    Log.d(APP_TAG,"UIActivity -> onSaveInstanceState()");
    super.onSaveInstanceState(outState);
    outState.putInt( "currentTab",  currentTab );
    // ---------------DEMO MODE CODE -------------------------------
    outState.putBoolean( "isDemo", isDemo );
    // ---------------DEMO MODE CODE -------------------------------
  }

  /**
   * UI Stop Event.
   * Activity is about to be destroyed.
   * Save Preferences.
   */
  @Override
  protected void onStop()
  {
    Log.d(APP_TAG,"UIActivity -> onStop()");
    // ---------------DEMO MODE CODE -------------------------------
    stopDemo();
    // ---------------DEMO MODE CODE -------------------------------
    super.onStop();
    saveSettings();
  }


  /**
   * UI Timer Handling Runnable.
   * Creates a Runnable which will be called after a delay,
   * to update the UI.
   * This runnable creates a timer to update the UI.
   * Note that this is a low priority UI update for
   * triggering a keep-alive signal to data service.
   */
  private Runnable uiTimerTask = new Runnable()
  {
    @Override
    public void run()
    {
      // Send Keep Alive to data service, etc:
      dashMessages.sendData(DataService.DATA_SERVICE, DataService.DATA_SERVICE_KEEPALIVE, null, null, null);
      dashMessages.sendData(VehicleData.VEHICLE_DATA, VehicleData.VEHICLE_DATA_KEEPALIVE, null, null, null);
      if (currentTab == 3) {
        dashMessages.sendData(ChargeNode.CHARGE_NODE_INTENT,ChargeNode.CHARGE_NODE_KEEPALIVE, null, null, null);
      }
      // XXX We only sent keep-alives to the ChargeNode class when the user is on the charge node page.
      // The effect of this is that they will disconnect from the charger if they leave the charge node page.
      // May want to change later - depends on how the charging interface works (this is just a demo).

      // Update UI Reset Counter
      uiResetCounter++;
      if (uiResetCounter > UI_RESET_AFTER) {
        uiResetCounter = 0;
        uiReset(); // Reset the UI if we receive no data for a certain time interval.
      }
      chargeNodeResetCounter++;
      if (chargeNodeResetCounter > CHARGENODE_RESET_AFTER) {
        chargeNodeResetCounter = 0;
        chargeNodeUIReset();
      }

      // Start the timer for next UI Update
      //   Make sure there is no active callback already
      uiTimer.removeCallbacks(uiTimerTask);
      //   Callback later!
      uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);
    }
  };

  @Override
  public void messageReceived(String action, int message, Float floatData, String stringData, Bundle data)
  {
    // --DEBUG!-- Log.i(APP_TAG, String.format( "UIActivity -> Message Rec; Mesage: %d; ", message) + action);
    // --DEBUG!-- if (stringData != null) Log.i(APP_TAG, "stringData -> " + stringData);
    uiResetCounter = 0;   // Reset the UI Reset Counter whenever we get some data from an input source.

    if (message == UI_TOAST_MESSAGE) {
      showMessage(stringData);
    }

    if (message == DashMessageListener.CHARGE_NODE_ID) {
      // Data Message from Charge Node
      // If string data is included in the Intent,
      // assume it's some HTML for the webview control to display
      //if (stringData != null) ((WebView)uiWidgets.get("webChargeNodeContent")).loadUrl(stringData);
      chargeNodeResetCounter = 0;
      if (stringData != null) ((WebView)uiWidgets.get("webChargeNodeContent")).loadData(stringData, "text/html", null);
      // Get the Current and AH values from the data:
      //Float chargeCurrent = data.getFloat(ChargeNode.CHARGE_CURRENT, 0.0f);
      //Float chargeAH      = data.getFloat(ChargeNode.CHARGE_AH, 0.0f);
      // Get the status lamp values:
      if (data.getFloat(ChargeNode.CONNECTION_STATUS, 0.0f) == 0.0f ) {
        ((StatusLamp)uiWidgets.get("lampChargeNodeOnline")).turnOff();
        ((Button)uiWidgets.get("buttonConnectToNode")).setText("Connect");
      } else {
        ((StatusLamp)uiWidgets.get("lampChargeNodeOnline")).turnOn();
        ((Button)uiWidgets.get("buttonConnectToNode")).setText("Disconnect");
      }
      if (data.getFloat(ChargeNode.CHARGE_STATUS, 0.0f) == 0.0f ) {
        ((StatusLamp)uiWidgets.get("lampCharging")).turnOff();
      } else {
        ((StatusLamp)uiWidgets.get("lampCharging")).turnOn();
      }
      //((TextWithLabel)uiWidgets.get("textChargeCurrent")) .setText(String.format("%.0f",chargeCurrent));
      //((TextWithLabel)uiWidgets.get("textChargeAH"))      .setText(String.format("%.1f",chargeAH)     );
    }

    if (message == DashMessageListener.VEHICLE_DATA_ID) {
      // Data from the Vehicle Data senor
      //   Get a list of data keys in the bundle of submitted data.
      Set<String> keys = data.keySet();
      //   This is an iterator to iterate over the list.
      Iterator<String> myIterator = keys.iterator();
      String key;
      float valueFloat;
      while (myIterator.hasNext()) {
        key = myIterator.next();
        valueFloat = data.getFloat(key, 0.0f);

        // Data Messages from vehicle data input
        // TODO refactor this into an else-if chain
        if (key.equals("DATA_MOTOR_RPM"))         ((Dial)uiWidgets.get("dialMotorRPM"))                .setValue(valueFloat / 1000);
        if (key.equals("DATA_MAIN_BATTERY_KWH"))  ((Dial)uiWidgets.get("dialMainBatteryKWh"))          .setValue(valueFloat       );

        if (key.equals("DATA_MOTOR_TEMP"))        ((BarGauge)uiWidgets.get("barTMotor"))               .setValue(valueFloat);
        if (key.equals("DATA_CONTROLLER_TEMP"))   ((BarGauge)uiWidgets.get("barTController"))          .setValue(valueFloat);
        if (key.equals("DATA_MAIN_BATTERY_TEMP")) ((BarGauge)uiWidgets.get("barTBattery"))             .setValue(valueFloat);

        if (key.equals("DATA_ACC_BATTERY_VLT"))   ((TextWithLabel)uiWidgets.get("textAccBatteryVlts")) .setText(String.format("%.1f", valueFloat));

        if (key.equals("DATA_DRIVE_TIME"))        ((TextWithLabel)uiWidgets.get("textDriveTime"))      .setText(String.format("%1d:%02d", getHours(valueFloat), getMinutes(valueFloat)));
        if (key.equals("DATA_DRIVE_RANGE"))       ((TextWithLabel)uiWidgets.get("textDriveRange"))     .setText(String.format("%.0f", valueFloat));

        if (key.equals("DATA_MAIN_BATTERY_VLT"))  ((TextWithLabel)uiWidgets.get("textMainBattVlts"))   .setText(String.format("%.1f", valueFloat));
        if (key.equals("DATA_MAIN_BATTERY_AH"))   ((TextWithLabel)uiWidgets.get("textMainBattAH"))     .setText(String.format("%.1f", valueFloat));

        if (key.equals("DATA_DATA_OK")) {
          if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampData")).turnOn();
          else                  ((StatusLamp)uiWidgets.get("lampData")).turnOff();
        }

        if (key.equals(NmeaProcessor.DATA_GPS_HAS_LOCK)) {
          if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampGPS")).turnOn();
          else                  ((StatusLamp)uiWidgets.get("lampGPS")).turnOff();
        }

        if (key.equals("DATA_CONTACTOR_ON")) {
          if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampContactor")).turnOn();
          else                  ((StatusLamp)uiWidgets.get("lampContactor")).turnOff();
        }

        if (key.equals("DATA_FAULT")) {
          if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampFault")).turnOn();
          else                  ((StatusLamp)uiWidgets.get("lampFault")).turnOff();
        }

        if (key.equals("DATA_PRECHARGE")) {
          if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampPreCharge")).turnOn();
          else                  ((StatusLamp)uiWidgets.get("lampPreCharge")).turnOff();
        }

        if (key.equals("DATA_MOTOR_REVERSE")) {
          if (valueFloat == 1f) ((StatusLamp)uiWidgets.get("lampReverse")).turnOn();
          else                  ((StatusLamp)uiWidgets.get("lampReverse")).turnOff();
        }
      }
    }
  }
}
