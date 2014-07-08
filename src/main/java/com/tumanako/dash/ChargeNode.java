package com.tumanako.dash;


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


import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


/***********************************************************************
 * Charge Node Module: 
 *
 * A WebView control displays data supplied by the charge node (HTML). 
 * Buttons in the UI generate intents which this class catches.
 *  
 *  -Connect to Node: Look for the server and display its web page
 *   if found. NOTE: Must already have a WiFi connection to the node. 
 * 
 *  -Start Charge: Tell the node that we want some power!
 *  
 *  -Stop Charge: Tell the node that we've had enough. 
 *  
 *  While connected to the node, we will periodically poll it to 
 *  get some status information. 
 *
 * A seperate thread will be used to handle network operations. beware
 * thread safety!
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 **********************************************************************/

public class ChargeNode implements IDashMessages
  {

public static Integer instanceCount;
private Integer thisInstance = 0;
  
  /************* Outgoing Intent action: ***************************************************/
  public static final String CHARGE_NODE   = "com.tumanako.chargenode"; 
  
  /************* Message IDs: Used to identify incomming intents. **************************/
  public static final String CHARGE_NODE_KEEPALIVE    = "com.tumanako.chargenode.keepalive";      // } Send by the UI: tells us to keep alive.
  public static final String CHARGE_NODE_CONNECT      = "com.tumanako.chargenode.connect";        // }                 tells us to connect to server. 
  public static final String CHARGE_NODE_CHARGESTART  = "com.tumanako.chargenode.chg_start";      // }                 tells us to start charging. 
  public static final String CHARGE_NODE_CHARGESTOP   = "com.tumanako.chargenode.chg_stop";       // }                 tells us to stop charging.
  public static final String CHARGE_NODE_HTML_DATA    = "com.tumanako.chargenode.HTMLData";       // } Passed to, and returned by, the ChargerHTTPConn classs.
  public static final String CHARGE_NODE_JSON_DATA    = "com.tumanako.chargenode.JSONData";       // } Indicate the data type we expect back from the request.
  private static final String intentFilters[] = 
    {
    CHARGE_NODE_KEEPALIVE,
    CHARGE_NODE_CONNECT,
    CHARGE_NODE_CHARGESTART,
    CHARGE_NODE_CHARGESTOP,
    CHARGE_NODE_HTML_DATA,
    CHARGE_NODE_JSON_DATA,
    ChargerHTTPConn.HTTP_ERROR
    };

  /************* Charger Data Tags: **************************************/
  public static final String CONNECTION_STATUS = "CONSTAT"; //
  public static final String CHARGE_STATUS     = "CHGSTAT"; //  
  public static final String CHARGE_CURRENT    = "CHGCUR";  // These are used as keys when sending data to the UI as a bundle. 
  public static final String CHARGE_AH         = "CHGAH";   //

  /************ Status Constants: **************************************/
  public static final int STATUS_OFFLINE      = 0;
  public static final int STATUS_CONNECTING   = 1;
  public static final int STATUS_CONNECTED    = 2;
  public static final int STATUS_NOT_CHARGING = 3;
  public static final int STATUS_CHARGING     = 4;
  
  /************ Short HTML Responses: ********************************/
  public static final String CHARGE_NODE_DEFAULT_HTML    = "<html><body></body></html>";
  public static final String CHARGE_NODE_CONNECT_HTML    = "<html><body><p>Connecting...</p></body></html>";  
  public static final String CHARGE_NODE_CONNECTED_HTML  = "<html><body><p>Connected!</p></body></html>";  
  public static final String CHARGE_NODE_CONNERROR_HTML  = "<html><body><p>Connection Error.</p></body></html>";
  public static final String CHARGE_NODE_UPDATING_HTML   = "<html><body><p>Updating...</p></body></html>";
  public static final String CHARGE_NODE_OK_HTML         = "<html><body><p>OK</p></body></html>";

  /************ Newton Road Charge Station Setup Constants: *************/
  private static final String NODE_ID          = "30";                 // Node ID for the site
  private static final String SWITCH_SOURCE_ID = "/power/switch/3";    // Look for this "SourceID" in JSON data to find switch status. 

  /************ Solare Network URIs: ***************/
  private static final String SN_HOST     = "data.solarnetwork.net";  // HTTP Hostname for solarnetwork server
  private static final String PING_URL    = "http://data.solarnetwork.net/solarquery/api/v1/sec/datum/mostRecent?nodeId=30&type=HardwareControl";
  private static final String CONTROL_URL = "http://data.solarnetwork.net/solaruser/api/v1/sec/instr/add";
  
  private DashMessages dashMessages;
  
  private static final int UPDATE_TIME = 500;              // UI Update timer interval (mS)
  private static final int SEND_PING_EVERY = 10;           // Send a 'ping' to get data from the server every n intervals of the update timer
  private static final int WATCHDOG_OVERFLOW = 8;          // Max watchdog counter value - the chargenode timer will automatically stop if we don't get a 'keepalive' message.   
  
  private final Handler updateTimer = new Handler();       // Message handler for update timer (sends data periodically)
  
  private int connectionStatus = STATUS_OFFLINE;
  private int chargeStatus = STATUS_NOT_CHARGING;
  
  private volatile int watchdogCounter = 0;                         // This counter will increment on the timer. If not reset (e.g. by Keep Alive message), the timer will stop itself.
  private int pingCounter = 0;                             // This counter will increment on the timer. When it reaches SEND_PING_EVERY, a PING is sent to get data from the server. 

  
  private final WeakReference<Context> weakContext; 
  
  private Bundle cookieData = new Bundle();       // Used to keep track of cookie data returned by the server, so we can keep a session going. 
  
  private Queue<ChargerHTTPConn> requestQueue = new LinkedList<ChargerHTTPConn>();    // Used to maintain a list of queued HTTP requests. 

  private String token  = "";   // Security token
  private String secret = "";   // Security secret / password
  
  
  /****** Constructor ***********************/
  public ChargeNode(Context context)
    {
if (instanceCount == null) instanceCount = 1;
else                       instanceCount++;
this.thisInstance = instanceCount;
Log.i(com.tumanako.ui.UIActivity.APP_TAG, String.format( " ChargeNode -> Constructor (%d of %d)", this.thisInstance,instanceCount ) );

    dashMessages = new DashMessages(context,this, intentFilters );
    weakContext = new WeakReference<Context>(context);    
    resume();      // Start the update timer!    
    }
  

  
  /******** Suspend and Resume: ************************
   * Suspend should be called when the UI is paused, to 
   * stop timers, etc. 
   * Resume should be called when the UI is reloaded. 
   ****************************************************/
  public void suspend()
    {
    chargeStatus = STATUS_NOT_CHARGING;
    connectionStatus = STATUS_OFFLINE;
    timerStop();                        //...Suspends the update timer.   
    }
 
  
  public void resume()
    {
    //chargeStatus = STATUS_NOT_CHARGING;
    //connectionStatus = STATUS_OFFLINE;
    timerStart();                      // ...Restarts the update timer.
    }

  
  
  /******* Public Get Status Methods: ***************************
   * Returns the status of the ChargeNode, so other parts of the app
   * can decide what to do.
   * (I.e. whether to reset the charge node UI page)  
   *************************************************************/
  public int getConnectionStatus()
    {  return connectionStatus;  }
  
  public int getChargeStatus()
    {  return chargeStatus;  }
  
  
  
  
  
  
  /*********** Intent Processor: ***********************************************************************************************************/ 
  public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData )
    {
    // --DEBUG!--    
    //Log.i(com.tumanako.ui.UIActivity.APP_TAG, String.format( " ChargeNode (%d) -> Msg Rec: %s", this.thisInstance,  action) );    
    // Message received! Reset the watchdog counter. 
    watchdogCounter = 0;
    
    // --- Check to see if any cookie data have been received (e.g. from a web server): --- 
    if ( (bundleData != null) && (bundleData.containsKey("Cookies")) )
      {
      Bundle tempCookies = bundleData.getBundle("Cookies");
      if (!tempCookies.isEmpty()) cookieData = new Bundle(tempCookies); 
      }
  
    // --- Get the security token and secret (if supplied): ---
    if (bundleData != null)
      {
      if (bundleData.containsKey("j_token"))    token  = bundleData.getString("j_token");
      if (bundleData.containsKey("j_password")) secret = bundleData.getString("j_password");
      }
        
    
    /************** Messages from the UI: *****************************/
    ///////// Connect / Disconnect /////////////////////////////////////
    if (action.equals(CHARGE_NODE_CONNECT))
      {
      // This is actually Connect OR Disconnect (depending on current status).
      switch (connectionStatus)
        {
        case STATUS_OFFLINE:
          // OFFLINE: Try to connect:
          dashMessages.sendData( CHARGE_NODE, null, null, CHARGE_NODE_CONNECT_HTML, makeChargeData(STATUS_OFFLINE,STATUS_NOT_CHARGING,0f,0f) );   // Clear old UI data.
          connectionStatus = STATUS_CONNECTED; //STATUS_CONNECTING;
          doPing();        // Send a ping request to test the connection 
          timerStart();    // Make sure the update timer is running!
          break;
          
        case STATUS_CONNECTING:
          // In the process of connecting now. Give up. 
          doStop();
          timerStart();    // Make sure the update timer is running (ironically, need this to process the stop)!
          chargeStatus = STATUS_NOT_CHARGING;
          connectionStatus = STATUS_OFFLINE;
          dashMessages.sendData( CHARGE_NODE, null, null, CHARGE_NODE_CONNERROR_HTML, makeChargeData(connectionStatus,chargeStatus,0.0f,0.0f) );
          break;
          
        case STATUS_CONNECTED:
          // Already connected: Disconnect.
          if (chargeStatus == STATUS_CHARGING) doChargeSet(0);  // Stop the charger. 
          doStop();
          timerStart();    // Make sure the update timer is running (ironically, need this to process the stop)!          
          break;
        
        }
      }  // [if (action.equals(CHARGE_NODE_CONNECT))]
    ///////// Charge START /////////////////////////////////////        
    if (action.equals(CHARGE_NODE_CHARGESTART))
      {
      if (connectionStatus == STATUS_CONNECTED) doChargeSet(1);  // Turn ON the charger!  
      }
    ///////// Charge STOP /////////////////////////////////////    
    if (action.equals(CHARGE_NODE_CHARGESTOP))
      {
      if (connectionStatus == STATUS_CONNECTED) doChargeSet(0);  // Turn off the charger!  
      }
      

    
    /************** HTML Data from the Comm thread: ****************************/        
    if (action.equals(CHARGE_NODE_HTML_DATA))
      {
      // HTTP response received from server.
/*
      if ( (connectionStatus == STATUS_CONNECTING) && (bundleData != null) && (bundleData.containsKey("ResponseCode")) )
        {
        // --DEBUG!--
Log.i(com.tumanako.ui.UIActivity.APP_TAG, String.format( " ChargeNode -> HTTP Response Code: %d", bundleData.getInt("ResponseCode")) );
        // Check the r4esponse code. Should be 200 if we logged in OK:
        if (bundleData.getInt("ResponseCode",999) == 200)
          {
          // Connected OK!
          connectionStatus = STATUS_CONNECTED;
          dashMessages.sendData( CHARGE_NODE, null, null, CHARGE_NODE_CONNECTED_HTML, makeChargeData(connectionStatus,chargeStatus,0.0f,0.0f) );
          timerStart();   // Make sure timer is running (for PING updates). 
          }
        else
          {
          // Login error. 
          connectionStatus = STATUS_OFFLINE;
          chargeStatus = STATUS_NOT_CHARGING;
          dashMessages.sendData( CHARGE_NODE, null, null, CHARGE_NODE_CONNERROR_HTML, makeChargeData(connectionStatus,chargeStatus,0.0f,0.0f) );
          timerStop();
          }
        }
      //serverPage = new String(stringData);
      //dashMessages.sendData( CHARGE_NODE, null, null, serverPage, getChargeData(STATUS_CONNECTED,0.0f,0.0f) );
 
*/
      }  // [if (action.equals(CHARGE_NODE_HTML_DATA))]
      

    
    /************** JSON Data from the Comm thread: ****************************/
    if (action.equals(CHARGE_NODE_JSON_DATA))
      {
      Log.i(com.tumanako.ui.UIActivity.APP_TAG, String.format( " ChargeNode -> HTTP Response Code: %d", bundleData.getInt("ResponseCode")) );          

      // Check for offline status: If we're offline, just ignore this status update. 
      if (chargeStatus != STATUS_OFFLINE)
        {
        // The text we received back should be JSON data. 
        // Parse: (note Try / Catch block to catch JSON errors, in case data can't be parsed).
        try
          {
         // connectionStatus = STATUS_OFFLINE;  // Default status (Offline) in case data parsing fails. 
          JSONObject jsonReceived = new JSONObject(stringData);  // Parse the JSON data
          JSONArray jsonDataSection = jsonReceived.getJSONArray("data");  // Get the "data" array.
          //--DEBUG!!-- 
          Log.i("HTTPConn", String.format("JSON Parsed. %d entries in data section.", jsonDataSection.length() ) );
          // The JSON data should contain 3 sections: 
          //   "datumQueryCommand" = JSONData: some name/value pairs with meta data
          //   "data"              = JSONArray: An array of JSON objects containing info about the items at
          //                         the solar node (i.e. switches). This is the data we are interested in.
          //   "tz"                = TIme zone description. 
          // Search the data array for an entry with sourceId = /power/switch/1:
          for ( int i=0; i<jsonDataSection.length(); i++ )
            {
            JSONObject jsonDataItem = jsonDataSection.getJSONObject(i);
            if ( (jsonDataItem.has("sourceId")) && (jsonDataItem.getString("sourceId").equals(SWITCH_SOURCE_ID) ) )
              {
              Log.i("HTTPConn", "  FOUND!!! Value = " + String.format("%d", jsonDataItem.getInt("integerValue") ) );
              if (jsonDataItem.getInt("integerValue") == 1) chargeStatus = STATUS_CHARGING;
              else                                          chargeStatus = STATUS_NOT_CHARGING;
              // If we got to here, then the JSON data was parsed ok. This means we have a good connection! 
              connectionStatus = STATUS_CONNECTED;
              dashMessages.sendData( CHARGE_NODE, null, null, CHARGE_NODE_OK_HTML, makeChargeData(connectionStatus,chargeStatus,0.0f,0.0f) );
              }
            }  // [for...]
          }  // [try]
        catch (Exception e)
          {  
          e.printStackTrace();
          chargeStatus = STATUS_NOT_CHARGING;
          connectionStatus = STATUS_OFFLINE;
          doStop();
          dashMessages.sendData( CHARGE_NODE, null, null, "", makeChargeData(connectionStatus,chargeStatus,0.0f,0.0f) ); 
          Log.i(com.tumanako.ui.UIActivity.APP_TAG, "Error Parsing JSON Data!"  );
          }
        }  // [if (chargeStatus != STATUS_OFFLINE)]
      }  // [if (action.equals(CHARGE_NODE_JSON_DATA))]
  
  
    if (action.equals(ChargerHTTPConn.HTTP_ERROR))
      {
      chargeStatus = STATUS_NOT_CHARGING;
      connectionStatus = STATUS_OFFLINE;
      dashMessages.sendData( CHARGE_NODE, null, null, CHARGE_NODE_CONNERROR_HTML, makeChargeData(connectionStatus,chargeStatus,0.0f,0.0f) );
      }  // [if (action.equals(CHARGE_NODE_CONN_ERROR))]
      

  }  // [...messageReceived(...)]
    
  
    
  
  
  
  
  
  
  
  
  
  
  
  
  
  /*********** Update Timer: *************************************************************************************/
  private void timerStop()
    {  updateTimer.removeCallbacks(updateTimerTask);   }
                      //...Suspends the update timer. 
  
  private void timerStart()
    {  updateTimer.postDelayed(updateTimerTask, UPDATE_TIME);  }
                      // ...Restarts the update timer.
    
  private Runnable updateTimerTask = new Runnable() 
   {
   // Creates a Runnable which will be called after a delay, to carry out a read of vehicle data. 
   public void run()  
     {
     timerStop();  // Clears existing timers.
     watchdogCounter++;
//--DEBUG!!--
Log.i(com.tumanako.ui.UIActivity.APP_TAG, " ChargeNode -> Tick. Counter:" + watchdogCounter );     
     if (watchdogCounter >= WATCHDOG_OVERFLOW)
       {
       // Nothing has happend for a while! Stop the timer and set status to 'Disconnected'.
       chargeStatus = STATUS_NOT_CHARGING;
       connectionStatus = STATUS_OFFLINE;  // Give up.
Log.i(com.tumanako.ui.UIActivity.APP_TAG, " ChargeNode -> Watchdog Overflow. Stopping. " );       
       }
     
     else
       {

       /***********************************************************************************************
       * HTTP Request Queue Management:
       *
       *  Check the status of the first object in the HTTP request queue. 
       *   * If it hasn't started, start it.
       *   * If it's running, do nothing. 
       *   * If it's finished, throw it away and start the next one!
       *   
       */
       if ( (requestQueue != null) && (!requestQueue.isEmpty())  )
         {
         // There is at least one HTTP request in the queue: 
         ChargerHTTPConn currentConn = requestQueue.peek();   // Get reference to current (first) item in the queue.
         if (currentConn.isStop()) 
           {
           // Special "STOP QUEUE" entry: 
           connectionStatus = STATUS_OFFLINE;
           currentConn = null;
           requestQueue.poll();   // Removes the "stop" item from the queue.
           }
         else
           {
           if (!currentConn.isAlive())
             {
             // Current item is not "Alive" (i.e. running). Has it started yet?
             if (currentConn.isRun())
               {
               //--DEBUG!!--- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " ChargeNode -> Thread Finished. Removing From Queue. " );             
               // The thread has run, so it must have finished! 
               currentConn = null;
               requestQueue.poll();   // Removes the item from the queue.
               }
             // NOW: If we still have a valid HTTP connection item from the queue, we know it needs to be started. 
             if (currentConn != null) 
               {
               //--DEBUG!!--- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " ChargeNode -> Starting HTTP Conn From Queue. " );             
               currentConn.run();           
               }
             }  // [if (!currentConn.isAlive())]
           }  // [if (currentConn.isStop()) ... else...]
         }  // [if (!requestQueue.isEmpty())]
       /***********************************************************************************************/
       
       
       switch (connectionStatus)
         {
         case STATUS_CONNECTING:
           // We are waiting for a response from the server:
           break;
           
         case STATUS_CONNECTED:
           // We are connected: Add to the Ping counter, and send a 'PING' if enough time has passed:
           pingCounter++;
           if (pingCounter >= SEND_PING_EVERY)
             {
             pingCounter = 0;
Log.i(com.tumanako.ui.UIActivity.APP_TAG, " ChargeNode -> Ping! " );
             dashMessages.sendData( CHARGE_NODE, null, null, CHARGE_NODE_UPDATING_HTML, makeChargeData(connectionStatus,chargeStatus,0.0f,0.0f) );
             doPing();
             }
           break;
           
         default: 
         }  // [switch (status)]
       if (connectionStatus != STATUS_OFFLINE) timerStart();  // Restarts the timer. 
       
       }  // [if (watchdogCounter >= WATCHDOG_OVERFLOW)...else]
     
     } 
   };
   /***************************************************************************************************************/
  
  
  
  
  
    
  
   
   
   
   
  
 
  /********** Controls: ************************************************************/
   
  
  /***** STOP the queue: *****************
   * This adds a "STOP" flag to the HTTP queue. When the "STOP" flag is
   * encountered, the timer is halted and no further queue processing 
   * will happen until it is restarted. 
   * Calling "doStop" allows any existing HTTP requests to be completed
   * before the timer is stopped.
   **************************************/  
  private void doStop()
    {
    requestQueue.add( new ChargerHTTPConn(  weakContext,
                                            "STOP", 
                                            null,
                                            null,
                                            null, 
                                            null,
                                            false,
                                            null,
                                            null )  );
    }
  
  
   
  /******** Ping / data update: ********************************
   * This method sends a 'most recent' datum request to the solar networks server, 
   * in order to get back the current state of the switches. 
   * The request will return JSON data if all goes well. 
   *************************************************************/
  private void doPing()
    {
    requestQueue.add( new ChargerHTTPConn(  weakContext,
                                            CHARGE_NODE_JSON_DATA, 
                                            PING_URL,
                                            SN_HOST,
                                            null, 
                                            cookieData,
                                            false,
                                            token,
                                            secret )  );
    }
   
   
   
  /******** Charge Start / Stop: ********************************
   * Sends a control request to the SolarNetworks server to 
   * turn the charger on or off.
   * newSetting specifies whether to turn the switch ON or OFF 
   * (1 = ON, 0 = OFF). 
   *************************************************************/
  private void doChargeSet(int newSetting)
    {
    requestQueue.add( new ChargerHTTPConn( weakContext,
                                           CHARGE_NODE_HTML_DATA,
                                           CONTROL_URL,
                                           SN_HOST,
                                           makeChargeControl(newSetting),
                                           cookieData,
                                           false,
                                           token,
                                           secret )  );    
    }
  
  
  
  
  
  private Bundle makeChargeData(int conStatus, int chgStatus, float current, float ah)
    {
    // This makes a bundle of values containing the charge data: 
    // charge status, chrage current and charge amount (ah): 
    Bundle chargeData = new Bundle();
    chargeData.putFloat( CONNECTION_STATUS,  (conStatus==STATUS_CONNECTED) ? 1.0f : 0.0f  );
    chargeData.putFloat( CHARGE_STATUS,      (chgStatus==STATUS_CHARGING)  ? 1.0f : 0.0f  );
    chargeData.putFloat( CHARGE_CURRENT,     current    );
    chargeData.putFloat( CHARGE_AH,          ah         );
    return chargeData;
    }
  
  
  
