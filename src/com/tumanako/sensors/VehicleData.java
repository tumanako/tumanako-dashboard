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

import android.content.Context;
import android.os.Handler;


/****************************************************************
 *  Tumanako Vehicle Data Input:
 *  -------------------------------
 *
 *  This class is designed to connect to get a stream of data from 
 *  the vehicle electronics.
 *   
 *  The stream is decoded as necessary and data are stored in a 
 *  DashData object which a parent class can retrieve.
 *  
 *  Messages are passed to the parent class to inform it of
 *  status changes. 
 *
 *  Currently there is no actual vehicle data source, so this 
 *  class just includes some code to generate demo data!
 *  
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************************************/


public class VehicleData extends TumanakoSensor implements IDroidSensor 
  {

  private final Handler updateTimer = new Handler();       // Message handler for sensor update timer (sends data periodically)
    
  private String lastData = "";                   // Stores the last full data record (for debug, etc)
  private volatile boolean isRunning = false;     // Internal flag to indicate whether vehicle data input is in a 'running' state (i.e. data being received) or not.  
  
  //private static final int RESULT_LENGTH = 1024;                       // This is the number of useful result bytes we expect to receive from the sensor when we request data.
  //private static final int LAST_BUFFER_INDEX = RESULT_LENGTH - 1;      // Pre-calculate the maximum index allowed in the data buffer. 
  private static final int READ_EVERY = 200;                             // Read the sensor every n milliseconds
   
    
  // ** General Message Type Indicators: **
  // One of these constants will be supplied as a 'SENSOR_INTENT_FROMID' parameter whenever an 
  // intent is sent to the rest of the application. Indicates what the intent is for.
  public static final int VEHICLE_DATA_UPDATED = VEHICLE_DATA_ID + 0;
  public static final int VEHICLE_DATA_ERROR   = VEHICLE_DATA_ID + 99;

  // *** Vehicle Data Message Type Indicators: ***
  // Primary Driver Data: 
  public static final int DATA_CONTACTOR_ON        = VEHICLE_DATA_ID +  1;        // Main contactor on (i.e. Inverter On!) (indicator light)
  public static final int DATA_FAULT               = VEHICLE_DATA_ID +  2;        // Fault (Warning symbol light)
  public static final int DATA_MAIN_BATTERY_KWH    = VEHICLE_DATA_ID +  3;        // Main Battery kWh remaining (fuel gauge dial)
  public static final int DATA_ACC_BATTERY_VLT     = VEHICLE_DATA_ID +  4;        // Accessory battery DC voltage (dial)
  public static final int DATA_MOTOR_RPM           = VEHICLE_DATA_ID +  5;        // Motor Rpm (dial)
  public static final int DATA_MAIN_BATTERY_TEMP   = VEHICLE_DATA_ID +  6;        // Main Battery Temperature (Bar)
  public static final int DATA_MOTOR_TEMP          = VEHICLE_DATA_ID +  7;        // Motor Temperature (Bar)
  public static final int DATA_CONTROLLER_TEMP     = VEHICLE_DATA_ID +  8;        // Controller Temperature (Bar)
  // Technical system data:
  public static final int DATA_PRECHARGE           = VEHICLE_DATA_ID +  9;        // pre-charge indicator
  public static final int DATA_MAIN_BATTERY_VLT    = VEHICLE_DATA_ID + 10;        // Main Battery Voltage
  public static final int DATA_MAIN_BATTERY_AH     = VEHICLE_DATA_ID + 11;        // Main Battery Amp hour
  public static final int DATA_AIR_TEMP            = VEHICLE_DATA_ID + 12;        // Air Temperature
  
  public static final int DATA_DATA_OK             = VEHICLE_DATA_ID + 13;        // Is the connection OK?

  
  // ---------------DEMO MODE CODE -------------------------------
  private boolean isDemo = false;  // Demo mode flag!
  private float kWh = 0f; 
  // ---------------DEMO MODE CODE -------------------------------  
  
  
  
  
  // ************** Constructor: *****************************************
  public VehicleData(Context context)
    {
    super(context);    // We are extenging the 'TumanakoSensor' class, and we need to call its Constructor here. 
    //messageBroadcaster = LocalBroadcastManager.getInstance(context);  // Get a Broadcast Manager so we can send out messages to other parts of the app.
    }

  
  
  
  
  
  /********** Public Methods: *****************************************************************************/
  @Override
  public boolean isOK()
    {  return isRunning;  }  // Basically, if we're not running, We're not OK (i.e. not yet connected). 

  @Override
  public boolean isRunning()
    {  return isRunning;  }


  /********** toString Method: *************************************
   * Returns a string with a data summary (useful for debugging):
   * @return String representing class data 
   ******************************************************************/
  @Override  
  public String toString()
    {  return lastData;  }


  
  // ---------------DEMO MODE CODE -------------------------------
  public void setDemo(boolean thisIsDemo)
    {
    updateTimer.removeCallbacks(updateTimerTask);   // Stops the sensor read timer.
    // Set the 'Demo' mode flag: 
    isDemo = thisIsDemo;
    updateTimer.postDelayed(updateTimerTask, READ_EVERY);  // ...Callback in n milliseconds!    
    }
  // ---------------DEMO MODE CODE -------------------------------
  
  @Override
  public void suspend()
    {
    // Stop the update timer if it's running:
    updateTimer.removeCallbacks(updateTimerTask);   // Stops the sensor read timer.

    // ---------------DEMO MODE CODE -------------------------------
    isDemo = false;
    // ---------------DEMO MODE CODE -------------------------------    
    
    }

  
  @Override
  public void resume()
    {
    // Remove any existing timer callbacks: 
    updateTimer.removeCallbacks(updateTimerTask);             // Clear Update Timer.    
    // Start an update timer: 
    updateTimer.postDelayed(updateTimerTask, READ_EVERY);    // ...Callback in n milliseconds!
    }

  
  
  
  
  
  
  
  /************* Data Decode: ************************************
   * This method decodes a string of data received from the input stream 
   * and fills in the various data fields in vehicleData.
   * @param thisData - A string containing encoded vehicle data. 
   ***************************************************************
  private void decodeVehicleData(String thisData)
    {
    // TO-DO: Figure out data format and write code to decode it!
    }
  ***/
  
  

  
  /*********** Data Update Timer: ********************************************************************************/
  private Runnable updateTimerTask = new Runnable() 
   {
   // Creates a Runnable which will be called after a delay, to carry out a read of vehicle data. 
   public void run()  
     {
     updateTimer.removeCallbacks(updateTimerTask);                // ...Make sure there is no active callback already....
     
     /*************************************
      *  Here's how the code might look...
      *
       vehicleBTConnection.sendCommand(READ_DATA_COMMAND);
       boolean dataAvaliable = vehicleBTConnection.isDataAvailable();
       if (dataAvaliable) 
         {
         decodeVehicleData( vehicleBTConnection.getLastData() );
         ...etc
         }
      *
      ************************************/
     
     // ---------------DEMO MODE CODE -------------------------------
     // Overrides normal BT operation in demo mode: 
     if (isDemo)
       {
       isRunning = true;  // Pretend we are running.
       /***** Generate some fake data  and send it to the UI: ***************/
       kWh = kWh + 1f;  if (kWh > 30f) kWh = 10f;
       float thisRPM = ((android.util.FloatMath.sin((float)(System.currentTimeMillis() % 12000) / 1909f  ) + 0.3f) * 3000f);
       float demoFault = (thisRPM < -1500)                                ? 1f : 0f;
       if (thisRPM < 0) thisRPM = 0;
       float contactorOn = (thisRPM > 1)                                  ? 1f : 0f;
       float demoGreenGlobe = ((System.currentTimeMillis() % 300) > 100)  ? 1f : 0f;
       sendFloat(DATA_CONTACTOR_ON,      contactorOn       );
       sendFloat(DATA_FAULT,             demoFault         );
       sendFloat(DATA_MAIN_BATTERY_KWH,  kWh               );
       sendFloat(DATA_ACC_BATTERY_VLT,   12.6f             );
       sendFloat(DATA_MOTOR_RPM,         thisRPM           );
       sendFloat(DATA_MAIN_BATTERY_TEMP, kWh               );
       sendFloat(DATA_MOTOR_TEMP,        (thisRPM/4000)+20 );
       sendFloat(DATA_CONTROLLER_TEMP,   (thisRPM/4000)+15 );
       sendFloat(DATA_PRECHARGE,         demoGreenGlobe    );
       sendFloat(DATA_MAIN_BATTERY_VLT,  133.5f            );
       sendFloat(DATA_MAIN_BATTERY_AH,   189.4f            );
       sendFloat(DATA_AIR_TEMP,          19.6f             );
       sendFloat(DATA_DATA_OK,           1f                );
       /**********************************************************************/
       }
     // ---------------DEMO MODE CODE -------------------------------
     
     updateTimer.postDelayed(updateTimerTask, READ_EVERY);        // ...Callback in n milliseconds!
     } 
   };
   /***************************************************************************************************************/

   
    

   
  
  }  // [class VehicleData]
