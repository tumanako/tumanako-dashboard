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


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.tumanako.dash.DashMessages;
import com.tumanako.dash.IDashMessages;





/**
 UI Activity Base Class
 
 This class provides a base for all activities used in the dashboard UI. 
 
 It implements some core methods which all activities use (such as the 
 DashMessages handler to process intent messages). 
 
 Actual UI activity classes should extend this class with any additional 
 functionality specific to their needs. 

 @author Jeremy Cole-Baker / Riverhead Technology

*/
public class UIActivity extends Activity implements IDashMessages
  {
    
    public static final String APP_TAG   = "TumanakoDash";

    private final Handler updateTimer = new Handler();  // Internal update timer: creates a 'watchdog' interval to check for UI_UPDATED messages
    private int staleUICounter = 0;                     // Used to count timer intervals without UI update.
    private final int UPDATE_INTERVAL = 1000;           // Internal update timer tick every 1 second (1000 ms). 
    private final int UI_STALE_TIME = 3;                // Declare the UI to be 'stale' after this many ticks without update.
    
    
    /** Intent actions: These action strings are recognised by UI Activities. */
    public static final String UI_TOAST_MESSAGE = "UI_TOAST";    // Display a brief popup message
    public static final String UI_PING          = "UI_PING";     // UI Ping - If we receive this, we respond with a "pong"
    public static final String UI_PONG          = "UI_PONG";     // UI Pong - response to ping
    public static final String UI_UPDATED       = "UI_UPDATED";  // The UI has been updated. If we don't receive one of these for 3 seconds, the UI should be reset 
    public static final String UI_RESET         = "UI_RESET";    // Reset the UI (sent to widgets to reset them)
    public static final String UI_NOTHING       = "UI_NOTHING";  // Do nothing

    
    /** Preferences File Name: Used whenever a class wants to store or load 'preferences'. */ 
    public static final String PREFS_NAME = "TumanakoDashPrefs";

    
    // DashMessages class to send/receive Intents: 
    protected DashMessages dashMessages;

    protected boolean autoReset = true; 
      // Auto-Reset flag. If true (the default), the 'stale' timer defined above 
      // is used to reset the UI after n seconds. Some pages should NOT be reset 
      // automatically; for these pages, the derived activity class should set 
      // autoReset to false in their constructor. 

    
    // List of intent actions we will respond to (constants defined above):
    // These are the base filters for all activities; sub-classes may add
    // additional filters with 'setExtraIntentFilters'. This MUST be carried out
    // BEFORE onCreate is called. 
    private static final String baseIntentFilters[] = 
      {
      UI_PING, 
      UI_TOAST_MESSAGE,
      UI_UPDATED,
      UI_RESET,
      UI_NOTHING
      };
       
    private String extraIntentFilters[] = null;   // Extra intent filters (those added by sub classes). 
    
        

    
    
    
    /**
     Add extra intent filters to base list.  <p> 
     
     To be be effective, this method MUST be called before onCreate.
     
     @param extraFilters String array containing extra intent filters to 
                         add to the filter list
     
     */
    protected void setExtraIntentFilters(String extraFilters[])
      {
      extraIntentFilters = extraFilters.clone();
      }
    
    
    
    
    
    
    
    /**
     Create UI: Called when the activity is first created
     @param savedInstanceState Bundle of saved state data from previous instance
    */
    @Override
    public void onCreate(Bundle savedInstanceState) 
      {
      super.onCreate(savedInstanceState);

      // --DEBUG!!--
      Log.i(APP_TAG,"UIActivity -> onCreate()");
      

      // Add any extra intent filters to the base filters:
      String intentFilters[];
      if (null != extraIntentFilters)
        {
        int lengthBase  = baseIntentFilters.length;
        int lengthExtra = extraIntentFilters.length;
        intentFilters= new String[lengthBase + lengthExtra];
        System.arraycopy( baseIntentFilters,  0, intentFilters, 0, lengthBase  );
        System.arraycopy( extraIntentFilters, 0, intentFilters, lengthBase, lengthExtra );
        }
      else
        {
        // No additional filters defined. 
        intentFilters = baseIntentFilters; 
        }
      // Get a DashMessages class so we can send out messages to other parts of the app.
      dashMessages = new DashMessages( this, this, intentFilters );
      
      }


     
    
    
    
    
    
    /**
     UI Resume Event - UI has restarted after being in background.  <p> 
     
     Also called when UI started for the first time.
     */ 
    @Override
    protected void onResume() 
      {
      super.onResume();
      updateStart();
      dashMessages.resume();
      // --DEBUG!--
      Log.i(APP_TAG,"UIActivity -> onResume()");
      }

    
    
    
    
    
    
    
    /**
     Restore Instance State: Called when UI is recovered after being in background, but
     NOT recreated from scratch.
     @param savedInstanceState Bundle of saved state data from previous instance 
    */ 
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
      {
      // --DEBUG!--
      Log.i(APP_TAG,"UIActivity -> onRestoreInstanceState()");
      super.onRestoreInstanceState(savedInstanceState);
      }
    

    
    
    
    
    
    
    
    /**
     UI Pause Event - Called when the activity goes into the background 
     */
    @Override
    protected void onPause() 
      {
      // --DEBUG!--
      Log.i(APP_TAG,"UIActivity -> onPause()");
      super.onPause();
      dashMessages.suspend();
      updateStop();
      }

    
    
    
    
    
    
    
    
    /**
     Save State Event: Save state info before the application is hidden / rotated / etc 
     (or otherwise trashed by the OS)
     @param outState Bundle into which data can be placed. This bundle is stored for us by the OS. 
    */    
    public void onSaveInstanceState(Bundle outState)
      {
      // --DEBUG!--
      Log.i(APP_TAG,"UIActivity -> onSaveInstanceState()");
      super.onSaveInstanceState(outState);
      }
    



    
    
    
    
    /**
     UI Stop Event: Activity is about to be destroyed. This method saves preferences.  
    @Override
    */
    protected void onStop()
      {
      // --DEBUG!--
      Log.i(APP_TAG,"UIActivity -> onStop()");
      super.onStop();
      updateStop();
      }

    

    
    
    
       
     
 
    

    /**
     Dashboard Intent Message Received  <p>
     
     The messages we recognise at this level are UI_PING and UI_TOAST_MESSAGE. 
     
      @param action
      @param intData
      @param floatData
      @param stringData
      @param bundleData 
     */
    public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
      {
      
      if (action.equals(UI_UPDATED))  staleUICounter = 0; 
        // ...This action indicates that the UI has been refreshed. Reset the 'stale' counter.  
      
      if (action.equals(UI_PING))  dashMessages.sendData(UI_PONG, null, null, null, null);
        // ...Ping received. Answer with Pong. 
      
      if (action.equals(UI_TOAST_MESSAGE))  Toast.makeText(getApplicationContext(), stringData, Toast.LENGTH_SHORT).show();
        // ...Show pop-up message. 

      }  // [function]





    
    
    
    
    
    /**********************************************************************************************
     * Update Timer Runnable:  
     **********************************************************************************************/
    private Runnable updateTimerTask = new Runnable() 
        {
        public void run()  
          {
          // Cancel any existing timers: 
          updateStop();        
          
          if (autoReset)
            {
            // Update stale counter and check for overflow:
            if (staleUICounter <= UI_STALE_TIME) staleUICounter++;
  
            // --DEBUG!!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " UIActivity -> Tick. Counter = " + staleUICounter);
  
            if (staleUICounter == UI_STALE_TIME)   
              {
              // n seconds without UI update. Issue a UI reset:
              // Nb: UI is only reset ONCE, until another UI_UPDATED message arrives
              // (UI refresh has restarted), at which time the counter is reset.  
              dashMessages.sendData(UI_RESET, null, null, null, null);
              }
            }  // [if (autoReset)]
            
          // Restart the update timer: 
          updateStart();
          } 
        };      
      /**********************************************************************************************
       * Start and Stop update timer:  
       **********************************************************************************************/
      private void updateStart()
        {
        updateTimer.removeCallbacks(updateTimerTask);
        updateTimer.postDelayed(updateTimerTask, UPDATE_INTERVAL);    
        }
      /**********************************************************************************************/    
      private void updateStop()
        {
        updateTimer.removeCallbacks(updateTimerTask);  
        }
      /**********************************************************************************************/

      
      
    
    
    
}  // [class]

