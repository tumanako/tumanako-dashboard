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

package com.tumanako.dash;

import android.os.Bundle;

/**
 * Classes which wish to use the DashMessages class should implement this.
 */
public interface IDashMessages
{

  /* Message ID Constants: **************************************************
   * These integer IDs are used as the 'value' of the DASHMESSAGE_MESSAGE
   * field in an intent. They identify the source of the message.
   * We define a global list here, and other classes may add their own by
   * using one of these as a base and adding an integer value.
   **************************************************************************/
  public static final int DASHMESSAGE_UNSPECIFIED  =    0;
  public static final int NMEA_GPS_SENSOR_ID       =  300;
  public static final int NMEA_PROCESSOR_ID        =  400;
  public static final int VEHICLE_DATA_ID          =  500;
  public static final int CHARGE_NODE_ID           = 1000;
  public static final int CHARGE_HTTPCON_ID        = 1100;


  /**
   * Receives filtered intents.
   */
  void messageReceived(String action, int message, Float floatData, String stringData, Bundle data);
}
