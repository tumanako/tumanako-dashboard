package com.tumanako.dash;

/**
Tumanako - Electric Vehicle and Motor control software <p>

Copyright (C) 2014 Jeremy Cole-Baker <jeremy@rhtech.co.nz> <p>

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

@author Jeremy Cole-Baker / Riverhead Technology

*/


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;



/**
 Tumanako Dash Message Receiver / Broadcaster <p>
 
 This class provides a generic framework for passing messages between 
 parts of the Tumanako app, using Intents. <p>    
 
 Each DashMessage class instance is configured to receive intents
 with a particular identifier as specified by the intentActionFilter
 passed to the constructor. <p>
 
 Intents passed to a DashMessages class should include an integer 
 extra value identified by the string DASHMESSAGE_MESSAGE (const 
 defined below). The VALUE of this integer tells us the source of the 
 received message. <p>   
 
 @author Jeremy Cole-Baker / Riverhead Technology

*/
public class DashMessages extends BroadcastReceiver
  {

  private LocalBroadcastManager messageBroadcaster;    
    // Will be used to send and receive intents from other parts of the app (E.g. UI)

  
  /*** Extra Data Field Identifiers: ***********************************************
   * These are used as names for extra data included in the intent.  
   *********************************************************************************/
  private static final String DASHMESSAGE_INT     = "i";
  private static final String DASHMESSAGE_FLOAT   = "f";
  private static final String DASHMESSAGE_STRING  = "s";
  private static final String DASHMESSAGE_DATA    = "d";
  
  private String actionFilter[] = null; 
  
  private IDashMessages parent;

  
  
  
  /**
   DashMessages Constructor
     
    @param context              Reference to the application context. Used to get an application-specific
                                broadcast manager object. 
                                  
    @param callbackParent       Reference to the object which will provide the messageReceived
                                method to handle messages. Must refer to an object which implements 
                                IDashMessages interface. 
                                 
    @param intentActionFilters  Array of strings to use as action filters. This DashMessages object 
                                will respond to any intent with one of these strings as the "action".  
    
   */
  public DashMessages(Context context, IDashMessages callbackParent, String intentActionFilters[])
    {
    parent = callbackParent;
    
    // Get a Broadcast Manager so we can send out messages to other parts of the app, and receive mesages for this class:
    messageBroadcaster = LocalBroadcastManager.getInstance(context);  
  
    // Register to receive messages via Intents if a filter was provided:
    if (intentActionFilters != null) actionFilter = intentActionFilters.clone();
    resume();
    
    }
  
  
  
  
  
  
  
  
  @Override
  public void onReceive(Context context, Intent intent)  
    {
    
    // Get the 'Action' from the intent:
    String action = intent.getAction();
    
    // Get other data (if it was sent):
    Integer intData   = null;
    Float floatData   = null;
    String stringData = null;
    Bundle bundleData = null;

    if ( intent.hasExtra(DASHMESSAGE_INT)    ) intData    = intent.getIntExtra(DASHMESSAGE_INT, 0);
    if ( intent.hasExtra(DASHMESSAGE_FLOAT)  ) floatData  = intent.getFloatExtra(DASHMESSAGE_FLOAT, 0f);
    if ( intent.hasExtra(DASHMESSAGE_STRING) ) stringData = intent.getStringExtra(DASHMESSAGE_STRING);
    if ( intent.hasExtra(DASHMESSAGE_DATA)   ) bundleData = intent.getBundleExtra(DASHMESSAGE_DATA);

    // --DEBUG!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, String.format( " DashMessages -> Msg Rec: %d", message) );

    parent.messageReceived(action, intData, floatData, stringData, bundleData);
    }
    

  
  
  
  /*****************************************************************************
   * Build Intent: 
   * Used to send data to another part of the app via an intent:
   * 
   *****************************************************************************/
    
  public void sendData(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
    {
    Intent intent = new Intent(action);
    if ( intData    != null ) intent.putExtra( DASHMESSAGE_INT,    intData     );
    if ( floatData  != null ) intent.putExtra( DASHMESSAGE_FLOAT,  floatData   );
    if ( stringData != null ) intent.putExtra( DASHMESSAGE_STRING, stringData  );
    if ( bundleData != null ) intent.putExtra( DASHMESSAGE_DATA,   bundleData  );
    messageBroadcaster.sendBroadcast(intent);    
    }
  
  
  
  public void suspend()
    {
    if (messageBroadcaster != null) messageBroadcaster.unregisterReceiver(this);
    }
  
  
  public void resume()
    {
    if (messageBroadcaster != null) messageBroadcaster.unregisterReceiver(this);
    if (actionFilter != null)   
      {
      for (int n=0; n<actionFilter.length; n++)
        {
        if (null != actionFilter[n]) messageBroadcaster.registerReceiver(this,  new IntentFilter(actionFilter[n]));
          // Register to receive each intent action in the supplied list. 
        }
      }      
    }
  
  
  
 }  // Class
