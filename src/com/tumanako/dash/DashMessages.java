package com.tumanako.dash;

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;



/**************************************************************************
 * Tumanako Dash Message Receiver / Broadcaster: 
 * 
 * This class provides a generic framework for passing messages between 
 * parts of the Tumanako app, using Intents.    
 * 
 * @author Jeremy
 *
 *************************************************************************/


public class DashMessages extends BroadcastReceiver
  {

  
  private LocalBroadcastManager messageBroadcaster;    
    // Will be used to send and receive intents from other parts of the app (E.g. UI)

  
  /******** Intent Content Strings: *******************
   * String constants to identify the data fields sent 
   * in each intent. 
   ***************************************************/
    
  public static final String DASHMESSAGE_MESSAGE = "com.tumanako.dash.message";
  
  public static final String DASHMESSAGE_FLOAT   = "com.tumanako.dash.value.float";
  public static final String DASHMESSAGE_STRING  = "com.tumanako.dash.value.string";
  public static final String DASHMESSAGE_DATA    = "com.tumanako.dash.value.data";
  
  
  /**** Message ID Constants: ***********************************************
   * These integer IDs are used as the 'value' of the DASHMESSAGE_MESSAGE
   * field in an intent. They idenitify the source of the message.    
   **************************************************************************/
  public static final int DASHMESSAGE_UNSPECIFIED  =   0;
  public static final int NMEA_GPS_SENSOR_ID       = 300;
  public static final int NMEA_PROCESSOR_ID        = 400;
  public static final int VEHICLE_DATA_ID          = 500;
  public static final int CHARGE_NODE_ID           = 1000;
  
  private String actionFilter = null; 
  
  private IDashMessages parent;

  
  
  /****** Constructor ***********************/
  public DashMessages(Context context, IDashMessages callbackParent, String intentActionFilter)
    {
    parent = callbackParent;

    // Get a Broadcast Manager so we can send out messages to other parts of the app, and receive mesages for this class:
    messageBroadcaster = LocalBroadcastManager.getInstance(context);  
  
    // Register to receive messages via Intents if a filter was provided:
    if (intentActionFilter != null)
      {
      actionFilter = intentActionFilter;
      resume();
      }

    }
  
  
  
  
  
  
  
  @Override
  public void onReceive(Context context, Intent intent)  
    {
    
    // Get the 'What Message?' from the intent. This is an integer sent by other parts
    // of the app, which identifies what they are trying to say to us: (note: meaning is defined by derived classes).
    String action = intent.getAction();
    int message = intent.getIntExtra( DASHMESSAGE_MESSAGE, DASHMESSAGE_UNSPECIFIED);
    
    Float floatData = null;
    String stringData = null;
    Bundle data = null;

    if ( intent.hasExtra(DASHMESSAGE_FLOAT)  ) floatData  = intent.getFloatExtra(DASHMESSAGE_FLOAT, 0f);
    if ( intent.hasExtra(DASHMESSAGE_STRING) ) stringData = intent.getStringExtra(DASHMESSAGE_STRING);
    if ( intent.hasExtra(DASHMESSAGE_DATA)   ) data       = intent.getBundleExtra(DASHMESSAGE_DATA);

    // --DEBUG!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, String.format( " DashMessages -> Msg Rec: %d", message) );

    parent.messageReceived(action, message, floatData, stringData, data);
    }
    

  
  
  
  /*****************************************************************************
   * Build Intent: 
   * Used to send data to another part of the app via an intent:
   * 
   *****************************************************************************/
    
  public void sendData(String action, int message, Float floatData, String stringData, Bundle data)
    {
    Intent intent = new Intent(action);
    intent.putExtra(DASHMESSAGE_MESSAGE, message);
    if ( floatData  != null ) intent.putExtra( DASHMESSAGE_FLOAT,  floatData   );
    if ( stringData != null ) intent.putExtra( DASHMESSAGE_STRING, stringData  );
    if ( data       != null ) intent.putExtra( DASHMESSAGE_DATA,   data        );
    messageBroadcaster.sendBroadcast(intent);
    }
  
  
  
  public void suspend()
    {
    if (messageBroadcaster != null) messageBroadcaster.unregisterReceiver(this);
    }
  
  
  public void resume()
    {
    if (actionFilter != null)  messageBroadcaster.registerReceiver(this,  new IntentFilter(actionFilter));
      // We are registering an observer (messageReceiver) to receive Intents
      // with action named.    
    }
  
  }
