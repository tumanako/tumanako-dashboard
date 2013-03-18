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


/**************************************************************
 * 
 * Tumanako Sensor Class Interface: 
 * 
 * Provides a basic sensor definition for several kinds of 
 * sensor. Currently: 
 *
 *  Vehicle Data - Some connection to the vehicle (probably Bluetooth or USB) 
 *  NMEA GPS     - For position, etc. Uses system location services
 *
 * The following methods must be implemented for all tumanako sensors / 
 * data sources: 
 * 
 * isOK()      - Returns a boolean indicating whether the sensor is available 
 * isRunning() - Returns a boolean indicating whether the sensor is generating data
 * 
 * resume()    - Start taking readings. The sensor will start generating messages.
 *  
 * suspend()   - Stop the sensor from taking readings. This should leave the sensor
 *               in a state which does NOT use CPU time or batteries - suitable 
 *               for if the application is put into the background indefinitely. 
 *               
 * toString()  - Return useful sensor info as a string. Used for debugging. 
 * 
 * 
 * The general lifecycle of a sensor is: 
 * 
 * Create - The constructor is called when a new instance is created. This should 
 *          set up the sensor but NOT start it (i.e. it shouldn't start 
 *          generating messages or using CPU / Battery time)
 *          
 * resume() - Called when the UI or parent is ready to receive messages. turn 'On' 
 *            the sensor.
 *            
 * suspend() - Called when the sensor should stop using CPU / Batteries and stop 
 *             generating messages - e.g. when the UI activity goes into the background
 *             or is closed.
 *             
 *             After suspend(), resume() may be called later when the UI is re-displayed, 
 *             or the OS may trash the app in which case resume() will never be called. 
 *             Thus, suspend() should unregister any sensors or system services and
 *             leave things in a state where they don't waste power or CPU time.  
 *    
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 **************************************************************/


public interface IDroidSensor
  {
  
  /****** Generic sensor methods, common to all sensors: *******************************************************************/
  public boolean isOK();           // Signals whether the Sensor was successfully found, initialised and set up.
  public boolean isRunning();      // Signals whether the Sensor is currently producing data.
 
  public void suspend();           // Stop the sensor from taking readings*.  
  public void resume();            // Start taking readings with the sensor. 
      //   *Suspend: This is typically used when the application is paused, and should leave the sensors in a state 
      //   where they don't consume batteries.
  
  public String toString();        // Returns interesting sensor data as a string. Generally only used for debugging.
    
  }
