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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;


/*********************************************************************************************
 * NMEA NMEAData Receiver: 
 * 
 * This class provides access to raw data from the NMEAData (position, track, speed, etc).
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 *********************************************************************************************/

public class NmeaGPS implements LocationListener, IDroidSensor 
  {

  private final Handler messageHandler;
  // Used to send messages back to UI class.
  
  private LocationManager mLocationManager;
  
  public NmeaProcessor NMEAData;                   // A reference to a NMEA processing object (used to decode NMEA Data strings from GPS)

  private boolean isAvailable = false;      // Is a NMEAData position available? 

  
  // ******* Message type constants for messages passed through messageHandler from this class: **********
  // Right now, we don't generate messages in this class, but we might at some point (e.g. Position Changed)
  //public static final int NMEA_GPS_DATA_READY   = NMEA_GPS_SENSOR_ID + 1;
  //public static final int NMEA_GPS_ERROR        = NMEA_GPS_SENSOR_ID + 10;

  
  
  // ***** Constructor: *******
  public NmeaGPS(Context thisParent, Handler thisHandler)
    {
    messageHandler = thisHandler;
    // Create a LocationManager object to get location data from NMEAData: 
    mLocationManager = (LocationManager) thisParent.getSystemService(Context.LOCATION_SERVICE);
    // Create an NMEA Processor to receive and process NMEA sentences: 
    NMEAData = new NmeaProcessor(messageHandler);
    }

  
  
  
  /***********************************************************************************
  *     Public Methods 
  ***********************************************************************************/
  
  // ******* Is the NMEAData available? *******************
  public boolean isOK()
    {  return isAvailable;  }

  
  public boolean isRunning()
    {  return isAvailable;  }

  
  
  /********** toString Method: *************************************
   * Returns a string with a data summary (useful for debugging):
   * @return String representing class data 
   ******************************************************************/
  @Override  
  public String toString()
    {  return NMEAData.toString();  }


  
  
  // Kill the NMEAData listener (saves batteries):
  public void suspend()
    {
    mLocationManager.removeNmeaListener(NMEAData);
    mLocationManager.removeUpdates(this);
    }

  
  
  // *** Start getting NMEAData updates: ***
  public void resume()
    {
    // Register the listener with the Location Manager to receive location updates:
    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    // Add a listener to receive NMEA sentences: 
    mLocationManager.addNmeaListener(NMEAData); 
    }
  
  
  
  
  
  
  

  /***********************************************************************************
  *     LocationListener Interface Methods 
  ***********************************************************************************/
  
  public void onLocationChanged(Location arg0)
    {  }

  public void onProviderDisabled(String arg0)
    {  }

  public void onProviderEnabled(String provider)
    {  }

  
  // ******* NMEAData Status Change: *******
  public void onStatusChanged(String provider, int status, Bundle extras)
    {
    // *** NMEAData Status: *****
    //  0 = OUT_OF_SERVICE
    //  1 = TEMPORARILY_UNAVAILABLE
    //  2 = AVAILABLE 
    //
    // Note - NMEAData status found to be slow in changing. Just look at 
    // ssFixGood() method of NMEAData object to see if a fix is available.
    if (status == 2)
      {  isAvailable = true;  }
    else
      {  isAvailable = false;  }
    }

 
  

  
  }  // Class
