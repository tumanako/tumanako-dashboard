package com.tumanako.sensors;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

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




/*******************************************************
 * General Tumanako Sensor Class: 
 * ------------------------------
 * 
 * Contains basic sensor functions (mostly sending messages).
 * This base class is extended by various other sensors. 
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ******************************************************/


public class TumanakoSensor implements IDroidSensor
  {

  private LocalBroadcastManager messageBroadcaster;    
    // Will be used to send intents to other parts of the app (E.g. UI)

  
  
  // ************** Constructor: *****************************************
  public TumanakoSensor(Context context)
    {
    messageBroadcaster = LocalBroadcastManager.getInstance(context);  
      // Get a Broadcast Manager so we can send out messages to other parts of the app.
    }


  
  
  
  
  
  /*****************************************************************************
   * Send Intent Methods: 
   * USed to send intents containg data values so that other parts of the app
   * (e.g. UI) can do something with the data (we don't care what!)
   * 
   * The Intent has the action string SENSOR_INTENT_ACTION
   * and has an extended data field called SENSOR_INTENT_FROMID
   * which contains the sensor message ID for data updates by this 
   * sensor.
   * 
   * There is another extended data field called SENSOR_INTENT_VALUE
   * which contains a float or string (depending on the type
   * of data to be sent - see below). 
   *  
   * The type of data to be sent is speficied by an extra field
   * with name SENSOR_INTENT_DATATYPE and one of the Intent Data Type
   * constants.   
   *  
   * See constants defined in IDroidSensor and derived classes. 
   * 
   *****************************************************************************/
    

  
  /*******************************************************
   * Sent Intent with numeric data (float).
   * Also used to send booleans at the moment
   *   1f = TRUE; 0f = FALSE; 
   *   Other values undefined.   
   * @param elementID - Identifies the sensor we are sending, 
   *                    e.g. DATA_CONTACTOR_ON
   * @param value     - Data value to send
   *******************************************************/
  void sendFloat(int elementID, float value)
    {
    Intent intent = new Intent(SENSOR_INTENT_ACTION);
    intent.putExtra( SENSOR_INTENT_FROMID,   elementID         );
    intent.putExtra( SENSOR_INTENT_DATATYPE, SENSOR_FLOAT_DATA );
    intent.putExtra( SENSOR_INTENT_VALUE,    value             );
    messageBroadcaster.sendBroadcast(intent);    
    }
  
  
  /*******************************************************
   * Sent Intent with String data: 
   * @param elementID - Identifies the sensor/data we are sending, 
   *                    e.g. VEHICLE_DATA_ERROR (see consts above)
   * @param value     - Data value to send
   *******************************************************/
  void sendString(int elementID, String value)
    {
    Intent intent = new Intent(SENSOR_INTENT_ACTION);
    intent.putExtra( SENSOR_INTENT_FROMID,   elementID          );
    intent.putExtra( SENSOR_INTENT_DATATYPE, SENSOR_STRING_DATA );    
    intent.putExtra( SENSOR_INTENT_VALUE,    value              );
    messageBroadcaster.sendBroadcast(intent);
    }


  
  
  
 
  
  
  
  /********* Generic IDroidSensor Methods: *************************
   * Should be overridden in sub classes to produce meaningful results!
   ****************************************************************/
  
  public boolean isOK()
    {
    // Auto-generated method stub
    return false;
    }

  
  public boolean isRunning()
    {
    // Auto-generated method stub
    return false;
    }

  
  public void suspend()
    {
    // Auto-generated method stub
    }

  
  public void resume()
    {
    // Auto-generated method stub
    }

  }
