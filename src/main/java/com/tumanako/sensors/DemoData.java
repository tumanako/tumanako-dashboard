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
import android.os.Bundle;
import android.os.Handler;
import com.tumanako.dash.DashMessages;
import com.tumanako.dash.IDashMessages;
import com.tumanako.ui.UIActivity;

/**
 *  Tumanako Demo Data Input:
 *  -------------------------------
 *
 *  This class is designed to generate demo data to show the UI
 *  (if a connection to a Tumanako vehicle controller is not
 *  available!)
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class DemoData implements IDashMessages
{

  /** Data send interval (ms) */
  private final int UPDATE_INTERVAL = 200;
  private Handler updateTimer = new Handler();


  /**
   * Demo Data Message Intent Filter.
   * We will catch any intents with this identifier.
   */
  public static final String DEMO_DATA = "com.tumanako.sensors.demodata";

  /** Demo mode is running flag! */
  private boolean isRunning = false;

  private float kWh = 0f;

  // Calculated Values relating to estimated range.
  private float avgEnergyPerHour = 0f;
  private float avgEnergyPerKm = 0f;

  private DashMessages dashMessages;


  public DemoData(Context context)
  {
    // We are extending the 'DashMessages' class, and we need to call its Constructor here.
    dashMessages = new DashMessages(context, this, DEMO_DATA);
    dashMessages.resume();
  }

  /**
   * Sets the 'Demo' mode flag.
   * Other classes call this to start or stop the demo
   */
  public void setDemo(boolean thisIsDemo)
  {
    if (thisIsDemo) {
      startDemo();
    } else {
      stopDemo();
    }
  }

  @Override
  public void messageReceived(String action, int message, Float floatData, String stringData, Bundle data)
  {
  }

  private void startDemo()
  {
    isRunning = true;
    avgEnergyPerHour = 10f;
    avgEnergyPerKm = 0.1f;
    updateTimer.removeCallbacks(updateTimerTask);                 // Stop existing timer.
    updateTimer.postDelayed(updateTimerTask, UPDATE_INTERVAL);    // Restart timer.
  }

  private void stopDemo()
  {
    isRunning = false;
    updateTimer.removeCallbacks(updateTimerTask);   // Stop existing timer.
  }






  /**
   * Update Timer:.
   * Runs on a timer, and sends fake data to the UI as long as demo mode is active.
   */
  private Runnable updateTimerTask = new Runnable()
  {
    public void run()
    {
      updateTimer.removeCallbacks(updateTimerTask); // ...Make sure there is no active callback already....

      // Generate some fake data  and send it to the UI
      kWh = kWh + 1f;  if (kWh > 30f) kWh = 10f;
      float thisRPM = ((android.util.FloatMath.sin((float)(System.currentTimeMillis() % 12000) / 1909f  ) + 0.3f) * 3000f);
      float demoFault   = (thisRPM < -1500)                              ? 1f : 0f;
      float demoReverse = (thisRPM < 0)                                  ? 1f : 0f;
      float contactorOn = (thisRPM > 1)                                  ? 1f : 0f;
      float preCharge = ((System.currentTimeMillis() % 300) > 100)  ? 1f : 0f;
      float driveTime  = (avgEnergyPerHour > 0f)  ?  (kWh / avgEnergyPerHour) : 99.99f;
      float driveRange = (avgEnergyPerKm   > 0f)  ?  (kWh / avgEnergyPerKm)   : 9999f;
      Bundle vehicleData = new Bundle();
      vehicleData.putFloat("DATA_CONTACTOR_ON",      contactorOn       );
      vehicleData.putFloat("DATA_FAULT",             demoFault         );
      vehicleData.putFloat("DATA_MAIN_BATTERY_KWH",  kWh               );
      vehicleData.putFloat("DATA_ACC_BATTERY_VLT",   12.6f             );
      vehicleData.putFloat("DATA_MOTOR_RPM",         Math.abs(thisRPM) );
      vehicleData.putFloat("DATA_MOTOR_REVERSE",     demoReverse       );
      vehicleData.putFloat("DATA_MAIN_BATTERY_TEMP", 60-(thisRPM/100)  );
      vehicleData.putFloat("DATA_MOTOR_TEMP",        (thisRPM/56)+25   );
      vehicleData.putFloat("DATA_CONTROLLER_TEMP",   (thisRPM/100)+35  );
      vehicleData.putFloat("DATA_PRECHARGE",         preCharge         );
      vehicleData.putFloat("DATA_MAIN_BATTERY_VLT",  133.5f            );
      vehicleData.putFloat("DATA_MAIN_BATTERY_AH",   189.4f            );
      vehicleData.putFloat("DATA_AIR_TEMP",          19.6f             );
      vehicleData.putFloat("DATA_DATA_OK",           1f                );
      vehicleData.putFloat("DATA_DRIVE_TIME",        driveTime         );
      vehicleData.putFloat("DATA_DRIVE_RANGE",       driveRange        );
      dashMessages.sendData( UIActivity.UI_INTENT_IN, IDashMessages.VEHICLE_DATA_ID, null, null, vehicleData);

      if (isRunning) updateTimer.postDelayed(updateTimerTask, UPDATE_INTERVAL); // Restart timer
    }
  };
}
