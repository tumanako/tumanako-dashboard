package com.tumanako.sensors;

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

import com.tumanako.dash.DashMessages;
import com.tumanako.dash.IDashMessages;
import com.tumanako.ui.UIActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


/****************************************************************
 *  Tumanako Demo Data Input:
 *  -------------------------------
 *
 *  This class is designed to generate demo data to show the UI
 *  (if a connection to a Tumanako vehicle controller is not 
 *  a vailable!)
 *   
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************************************/


public class DemoData implements IDashMessages  
  {

  private final int UPDATE_INTERVAL = 50;                  // Data send interval (ms)
  private Handler updateTimer = new Handler(); 
  
  private boolean isRunning = false;  // Demo mode is running flag!
  private float kWh = 0f; 
  
  private float avgEnergyPerHour = 0f;   // Calculated Values relating to estimated range. 
  private float avgEnergyPerKm = 0f;     //  
  
  private DashMessages dashMessages;
  
  

  
  // ************** Constructor: *****************************************
  public DemoData(Context context)
    {
    dashMessages = new DashMessages(context, this, null);    // We are extending the 'DashMessages' class, and we need to call its Constructor here. 
    }

  
  
  
 /********** Other classes call this to start or stop the demo: *******************/ 
 public void setDemo(boolean thisIsDemo)
   {
   // Set the 'Demo' mode flag:
   if (thisIsDemo) startDemo();
   else            stopDemo();
   }


 
 
 

 /********** Dash Message Received: *************************************
  * Called when we receive an intent message via our Dashmessage object.
  * Don't actually do anything!    
  ***********************************************************************/
 public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData )
   {   }
  
 
 
 
 
 
 private void startDemo()
   {
   isRunning = true;
   avgEnergyPerHour = 10f;   
   avgEnergyPerKm = 0.1f;   
   updateTimer.removeCallbacks(updateTimerTask);                 // Stop existing timer.
   updateTimer.postDelayed(updateTimerTask, UPDATE_INTERVAL);    // Restart timer.   
   }
 
 
 private void stopDemo()
   {
   isRunning = false;
   updateTimer.removeCallbacks(updateTimerTask);   // Stop existing timer. 
   }
 
 
 
 

 
  /**
    Convert a 'time' in decimal format to a string
    
    E.g. 8.5 is converted to "8:30".
    
     @param time in decimal format
     @return String containing formated version of time
   ********************************************************************/
  private String getTime( float time )
    {
    return String.format("%1d:%02d", (int)(time), (int)((time - (float)((int)(time))) * 60) );
    }
  
 
 
  
  
  /********************* Update Timer: **********************************************
   * Runs on a timer, and sends fake data to the UI as long as demo mode is active.  
   **********************************************************************************/
  private Runnable updateTimerTask = new Runnable() 
   {
   public void run()  
     {

// -- DEBUG!! -- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " DemoData -> Tick (=data update)." );
     
     updateTimer.removeCallbacks(updateTimerTask);                    // ...Make sure there is no active callback already....

     /***** Generate some fake data  and send it to the UI: ***************/
     kWh = kWh - 0.11f;  if (kWh < 0f) kWh = 30f;
     float thisRPM = (float) ((java.lang.Math.sin((float)(System.currentTimeMillis() % 12000) / 1909f  ) + 0.3f) * 3000f);
     float demoFault   = (thisRPM < -1500)                              ? 1f : 0f;
     float demoReverse = (thisRPM < 0)                                  ? 1f : 0f;  
     float contactorOn = (thisRPM > 1)                                  ? 1f : 0f;
     float preCharge = ((System.currentTimeMillis() % 300) > 100)  ? 1f : 0f;
     float driveTime  = (avgEnergyPerHour > 0f)  ?  (kWh / avgEnergyPerHour) : 99.99f; 
     float driveRange = (avgEnergyPerKm   > 0f)  ?  (kWh / avgEnergyPerKm)   : 9999f;

     // Status Lamps: 
     dashMessages.sendData( "DATA_DATA_OK",           null, 1f,                              null, null );
     dashMessages.sendData( "DATA_CONTACTOR_ON",      null, contactorOn,                     null, null );
     dashMessages.sendData( "DATA_FAULT",             null, demoFault,                       null, null );
     dashMessages.sendData( "DATA_MOTOR_REVERSE",     null, demoReverse,                     null, null );
     dashMessages.sendData( "DATA_PRECHARGE",         null, preCharge,                       null, null );
     // Gauges: 
     dashMessages.sendData( "DATA_MOTOR_RPM",         null, Math.abs(thisRPM)/1000,          null, null );
     dashMessages.sendData( "DATA_MAIN_BATTERY_KWH",  null, kWh,                             null, null );
     // Bar Plots:
     dashMessages.sendData( "DATA_MAIN_BATTERY_TEMP", null, 60-(thisRPM/100),                null, null );
     dashMessages.sendData( "DATA_MOTOR_TEMP",        null, (thisRPM/56)+25,                 null, null );
     dashMessages.sendData( "DATA_CONTROLLER_TEMP",   null, (thisRPM/100)+35,                null, null );
     // Additional data (displayed in text boxes):    
     dashMessages.sendData( "DATA_DRIVE_RANGE",       null, driveRange, "%.0f",                    null );
     dashMessages.sendData( "DATA_ACC_BATTERY_VLT",   null, 12.67f,     "%.2f",                    null );
     dashMessages.sendData( "DATA_DRIVE_TIME",        null, null,       getTime(driveTime),        null );
     dashMessages.sendData( "DATA_MAIN_BATTERY_VLT",  null, 133.5f,                          null, null );
     dashMessages.sendData( "DATA_MAIN_BATTERY_AH",   null, 189.4f,                          null, null );
     dashMessages.sendData( "DATA_AIR_TEMP",          null, 19.6f,                           null, null );
    
     /**********************************************************************/

     // Send "UI Updated" message: 
     dashMessages.sendData( UIActivity.UI_UPDATED, null, null, null, null );

     if (isRunning) updateTimer.postDelayed(updateTimerTask, UPDATE_INTERVAL);    // Restart timer.
     } 
   };

  
   
   
  }  // [class DemoData]
