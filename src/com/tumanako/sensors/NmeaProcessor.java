package com.tumanako.sensors;


import android.location.GpsStatus;
import android.os.Handler;
import android.os.SystemClock;


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
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************************************/

public class NmeaProcessor implements GpsStatus.NmeaListener, IDroidSensor
  {
    
  private final Handler messageHandler;
    // Used to send messages back to UI class.


  // ****** Information directly from the NMEAData: **********
  private float  gpsTime   = 0F;           // NMEAData Time (UTC), HHMMSS (e.g. 123542.0 for 12:35:42 pm)
  private double gpsLat    = 0.0;          // NMEAData Latitude (Dec. Degrees)
  private double gpsLon    = 0.0;          // NMEAData Longitude (Dec. Degrees)
  private int    gpsQual   = 0;            // NMEAData Fix Quality *
  private int    gpsSats   = 0;            // NMEAData Number of satellites
  private float  gpsAlt    = 0F;           // NMEAData Altitude (m)
  private float  gpsTrackT = 0F;           // NMEAData Ground track (Deg, True)
  private float  gpsSpeed  = 0F;           // NMEAData Grong speed (kph)
  private String gpsLastGGA = "";          // Will store the last GGA and
  private String gpsLastVTG = "";          // VTG strings received (for debugging)
  
  // ***** Information derived during operation: **********
  private boolean isLastSentence   = false;   // Set to true after the 'GSA' sentence arrives (last in a cycle), and false when any other sentence arrives. 
  private long    timeLastPosition = 0L;      // System time (mS) for the last position update.
  private boolean isFixGood        = false;   // Do we have a current fix? True when we are receiving good NMEA data; false if NMEA data is empty (i.e. no fix)
                                              //  NOTE: This only looks at the last NMEA we received; if NMEA data stop alltogether, isFixGood may still be true. 
                                              //  See IsFixGood() method below (also checks for time since last NMEA data).
  
  private static final int NMEA_WAIT_TIMEOUT = 3000;   // If no NMEA sentences received after this many mS, we'll declare that the NMEAData has stopped. 
  
  
  // ******* Message type constants for messages passed through messageHandler from this class: **********
  public static final int NMEA_PROCESSOR_DATA_UPDATED  = NMEA_PROCESSOR_ID + 1;
  public static final int NMEA_PROCESSOR_ERROR         = NMEA_PROCESSOR_ID + 10;
  
  
  // ---------------DEMO MODE CODE -------------------------------
  private boolean isDemo = false;  // Demo mode flag!
  // ---------------DEMO MODE CODE -------------------------------  

  
  //***** Constructor: *******
  public NmeaProcessor(Handler thisHandler)
    {
    // Stores a reference to the provided handler so that messages can be sent.
    messageHandler = thisHandler;
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
  @Override 
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

  public void suspend()
    {  }   // We don't need to suspend or resume anything for this sensor. (See NmeaGPS class)

  public void resume()
    {  }   //

  
  
  
  
  
  // ****** NMEA received Method: ************
  public void onNmeaReceived(long timestamp, String nmea)
    {
    // Called by the location services when an NMEA sentence is received. 
    nmeaDecode(nmea);
    
    // ---------------DEMO MODE CODE -------------------------------
    // Overrides normal operation in demo mode: 
    if (isDemo)
      {
      gpsSpeed  = (float)(Math.cos( (double)(System.currentTimeMillis() % 20000) / 3183  ) + 2) * 20;
      isFixGood = true;                                  //
      if (isLastSentence) messageHandler.sendMessage(messageHandler.obtainMessage(NMEA_PROCESSOR_DATA_UPDATED));
      return;
      }
    // ---------------DEMO MODE CODE -------------------------------      
        

    if (isLastSentence)
      {
      // Finished one NMEA sentence cycle. Notify UI Class:
      messageHandler.sendMessage(messageHandler.obtainMessage(NMEA_PROCESSOR_DATA_UPDATED));
      }
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

  
  
  }  // Class
