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

package com.tumanako.sensors;

/**
 * Tumanako Sensor.
 *
 * Provides a basic sensor definition for several kinds of
 * sensor. Currently:
 *
 * - {@link VehicleData} : Some connection to the vehicle (probably Bluetooth or USB)
 * - {@link NmeaGPS}     : For position, etc. Uses system location services
 *
 * The methods in this interface have to be implemented for all Tumanako sensors /
 * data sources.
 *
 * The general life-cycle of a sensor is:
 *
 * - Create
 *   This should set up the sensor but NOT start it (i.e. it should not start
 *   generating messages or using CPU / Battery time)
 * - {@link #resume()}
 *   Called when the UI or parent is ready to receive messages.
 *   Turn 'On' the sensor.
 * - {@link #suspend()}
 *   Called when the sensor should stop using CPU / Batteries and stop generating messages,
 *   e.g. when the UI activity goes into the background or is closed.
 *   After {@link #suspend()}, {@link #resume()} may be called later when the UI is re-displayed,
 *   or the OS may trash the application in which case {@link #resume()} will never be called.
 *   Thus, {@link #suspend()} should unregister any sensors or system services and leave things
 *   in a state where they do not waste power or CPU time.
 *   This is typically used when the application is paused, and should leave the sensors
 *   in a state where they do not consume batteries.
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public interface IDroidSensor
{

  /**
   * Signals whether the Sensor was successfully found, initialized and set up.
   */
  boolean isOK();

  /**
   * Signals whether the Sensor is currently producing data.
   */
  boolean isRunning();

  /**
   * Stop the sensor from taking readings.
   * This should leave the sensor in a state which does NOT use CPU time or batteries,
   * suitable for if the application is put into the background indefinitely.
   */
  void suspend();

  /**
   * Start taking readings with the sensor.
   * The sensor will start generating messages.
   */
  void resume();

  /**
   * Returns interesting sensor data as a string.
   * Generally only used for debugging.
   * XXX maybe rename to something like getDebugInfo?
   */
  @Override
  String toString();
}
