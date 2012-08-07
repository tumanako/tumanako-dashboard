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


/****************************************
 * 
 * Generic Andriod Sensor Class Interface: 
 * 
 * Provides a basic sensor definition for several kinds of 
 * sensor. 
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************/


public interface IDroidSensor
  {

  /**** Sensor ID Constants: ***************************
   * These provide a unique ID range for each class of sensor
   * (Used to identify messages received by the UI class).  
   *****************************************************/
  
  public static final int ACCEL_SENSOR_ID          = 100;
  public static final int MAGNETIC_SENSOR_ID       = 200;
  public static final int NMEA_GPS_SENSOR_ID       = 300;
  public static final int NMEA_PROCESSOR_ID        = 400;
  public static final int VEHICLE_DATA_ID          = 500;
  
  
  // *** Generic sensor methods, common to all sensors: ***

  public boolean isOK();           // Signals whether the Sensor was successfully found, initialised and set up.
  public boolean isRunning();      // Signals whether the Sensor is currently producing data.
 
  public String toString();        // Returns interesting sensor data as a string. Generally only used for debugging.
  
  public void suspend();           // Stop the sensor from taking readings*.  
  public void resume();            // Start taking readings with the sensor. 
  
  //   *Suspend: This is typically used when the application is paused, and should leave the sensors in a state where they don't consume batteries.
    
  }
