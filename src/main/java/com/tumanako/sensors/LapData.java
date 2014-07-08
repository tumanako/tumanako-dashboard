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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import com.tumanako.dash.DashMessages;
import com.tumanako.dash.IDashMessages;
import com.tumanako.ui.UIActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


/**
 Lap Data Input Class <p>

 This class uses the NMEA gps sensor and other data sources to 
 keep track of lap data when operating in "track" mode.  
   
 @author Jeremy Cole-Baker / Riverhead Technology

***************************************************************/
public class LapData implements IDashMessages
  {

  public static final String START_FINISH_SET = "LAPDATA_START_FINISH_SET";

  public static final float START_FINISH_BOX_RADIUS = 8f;            // metres
  private static final int   START_FINISH_DEAD_TIME = 10;             // GPS messages, =seconds
  
  private float startFinishLat = 0.0f;
  private float startFinishLon = 0.0f;

  private boolean startFinishSet = false;
  
  private float startBoxRadius = START_FINISH_BOX_RADIUS; 
  
  private double  positionLat = 0.0;
  private double  positionLon = 0.0;
  private float   speed = 0.0f;          // Speed (kph) 
  private float   track = 0.0f;          // Ground track (Deg. True)
  private boolean positionGood = false;
  
  private float range = 0.0f;
  private float bearing = 0.0f;
  private float lastRange = 0.0f;
     
  private boolean flagLapStarted = false;
  private boolean flagFinishPending = false;
  
  private int startBoxTimer = 0;
 
  private int   [] lapNumber = { 0,  0,  0,  0,  0  };
  private float [] lapTime   = { 0f, 0f, 0f, 0f, 0f };
  private float [] lapKWH    = { 0f, 0f, 0f, 0f, 0f };

  private float kwhRemaining = 20f;   // DEMO: This value should actually be read from BMS?   
  private int lapsRemaining = 0;
  private float lapAvgKWH = 0f;
  
  private DashMessages dashMessages;
  private final Context lapDataContext;

  
  
  
  // ************** Constructor: *****************************************
  public LapData(Context context)
    {
    
    lapDataContext = context; 
    
    String [] intentFilters = 
        { 
        START_FINISH_SET,
        "CLICK_RESETLAPDATA",
        NmeaProcessor.GPS_POSITION
        };

    setDefaults();
    
    dashMessages = new DashMessages(context, this, intentFilters);    // We are extending the 'DashMessages' class, and we need to call its Constructor here. 

    readStartPos();
    readLaps();
    
    }  // Constructor

  
   
  
  
  

  private void readStartPos()
    {
    // Read stored preferences, and extract the start / finish location:
    SharedPreferences settings = lapDataContext.getSharedPreferences(UIActivity.PREFS_NAME, 0);
    startFinishLat = settings.getFloat("startFinishLat", 0.0f);
    startFinishLon = settings.getFloat("startFinishLon", 0.0f);
    startFinishSet = settings.getBoolean("startFinishSet", false);
    startBoxRadius = settings.getFloat("startBoxRadius", START_FINISH_BOX_RADIUS);
    }

  
  private void readLaps()
    {
    // Read stored preferences, and extract the data for the previous 5 laps:
    SharedPreferences settings = lapDataContext.getSharedPreferences(UIActivity.PREFS_NAME, 0);
    int i;
    for (i=0; i<5; i++)
      {
      lapNumber[i] = settings.getInt   ( String.format("lapNumber%d", i),   0  );
      lapTime[i]   = settings.getFloat ( String.format("lapTime%d", i),   0.0f );
      lapKWH[i]    = settings.getFloat ( String.format("lapKWH%d", i),    0.0f );
      }
    }
 
  
  private void saveLaps()
    {
    // Save lap data to shared preferences:
    SharedPreferences settings = lapDataContext.getSharedPreferences(UIActivity.PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    int i;
    for (i=0; i<5; i++)
      {
      editor.putInt   ( String.format("lapNumber%d", i), lapNumber[i] );
      editor.putFloat ( String.format("lapTime%d", i),   lapTime[i]   );
      editor.putFloat ( String.format("lapKWH%d", i),    lapKWH[i]    );
      }
    editor.commit();        // Commit the edits!
    }
  
  
  
  
  
  
  
  /**
   Calculate Range and Bearing from current position to start / finish <p>
   
   Relative coordinates are first calculated from the start/finish position,
   giving metres east and metres north. This calculation is done using a 
   simplified method which assumes a spherical earth where one minute of 
   latitude is one naultical mile (1852 m). Latitudes are in decimal degrees,
   and one degree is 60 minutes.  <p>
   
   Note that moving north or south by one minute of latitude is always
   a constant distance (irrespective of absolute latitude), but moving
   east or west by one minute of longitude means moving a varying distance 
   depending on latitude, because longitudinal meridians converge at the 
   poles. Hence:   
   
      [Metres North] = [Latitude Differnce] * 60 * 1852
   
      [Metres East]  = [Longitude Differnce] * Cos( [Latitude] ) * 60 * 1852

   Once relative coordinates are known, pythagoras is used to calculate range, 
   and trigonometry is used to calculate bearing (FROM location TO start/finish). 
   
   Note that we need to break the compass rose into quadrants, as the tan 
   function is only meaningful in the range +/- 90 degrees. 
   
   */
  private void rangeAndBearing()
    {
    range = 0;
    bearing = 0;
    positionGood = false;

    if (!startFinishSet) return;   // Can't calculate range / bearing: Start / Finish not set.
    
    // Calculate relative coordinates (assumes spherical earth):
    double metresNorth = (startFinishLat - positionLat) * 111120;
    double metresEast  = (startFinishLon - positionLon) * Math.cos( Math.toRadians(startFinishLat) ) * 111120;

    //dashMessages.sendData("DATA_METRES_EAST",  null, (float)metresEast,  null, null); 
    //dashMessages.sendData("DATA_METRES_NORTH", null, (float)metresNorth, null, null);
    
    // Check that we are within 1000 km of start/finish. If not, we probably have a bad 
    // GPS location, or the start/finish hasn't been set!
    if ( (metresNorth > 1000000) || (metresEast > 1000000) ) return; 
    
    positionGood = true;
    
    // Check for same location: If m east and m north are 0, range and bearing are 0 (by convention): 
    if ( (metresEast == 0) && (metresNorth == 0) ) return;

    // Calculate range using pythagoras theorem:
    range = (float) Math.sqrt(  Math.pow(metresEast,2) + Math.pow(metresNorth,2) );  // range in Metres

    // Special case: Bearing is due east or due west:
    if (metresNorth == 0)
      {
      if (metresEast > 0) bearing = (float) (Math.PI / 2.0);
      else                bearing = (float) (Math.PI * 1.5);
      return;
      }
    
    // Use trig to calculate bearing.  
    if (metresNorth > 0) 
      {
      // Start / finish is north of current location. 
      // If its in the NW quadrant, bearing will be -ve. 
      // In that case, add 2Pi radians to shift it to 270-360 degree range:  
      bearing = (float) Math.atan(metresEast / metresNorth);
      if (bearing < 0f) bearing = (float)(Math.PI * 2.0) + bearing;
      }
    else
      {
      // Start / finish is south of current location. Add Pi radians (180 degrees)
      // to shift from -90 - +90 range, to +90 - +270 range:
      bearing = (float) Math.atan(metresEast / metresNorth) + (float) Math.PI;
      }
    }

  
  
  /**
   * Ckeck whether current ground track is "towards" a destination track. 
   * This function checks whether the 'track' given by track (degrees) is 
   * within 20 degrees of the bearing given by 'destination'. 
   *  
   * @param track         Current track (typically in degrees true)
   * @param destination   Bearing to destination point (typically in degrees true)
   * @return              True - On track (i.e. heading towards destination)
   *                      False - Not heading towards destination 
   */
  private boolean trackTowards(float track, float destination)
    {
    float diff = Math.abs(destination-track);   // Angle difference (trivial case). 

    // Special case: Destination and Track near to due north. Must deal with 'wrap' 
    // (e.g. destination = 355, track = 5; Difference is actually 10): 
    if ( (destination > 345) && (track < 15)  ) diff = track - (destination - 360);
    if ( (track > 345) && (destination < 15)  ) diff = destination - (track - 360);

    if (diff < 15) return true;
    else           return false; 
    }
  
  
  
  
  
  
  /**
    End of Lap: Update lap statistics 
   */
  private void lapEnd()
    {
    dashMessages.sendData( UIActivity.UI_TOAST_MESSAGE, null, null, "Start / Finish!", null);
    // Update "previous laps" list: Inefficient array shuffle method:
    // We also calculate the average energy use over the last 5 laps as we go. 
    lapAvgKWH = 0f;
    int i;
    int avgItemCount = 0;
    if (flagLapStarted)
      {
      for (i=4; i>0; i--)
        {
        // Add this lap to the KWHr average:
        if ( (lapNumber[i] > 0) && (lapKWH[i] > 0) )
          {
          lapAvgKWH += lapKWH[i];
          avgItemCount++;
          }
        lapNumber[i] = lapNumber[i-1];
        lapTime[i] = lapTime[i-1];
        lapKWH[i] = lapKWH[i-1];
        }
      
      lapAvgKWH += lapKWH[0];
      lapAvgKWH = (lapAvgKWH / (avgItemCount+1) );

      }  // [if (flagLapStarted)]
    
    
    // Reset current lap data:
    lapNumber[0]++;
    lapTime[0] = 0.0f;
    lapKWH[0] = 0.0f;

    // Start the "start box timer" so we don't continually restart.
    startBoxTimer = START_FINISH_DEAD_TIME;
    saveLaps();
    redrawLapData();
    flagFinishPending = false;  // Start / Finish now processed (no longer pending)
    flagLapStarted = true;      // New lap started!
    }
  
  
  
  
  
  
  
  
  /**
    Redraw "Previous Lap" data. This should be called after each lap, and also 
    if the UI is recreated (e.g. on resume). 
   */
  private void redrawLapData()
    {
    // Update the previous lap details: 
    int i;
    for (i=1; i<5; i++)
      {
      dashMessages.sendData( String.format("DATA_LAP_NUMBER_%d", i), lapNumber[i], null, null, null );
      dashMessages.sendData( String.format("DATA_LAP_TIME_%d", i), null, null, String.format("%02.0f:%04.1f", (float)Math.floor(lapTime[i] / 60f), (lapTime[i] % 60f) ), null );
      dashMessages.sendData( String.format("DATA_LAP_KWH_%d", i), null, lapKWH[i], "%4.2f", null );
      }
    }
  

  
  
  
  
  /**
   Reset lap data to default values
   */
  private void setDefaults()
    {
    flagLapStarted = false;
    flagFinishPending = false;
    startBoxTimer = 0;
   
    int i;
    for (i=0; i<5; i++)
      {
      lapNumber[i] = 0;
      lapTime[i] = 0f;
      lapKWH[i] = 0f;
      }

    kwhRemaining = 20f;   // DEMO: This value should actually be read from BMS?   
    lapsRemaining = 0;
    lapAvgKWH = 0f;  
    }
  
  
  
  
  
  
  
  
  public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
    {
     if (action.equals(START_FINISH_SET)) 
        {
        // Start / Finish location has changed. Reload new location:
        readStartPos();
        }
     
     
     if (action.equals("CLICK_RESETLAPDATA"))  setDefaults();
       // ...RESET the lap data 
       
     
     if (action.equals(NmeaProcessor.GPS_POSITION))
       {
       // GPS position update:
       positionGood   = bundleData.getBoolean ( "FIX",   false );
       positionLat    = bundleData.getDouble  ( "LAT",    0.0  );
       positionLon    = bundleData.getDouble  ( "LON",    0.0  );
       speed          = bundleData.getFloat   ( "SPEED",  0.0f );
       track          = bundleData.getFloat   ( "TRACKT", 0.0f );

       if (!positionGood) return;  // GPS doesn't have a lock. Give up.  
       
       // Calculate range and bearing to start / finish (but only if the GPS has a fix): 
       rangeAndBearing();
       if (!positionGood) return;
         // Range and bearing calculation indicated that we don't have a good position, 
         // or the start/finish isn't correctly set.  
       
       dashMessages.sendData("DATA_START_RANGE",   null, range,   "%4.2f", null); 
       //dashMessages.sendData("DATA_START_BEARING", null, (float)Math.toDegrees(bearing), "%.0f",  null);
       
       if (startBoxTimer > 0)
         {
         // Start box timer is running. Ignore start/finish checking during this time.  
         // This provides "anti-jitter" to prevent false start/finish in the event of 
         // jumpy location data 
         startBoxTimer--;  
         }
       else
         {
         float deltaRange = range - lastRange; 

         // CASE 1: 
         // In the start box, but already past the start/finish (getting further away)!
         if ( (flagFinishPending) || 
              (deltaRange >= 0) && (range <= startBoxRadius) )
           {

           lapEnd();
           }
         else
           {
           // CASE 2: 
           // The range to the start/finish is smaller than the distance we are covering 
           // each period, AND we are heading towards the start/finish. 
           // Predict a start/finish in the next period. 
           if (  ( (-1 * deltaRange) > range) &&
                 ( trackTowards(track, bearing) )  )
             {
             flagFinishPending = true;
             }
           }
           
         /* ------ Simple Method: ---------------------------------------------------------------- */
         //if (range <= START_FINISH_BOX_RADIUS)  lapEnd();    // We're currently in the "start box".
         /* -------------------------------------------------------------------------------------- */
         
         }
                
       lastRange = range;
       
       if (flagLapStarted) 
         {
         lapTime[0]++;

         //------- FAKE: Use formula based on speed to simulate energy usage for demo: -------
         float thisLapKWH = ( (float)Math.pow(speed, 2) / 200);
         lapKWH[0] += thisLapKWH;
         kwhRemaining -= thisLapKWH;
         if (kwhRemaining < 0f) kwhRemaining = 0f;
         if (lapAvgKWH > 0) lapsRemaining = (int) (kwhRemaining / lapAvgKWH);
         else               lapsRemaining = 0;
         //-----------------------------------------------------------------------------------

         }
       
       // Update UI with latest data for current lap:
       dashMessages.sendData("DATA_LAP_NUMBER",  lapNumber[0], null, null, null);
       dashMessages.sendData("DATA_LAP_KWH",     null, lapKWH[0], "%1.2f", null);
       dashMessages.sendData("DATA_LAP_AVG_KWH", null, lapAvgKWH, "%1.2f", null);
       dashMessages.sendData("DATA_LAP_TIME",    null, null, String.format("%02.0f:%04.1f", (float)Math.floor(lapTime[0] / 60f), (lapTime[0] % 60f) ), null);

       dashMessages.sendData("DATA_LAPS_REMAINING", lapsRemaining, null, null, null );
       dashMessages.sendData("DATA_KWH_REMAINING", null, kwhRemaining, "%1.2f", null );
       
       redrawLapData();
       
       //gpsTime   = bundleData.getFloat   ( "TIME",   0.0f  );
       //gpsSpeed  = bundleData.getFloat   ( "SPEED",  0.0f  );
       //gpsSats   = bundleData.getInt     ( "NSATS",  0     );

       }
        
    }
  
  }
