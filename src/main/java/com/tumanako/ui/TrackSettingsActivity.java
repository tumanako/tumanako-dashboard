package com.tumanako.ui;

/************************************************************************************

Tumanako - Electric Vehicle and Motor control software <p>

Copyright (C) 2012 Jeremy Cole-Baker <jeremy@rhtech.co.nz> <p>

This file is part of Tumanako Dashboard. <p>

Tumanako is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. <p>

Tumanako is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details. <p>

You should have received a copy of the GNU Lesser General Public License
along with Tumanako.  If not, see <http://www.gnu.org/licenses/>. <p>

@author Jeremy Cole-Baker / Riverhead Technology <jeremy@rhtech.co.nz> <p>

*************************************************************************************/



import com.tumanako.sensors.LapData;
import com.tumanako.sensors.NmeaProcessor;
import com.tumanako.ui.TextBox;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;




public class TrackSettingsActivity extends UIActivity
  {
  private double  gpsLat    = 0.0;
  private double  gpsLon    = 0.0;
  private boolean gpsHasFix      = false;
  private boolean startFinishSet = false;
  private float   startBoxRadius = LapData.START_FINISH_BOX_RADIUS;
 
  
  @Override
  public void onCreate(Bundle savedInstanceState) 
    {
    // Add our extra intent filters... (must be called before super.onCreate)
    final String intentFilters[] = { NmeaProcessor.GPS_POSITION, "CLICK_SETSTARTFINISH", "CLICK_RESETLAPDATA" };
    setExtraIntentFilters(intentFilters);
    
    super.onCreate(savedInstanceState);
    setContentView(R.layout.track_settings);

    // Turn off auto-reset for this page: 
    autoReset = false; 
    
    // Read previous settings from prefs file (if any):
    SharedPreferences settings = getSharedPreferences(UIActivity.PREFS_NAME, 0);
    gpsLat = settings.getFloat("startFinishLat", 0.0f);
    gpsLon = settings.getFloat("startFinishLon", 0.0f);
    startFinishSet = settings.getBoolean("startFinishSet", false);
    startBoxRadius = settings.getFloat("startBoxRadius", LapData.START_FINISH_BOX_RADIUS);

    if (startFinishSet)
      {
      ((TextBox)findViewById(R.id.textStartFinishLat)).setText( String.format( "%1.5f", gpsLat) );
      ((TextBox)findViewById(R.id.textStartFinishLon)).setText( String.format( "%1.5f", gpsLon) );
      }
    
    ((EditText)findViewById(R.id.editTextStartBoxRadius)).setText( String.format( "%1.0f", startBoxRadius) );
    
    
    }
  
  
  
  

  public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
    {  
    super.messageReceived(action, intData, floatData, stringData, bundleData);
    
    // *** Check for button press messages: ***
    if (action.equals("CLICK_SETSTARTFINISH")) 
      {
      // Set Latitude and Longitude of start / finish location:  
      if (gpsHasFix)
        {
        ((TextBox)findViewById(R.id.textStartFinishLat)).setText( String.format( "%1.5f", gpsLat) );
        ((TextBox)findViewById(R.id.textStartFinishLon)).setText( String.format( "%1.5f", gpsLon) );

        SharedPreferences settings = getSharedPreferences(UIActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("startFinishLat", (float)gpsLat);
        editor.putFloat("startFinishLon", (float)gpsLon);
        editor.putBoolean("startFinishSet", true );
        editor.commit();        // Commit the edits!
        startFinishSet = true;
        finish();
        }
      else
        {
        // Can't set start / finish as there is no GPS position:
        dashMessages.sendData( UIActivity.UI_TOAST_MESSAGE, null, null, "Can't set Start/Finish Location: No GPS!", null );
        }
      }  // [if (action.equals("CLICK_SETSTARTFINISH"))]
    
    if (action.equals("CLICK_RESETLAPDATA")) 
      {
      // Reset the lap data: Actually, we don't do anything in this case. Intent should be handled by LapData class.  
      finish();
      }  // [if (action.equals("CLICK_RESETLAPDATA"))]
    
    
    // *** Check for GPS data update: *** 
    if ( (action.equals(NmeaProcessor.GPS_POSITION)) && (null != bundleData) )
      {
      // GPS position received:
      gpsHasFix = bundleData.getBoolean ( "FIX",    false );
      if (gpsHasFix)
        {
        gpsLat    = bundleData.getDouble  ( "LAT",    0.0   );
        gpsLon    = bundleData.getDouble  ( "LON",    0.0   );
        }
      else
        {
        gpsLat    = 0.0;
        gpsLon    = 0.0;
        }
      }
    }

  
  
  
  
  
  
  
  
   /**
   UI Pause Event - Called when the activity goes into the background 
   */
  @Override
  protected void onPause() 
    {
    super.onPause();
    
    // Get the text from the start box radius field, and try to turn it into a number:
    try
      {
      startBoxRadius = Float.parseFloat(  ((EditText)findViewById(R.id.editTextStartBoxRadius)).getText().toString()  );
      }
    catch (Exception e)
      {
      // Couldn't read the number format. Use the default: 
      startBoxRadius = LapData.START_FINISH_BOX_RADIUS;
      }
    
    // Save the start box radius setting:  
    SharedPreferences settings = getSharedPreferences(UIActivity.PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putFloat("startBoxRadius", startBoxRadius);
    editor.commit();        // Commit the edits!

    // Send an intent message to tell the rest of the app that the start / finish location has changed.   
    dashMessages.sendData(LapData.START_FINISH_SET, null, null, null, null);        

    }

 
 
 
  
  }  // Class
