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


//import com.tumanako.dash.DashMessages;
import com.tumanako.dash.DashMessages;
import com.tumanako.dash.IDashMessages;

import android.content.Context;
import android.location.GpsStatus;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;


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
  private boolean isDataSent       = false;   // Flag to indicate that the latest update has been sent. Reset when a new cycle is started. 
  private long    timeLastPosition = 0l;      // System time (mS) for the last position update.
  private boolean isFixGood        = false;   // Do we have a current fix? True when we are receiving good NMEA data; false if NMEA data is empty (i.e. no fix)
                                              //  NOTE: This only looks at the last NMEA we received; if NMEA data stop alltogether, isFixGood may still be true. 
                                              //  See IsFixGood() method below (also checks for time since last NMEA data).
  
  private static final int NMEA_WAIT_TIMEOUT = 3000;   // If no NMEA sentences received after this many mS, we'll declare that the NMEAData has stopped. 
  
  /****** GPS Data Message Intent Filters: *********/
  public static final String GPS_POSITION = "DATA_GPS_POSITION";  

    
  private DashMessages dashMessages;
  
  // ---------------DEMO MODE CODE -------------------------------
  private boolean isDemo = false;  // Demo mode flag!
  // ---------------DEMO MODE CODE -------------------------------  

  
  /************************************************************
   * Constructor: Sets up a message broadcaster. 
   * Nb: We don't receive any messages, so intentFilter is null. 
   ************************************************************/
  public NmeaProcessor(Context context)
    {  
    dashMessages = new DashMessages(context, (IDashMessages)this, null);
    }

  
  
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
    Bundle gpsData = new Bundle();
    gpsData.putDouble  ( "LAT",    gpsLat    );
    gpsData.putDouble  ( "LON",    gpsLon    );
    gpsData.putFloat   ( "TIME",   gpsTime   );
    gpsData.putFloat   ( "SPEED",  gpsSpeed  );
    gpsData.putFloat   ( "TRACKT", gpsTrackT );
    gpsData.putInt     ( "NSATS",  gpsSats   );
    gpsData.putBoolean ( "FIX",    isFixGood ) ;
    // Now transmit the data to the UI by sending a message!  
    dashMessages.sendData( GPS_POSITION, null, null, null, gpsData );
    isDataSent = true; 
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
      }
    // ---------------DEMO MODE CODE -------------------------------      
        

    // Finished one NMEA sentence cycle. Notify UI Class:
    if ( (isLastSentence) && (!isDataSent) ) sendGPSData();   // Send some GPS data as Intents.
    }
  
  
  
  
  
  
  
  
  
  /**
   NMEA Sentence filter and decoder <p>
   
   Filters out specific NMEA sentences, and decodes the values contained in them. 
   In particular, it uses the GPGGA sentence to get most data (position, time, etc) 
   and either GPVTG or GPRMC to get velocity and ground track. <p> 
   
   GPS data output seems to vary between devices, in terms of which NMEA sentences are 
   included. The decoder process makes several assumptions:  <p>
   
    <ul>
     <li>Sentences will be transmitted on an update cycle, assumed to be 1 second. 
         This means that a number of sentences will be transmitted close together, 
         followed by a pause
          
     <li>Each cycle contains the GPGGA sentence on every device (minimum position information).
         This sentence must occur BEFORE GPVTG or GPRMC (see below) 
     
     <li>Each cycle will include either GPVTG or GPRMC (or both), which will contain velocity 
         and track information
         
     <li>If GPVTG and GPRMC are both included, they will contain the same speed and ground track, 
         so the first version in each cycle will be used.
    </ul>
    
   The code assumes a new cycle when it receives a GPGGA. The cycle is considered "complete" 
   and the isLastSentence flag is set when the first of either GPVTG or GPRMC is received. 

   @param thisNMEA  String containing an NMEA sentence from the GPS  
   */
  private void nmeaDecode( String thisNMEA )
    {
    // Decodes NMEA sentences and sets NMEAData values.
    //
    // Example NMEA data sentence: 
    //  "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"
    
    // --DEBUG!!-- Dump NMEA Sentences: Log.i( "NMEA", thisNMEA );
        
    if (thisNMEA.length() < 6) return;   // Can't decode string - not enough data!

    // "GGA" identifies the sentence:
    String nmeaSentenceID = thisNMEA.substring(3,6);  // Get the name of the sentence. 
    
    // Split the sentence into fields using the comma: 
    String[] nmeaParts = thisNMEA.split(",");
    
    // Select and process the sentences we want to use:
    /*********************************************************************************************/
    if (nmeaSentenceID.equals("GGA"))
      {
      // GGA String - Essential Fix Data:
      //  $GPGGA,Time, Lat, N|S, Lon, E|W, Qual, Sats, HDOP, Alt,M, Geoid,M, , , Checksum
      isLastSentence = false;   // Start of new cycle.
      isDataSent     = false;   //
      gpsLastGGA = thisNMEA;
      // --DEBUG!!-- dashMessages.sendData( "GPS_GGA", null, null, gpsLastGGA, null );
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
    if (isLastSentence) return;  // If we've found the VTG or RMC data, don't bother checking 
                                 // anything until we receive another GGA.  
    /*********************************************************************************************/
    if (nmeaSentenceID.equals("VTG"))
      {
      // VTG String - Velocity Made Good:
      //  $GPVTG,TrueTrack,T, MagTrack,M, Speed_knots,N, Speed_kph,K, Checksum
      isLastSentence = true;  // A VTG means the end of the cycle (we have all the data we need...). 
      gpsLastVTG = thisNMEA;
      if (nmeaParts.length >= 8)
        {
        // Should be at least 8 fields in a GGA String.
        try
          {
          gpsSpeed  = Float.valueOf(nmeaParts[7]);
          gpsTrackT = Float.valueOf(nmeaParts[1]);
          // isFixGood = true;                                  //
          timeLastPosition = SystemClock.elapsedRealtime();  // We have VTG data!
          }
        catch (NumberFormatException e)
          {  
          // Number format exception... Indicates that we aren't receiving good data. (e.g. empty fields) 
          //isFixGood = false; // Ignore
          }
        }  // [if (nmeaParts.length >= 8)]
      }  // [if (nmeaSentenceID.equals("VTG"))]
    /*********************************************************************************************/
    if (nmeaSentenceID.equals("RMC"))
      {
      // RMC String - Recommended minimum data: 
      //  $GPRMC, Time, [A|V], Lat, N|S, Lon, E|W, Speed (Knots), Track (Deg True), Date, MagVariation, E|W, A*4A
      isLastSentence = true;  // A RMC means the end of the cycle (we have all the data we need...). 
      gpsLastVTG = thisNMEA;
      // --DEBUG!!-- dashMessages.sendData( "GPS_VTG", null, null, gpsLastVTG, null );
      if (nmeaParts.length >= 12)
        {
        // Should be at least 12 fields in a RMC String.
        try
          {
          gpsSpeed  = Float.valueOf(nmeaParts[7]) * 1.852f;  // Note: RMC speed is in knots! Convert to kph
          gpsTrackT = Float.valueOf(nmeaParts[8]);
          // isFixGood = true;                                  //
          timeLastPosition = SystemClock.elapsedRealtime();  // We have RMC data!
          }
        catch (NumberFormatException e)
          {  
          // Number format exception... Indicates that we aren't receiving good data. (e.g. empty fields) 
          //isFixGood = false; // Ignore
          }
        }  // [if (nmeaParts.length >= 8)]
      }  // [if (nmeaSentenceID.equals("VTG"))]
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



  public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
    {}



  public void suspend()
    {}



  public void resume()
    {}

  
  
  }  // Class