/**** GET Method for setting switches: Doesn't work.   
  private String makeChargeControl(int setTo)
    {
    // This makes a URL string containing a querystyring to control a switch, 
    // in a format suitable for use in a GET request. 
    //  setTo specifies the new status: 0 = OFF, 1 = ON.  
    String chargeString = String.format(
               Locale.US,
               "%s?nodeId=%s&topic=SetControlParameter&parameters[0].name=%s&parameters[0].value=%d",
               CONTROL_URL,
               NODE_ID,
               SWITCH_SOURCE_ID,
               setTo       ); 
    return chargeString;
    }
*********/
    
  
  private Bundle makeChargeControl(int setTo)
    {
    // This makes a bundle of data (name / value pairs) which can be used in a 
    // POST HTTP request to set the charger status (ON or OFF). 
    //  setTo specifies the new status: 0 = OFF, 1 = ON.  
    Bundle chargeData = new Bundle();
    chargeData.putString( "nodeId",              NODE_ID                    );
    chargeData.putString( "topic",               "SetControlParameter"      );
    chargeData.putString( "parameters[0].name",  SWITCH_SOURCE_ID           );
    chargeData.putString( "parameters[0].value", String.format("%d",setTo)  );
    return chargeData;
    }
  

    
  
  }  // [class]
