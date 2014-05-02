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

/**************************************************************************
 * Tumanako Dash Message Receiver / Broadcaster:
 *
 * This class provides a generic framework for passing messages between
 * parts of the Tumanako app, using Intents.
 *
 * Each DashMessage class instance is configured to receive intents
 * with a particular identifier as specified by the intentActionFilter
 * passed to the constructor.
 *
 * Intents passed to a DashMessages class should include an integer
 * extra value identified by the string DASHMESSAGE_MESSAGE (const
 * defined below). The VALUE of this integer tells us the source of the
 * received message.
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 *************************************************************************/

public class DashMessages extends BroadcastReceiver
  {


  private final LocalBroadcastManager messageBroadcaster;
    // Will be used to send and receive intents from other parts of the app (E.g. UI)



  /*** String Identifiers to identify different elements of the Intent structure we are using: ****/
  public static final String DASHMESSAGE_MESSAGE = "com.tumanako.dash.message";   // This is the name of the integer field in the received intent, which tells us the source of the message.


  /*** Extra Data Field Identifiers: ***********************************************
   * These are used as names for extra data included in the intent.
   *********************************************************************************/
  private static final String DASHMESSAGE_FLOAT   = "f";
  private static final String DASHMESSAGE_STRING  = "s";
  private static final String DASHMESSAGE_DATA    = "d";

  private String actionFilter = null;

  private final IDashMessages parent;



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
    int message = intent.getIntExtra( DASHMESSAGE_MESSAGE, 0);

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
