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
import android.location.GpsStatus;
import android.os.Bundle;
import android.os.SystemClock;
import com.tumanako.dash.IDashMessages;

/****************************************************************
 *  NMEA NMEAData Listner and Processor:
 *  -------------------------------
 *
 *  Implements the GpsStatus.NmeaListener interface, which receives NMEA
 *  sentences from the NMEAData.
 *
 *  This class also includes filters to select specific NMEA strings and
 *  extract the data fields.
 *
 *  Methods are provided to return the extracted gps data to a parent
 *  class.
 *
 *  Call isFixGood() to check whether good NMEA data are being received.
 *
 *  To Use:
 *
 *  First create a NmeaGPS object (see NmeaGPS.java). That class will
 *  create an instance of this class to receive and process NMEA data.
 *
 *  Once up and running, this class generates an intent whenever GPS data are
 *  updated, used to signal other components that they can request elements
 *  of the new data by calling getTime(), getLat(), etc as needed.
 *
 * NOTE:
 *  Currently, any other component which wants actual GPS data
 *  must obtain a reference to this instance (through NmeaGPS class) and
 *  retrieve the data with calls to getTime(), getLat(), etc.
 *  TO DO: Could send out an intent on data update, containing a
 *  Bundle of GPS data. That way no direct reference to this instance
 *  would be needed!
 *  QUESTION: Would this be wasteful if only some of the GPS values
 *  were needed?
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************************************/

