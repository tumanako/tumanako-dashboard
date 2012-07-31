package com.tumanako.sensors;

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
