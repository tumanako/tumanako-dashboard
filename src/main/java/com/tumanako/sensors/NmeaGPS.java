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
import com.tumanako.dash.IDashMessages;

/**
 * NMEA NMEAData Receiver:
 *
 * This class provides access to location services so we can use the GPS.
 *
 * Note that this class uses the NmeaProcessor class to actually listen for GPS messages and
 * do the processing work. See NmeaProcessor.java
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class NmeaGPS implements LocationListener, IDroidSensor, IDashMessages
  {

  private final LocationManager mLocationManager;
  /** Is a NMEAData position available? */
  private boolean isAvailable = false;

  /** A reference to a NMEA processing object (used to decode NMEA Data strings from GPS) */
  public NmeaProcessor NMEAData;


  public NmeaGPS(Context context)
  {
    // Create a LocationManager object to get location data from NMEAData
    mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    // Create an NMEA Processor to receive and process NMEA sentences
    NMEAData = new NmeaProcessor(context);
  }

  /**
   * Is the NMEAData OK / available?
   * Note - NMEAData status found to be slow in changing. It's better to look at
   * isFixGood() method of NMEAData object to see if a fix is available.
   */
  @Override
  public boolean isOK()
  {
    return isAvailable;
  }

  @Override
  public boolean isRunning()
  {
    return isAvailable;
  }

  /**
   * Returns a string with a data summary (useful for debugging).
   * @return String representing class data
   */
  @Override
  public String toString()
  {
    return NMEAData.toString();
  }

  /**
   * Start getting NMEAData updates
   */
  public void resume()
  {
    // Register the listener with the Location Manager to receive location updates:
    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    // Add a listener to receive NMEA sentences:
    // Tihs causes our NmeaProcessor class to get NMEA messages from the GPS:
    mLocationManager.addNmeaListener(NMEAData);
  }

  /** Stop the NMEAData listener (saves batteries) */
  public void suspend()
  {
    mLocationManager.removeNmeaListener(NMEAData);
    mLocationManager.removeUpdates(this);
  }

  @Override
  public void onLocationChanged(Location arg0)
  {
  }

  @Override
  public void onProviderDisabled(String arg0)
  {
  }

  @Override
  public void onProviderEnabled(String provider)
  {
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras)
  {
    // *** NMEAData Status: *****
    //  0 = OUT_OF_SERVICE
    //  1 = TEMPORARILY_UNAVAILABLE
    //  2 = AVAILABLE
    //
    // Note - NMEAData status found to be slow in changing. Just look at
    // isFixGood() method of NMEAData object to see if a fix is available.
    isAvailable = (status == 2);
  }

  @Override
  public void messageReceived(String action, int message, Float floatData, String stringData, Bundle data)
  {
  }
}