public class NmeaProcessor implements GpsStatus.NmeaListener, IDroidSensor, IDashMessages
  {

  // ****** Information directly from the NMEAData: **********
  private float  gpsTime   = 0f;           // NMEAData Time (UTC), HHMMSS (e.g. 123542.0 for 12:35:42 pm)
  private double gpsLat    = 0.0;          // NMEAData Latitude (Dec. Degrees)
  private double gpsLon    = 0.0;          // NMEAData Longitude (Dec. Degrees)
  private int    gpsQual   = 0;            // NMEAData Fix Quality *
  private int    gpsSats   = 0;            // NMEAData Number of satellites
  private float  gpsAlt    = 0f;           // NMEAData Altitude (m)
  private float  gpsTrackT = 0f;           // NMEAData Ground track (Deg, True)
  private float  gpsSpeed  = 0f;           // NMEAData Grong speed (kph)
  private String gpsLastGGA = "";          // Will store the last GGA and
  private String gpsLastVTG = "";          // VTG strings received (for debugging)

  // ***** Information derived during operation: **********
  private boolean isLastSentence   = false;   // Set to true after the 'GSA' sentence arrives (last in a cycle), and false when any other sentence arrives.
  private long    timeLastPosition = 0l;      // System time (mS) for the last position update.
  private boolean isFixGood        = false;   // Do we have a current fix? True when we are receiving good NMEA data; false if NMEA data is empty (i.e. no fix)
                                              //  NOTE: This only looks at the last NMEA we received; if NMEA data stop alltogether, isFixGood may still be true.
                                              //  See IsFixGood() method below (also checks for time since last NMEA data).

  private static final int NMEA_WAIT_TIMEOUT = 3000;   // If no NMEA sentences received after this many mS, we'll declare that the NMEAData has stopped.


  // *** GPS Processing / Data Message Type Indicators: ***
  public static final String NMEA_PROCESSOR_DATA_UPDATED  = String.format("%d", IDashMessages.NMEA_PROCESSOR_ID + 0  );
  public static final String NMEA_PROCESSOR_ERROR         = String.format("%d", IDashMessages.NMEA_PROCESSOR_ID + 99 );

  public static final String DATA_GPS_HAS_LOCK            = String.format("%d", IDashMessages.NMEA_PROCESSOR_ID + 1  );
  public static final String DATA_GPS_TIME                = String.format("%d", IDashMessages.NMEA_PROCESSOR_ID + 2  );
  public static final String DATA_GPS_SPEED               = String.format("%d", IDashMessages.NMEA_PROCESSOR_ID + 3  );



  // ---------------DEMO MODE CODE -------------------------------
  private boolean isDemo = false;  // Demo mode flag!
  // ---------------DEMO MODE CODE -------------------------------


  /************************************************************
   * Constructor: Sets up a message broadcaster.
   ************************************************************/
  public NmeaProcessor(Context context)
    {  }



  // ******* Methods to return NMEAData data: ******************
  public float getTime()
    {  return gpsTime;  }

  public double getLat()
    {  return gpsLat;  }

  public double getLon()
    {  return gpsLon;  }

  public int getQual()
    {  return gpsQual;  }

  public int getSats()
    {  return gpsSats;  }

  public float getAlt()
    {  return gpsAlt;  }

  public float getTrackT()
    {  return gpsTrackT;  }

  public float getSpeed()
    {  return gpsSpeed;  }

  public boolean isFixGood()
    {
    // Do we have good NMEAData data?

    // ---------------DEMO MODE CODE -------------------------------
    // Overrides normal operation in demo mode:
    if (isDemo) return true;
    // ---------------DEMO MODE CODE -------------------------------

    //  If isFixGood and it's been less than NMEA_WAIT_TIMEOUT mS since the last good NMEA data, this is a good fix!
    if (isFixGood && (timeLastPosition > NMEA_WAIT_TIMEOUT) && (timeLastPosition + NMEA_WAIT_TIMEOUT) > (SystemClock.elapsedRealtime()) )
      {  return true;  }
    else
      {  return false;  }
    }



  public String getLastGGA()
    {  return gpsLastGGA;  }



  public String getLastVTG()
    {  return gpsLastVTG;  }




  // ---------------DEMO MODE CODE -------------------------------
  public void setDemo(boolean thisIsDemo)
    {
    // Set the 'Demo' mode flag:
    isDemo = thisIsDemo;
    }
  // ---------------DEMO MODE CODE -------------------------------




  /********** toString Method: *************************************
   * Returns a string with a data summary (useful for debugging):
   * @return String representing class data
   ******************************************************************/
  public String toString()
    {
    // Return a summary of NMEAData data as a string (for debugging).
    StringBuffer thisDump = new StringBuffer();

    thisDump.append(  String.format( " Time:      %.1f\n" +
                                     " Qual:      %d\n" +
                                     " Sats:      %d\n\n" +
                                     " Latitude:  %.6f\n" +
                                     " Longitude: %.6f\n" +
                                     " Altitude:  %.1f\n" +
                                     " Track:     %.1f\n" +
                                     " Speed:     %.1f\n\n",
                                     gpsTime, gpsQual, gpsSats, gpsLat, gpsLon, gpsAlt, gpsTrackT, gpsSpeed )  );
    if (isFixGood) thisDump.append( "Fix: GOOD\n\n");
    else           thisDump.append( "Fix: NO FIX\n\n");
    // -- DEBUG: -- thisDump.append( gpsLastGGA.replace(",",",\n") + "\n\n" + gpsLastVTG.replace(",",",\n");
    return thisDump.toString();
    }


  public boolean isOK()
    {  return isFixGood();  }  // Returns the 'Fix Good' indication

  public boolean isRunning()
    {  return true;  }        // Always 'true' since this sensor is always ready.





  private void sendGPSData()
    {
    // Send some useful GPS data as Intents:
    float fixGood = (isFixGood) ? 1f : 0f;
    Bundle gpsData = new Bundle();
    gpsData.putFloat(DATA_GPS_HAS_LOCK, fixGood);
    gpsData.putFloat(DATA_GPS_TIME,     gpsTime);
    gpsData.putFloat(DATA_GPS_SPEED,    gpsSpeed);
    }


  // ****** NMEA received Method: ************
  public void onNmeaReceived(long timestamp, String nmea)
    {
    // Called by the location services when an NMEA sentence is received.
    nmeaDecode(nmea);

    // ---------------DEMO MODE CODE -------------------------------
    // Overrides normal operation in demo mode:
    if (isDemo)
      {
      //gpsSpeed  = (float)(Math.cos( (double)(System.currentTimeMillis() % 20000) / 3183  ) + 2) * 20;
      isFixGood = true;                    //
      if (isLastSentence) sendGPSData();   // Send some GPS data as Intents.
      return;
      }
    // ---------------DEMO MODE CODE -------------------------------


    // Finished one NMEA sentence cycle. Notify UI Class:
    if (isLastSentence) sendGPSData();   // Send some GPS data as Intents.
    }



  // **** NMEA Sentence filter and decode: ********
  private void nmeaDecode( String thisNMEA )
    {
    // Decodes NMEA sentences and sets NMEAData values.
    //
    // Example NMEA data sentence:
    //  "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"

    isLastSentence = false;  // Set to true when we receive the last sentence in a sentence cycle (GSA sentence).

    if (thisNMEA.length() < 6) return;   // Can't decode string - not enough data!

    // "GGA" identifies the sentence:
    String nmeaSentenceID = thisNMEA.substring(3,6);  // Get the name of the sentence.

    // Split the sentence into fields using the comma:
    String[] nmeaParts = thisNMEA.split(",");

    // Select and process the sentences we want to use:
    /*********************************************************************************************/
    if (nmeaSentenceID.equals("GGA"))
      {
      // GGA String -
      // Essential Fix Data ($GPGGA,Time, Lat, Lon, Qual, Sats, HDOP, Alt,M, Geoid,M, , , Checksum):
      gpsLastGGA = thisNMEA;
      if (nmeaParts.length >= 12)
        {
        // Should be at least 12 fields in a GGA String.
        try
          {
          gpsTime = Float.valueOf(nmeaParts[1]);
          gpsLat  = nmeaDegreeFix(nmeaParts[2]);
          gpsLon  = nmeaDegreeFix(nmeaParts[4]);
          gpsQual = Integer.valueOf(nmeaParts[6]);
          gpsSats = Integer.valueOf(nmeaParts[7]);
          gpsAlt  = Float.valueOf(nmeaParts[9]);
          // Correct sign (South of equator and East of Grenwitch should be negative):
          if (nmeaParts[3].equals("S")) gpsLat = gpsLat * -1;   // South of equator!
          if (nmeaParts[5].equals("W")) gpsLon = gpsLon * -1;   // West of Grenwitch!
          if (gpsQual > 0)
            {  isFixGood = true;  }                          //
          else
            {  isFixGood = false;  }                         //
          timeLastPosition = SystemClock.elapsedRealtime();  //  We have a position fix!
          }
        catch (NumberFormatException e)
          {
          // Number format exception... Indicates that we aren't receiving good data. (e.g. empty fields)
          isFixGood = false;
          }
        }  // [if (nmeaParts.length >= 12)]
      }  // [if (nmeaSentenceID.equals("GGA"))]
    /*********************************************************************************************/
    if (nmeaSentenceID.equals("VTG"))
      {
      // VTG String -
      // Velocity Made Good - ($GPVTG,TrueTrack,T, MagTrack,M, Speed_knots,N, Speed_kph,K, Checksum)
      gpsLastVTG = thisNMEA;
      if (nmeaParts.length >= 8)
        {
        // Should be at least 8 fields in a GGA String.
        try
          {
          gpsSpeed  = Float.valueOf(nmeaParts[7]);
          gpsTrackT = Float.valueOf(nmeaParts[1]);
          isFixGood = true;                                  //
          timeLastPosition = SystemClock.elapsedRealtime();  // We have VTG data!
          }
        catch (NumberFormatException e)
          {
          // Number format exception... Indicates that we aren't receiving good data. (e.g. empty fields)
          isFixGood = false;
          }
        }  // [if (nmeaParts.length >= 8)]
      }  // [if (nmeaSentenceID.equals("VTG"))]
    /*********************************************************************************************/
    if (nmeaSentenceID.equals("GSA"))
      {
      // "GSA" should be the last sentence in each NMEA cycle.
      isLastSentence = true;
      }  // [if (nmeaSentenceID.equals("GSA"))]
    /*********************************************************************************************/
    }




  // ******** NMEA Degree format fix: *******
  private double nmeaDegreeFix(String thisLatLon)
    {
    // NMEA lat and long are in a funny format:
    // "DDDMM.MMMMM" or "DDMM.MMMMM"
    // Where DDD is degrees (000-180) and DD is degrees (00-90),
    // and MM.MMM is decimal minutes.
    //
    // This method unpacks the above number (represented as a string) and
    // returns the value in decimal degrees.
    // (i.e. DDD + (MM.MMM / 60)
    //
    // Returns 0 if the conversion fails.
    if (thisLatLon.length() < 7) return 0.0;   // Should have at least 'DDMM.MM'.
    try
      {
      int dotAt = thisLatLon.indexOf(".");
      Double degrees = Double.valueOf(thisLatLon.substring(0,dotAt-2));
      Double minutes = Double.valueOf(thisLatLon.substring(dotAt-2));
      return degrees + (minutes /60.0);
      }
    catch (Exception e)
      {
      return 0.0;  // On error, give up and return 0.0
      }
    }



  public void messageReceived(String action, int message, Float floatData,
      String stringData, Bundle data)
    {
    // TODO Auto-generated method stub

    }



  public void suspend()
    {
    // TODO Auto-generated method stub

    }



  public void resume()
    {
    // TODO Auto-generated method stub

    }



  }  // Class
