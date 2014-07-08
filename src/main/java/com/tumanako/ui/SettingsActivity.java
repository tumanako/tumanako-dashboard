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

import java.util.Set;

import com.tumanako.sensors.VehicleData;

import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;



public class SettingsActivity extends UIActivity implements OnItemClickListener
  {

  // Keepalive Timer Handler:
  // DEPRECATED private Handler uiTimer = new Handler();
  //DEPRECATED private static final int UPDATE_EVERY = 500;   // Update the UI every n mSeconds.


  private ListView deviceList;
  private String[] addressList;
  
  
   
  @Override
  public void onCreate(Bundle savedInstanceState) 
    {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    // Turn off auto-reset for this page: 
    autoReset = false; 

    // Get a reference to the Bluetooth Devices listview: 
    deviceList = (ListView) findViewById(R.id.listBluetoothDevices);
    
    // Populate the list of Bluetooth adaptors:
    ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
    if (pairedDevices.size() > 0) 
      {
      addressList = new String[pairedDevices.size()];
      int i = 0;
      for (BluetoothDevice device : pairedDevices) 
        {
        String deviceBTName = device.getName();
        listAdapter.add(deviceBTName);
        addressList[i] = new String(device.getAddress());
        i++;
        }
      }
    else
      {
      // No paired bluetooth devices!
      dashMessages.sendData(UIActivity.UI_TOAST_MESSAGE, null, null, "No paired devices found!", null );
      }
    deviceList.setAdapter(listAdapter);
    deviceList.setOnItemClickListener(this);

    // Set up update timer to keep services alive: 
 // DEPRECATED     uiTimer.removeCallbacks(uiTimerTask);            // ...Make sure there is no active callback already....     
 // DEPRECATED     uiTimer.postDelayed(uiTimerTask, UPDATE_EVERY);  // ...Callback later!
    
    }
  
  

  public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
    {  
    // We don't need to process any messages, but need to call UIActivity version:
    super.messageReceived(action, intData, floatData, stringData, bundleData);
    }

  
 




  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
    switch (parent.getId())
      {
      case R.id.listBluetoothDevices:
        String selectedDevice = deviceList.getItemAtPosition(position).toString();
        SharedPreferences settings = getSharedPreferences(UIActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("btDeviceName", selectedDevice);
        editor.putString("btDeviceAddress",addressList[position]);
        editor.commit();        // Commit the edits!
        // Send an intent message to the bluetooth connection to tell it that the address has changed: 
        dashMessages.sendData(VehicleData.VEHICLE_DATA_BTADDRESS_CHANGE, null, null, null, null);        
        finish();
        break;
      } 
    }


  
  
  /**** UI Timer Handling Runnable: *******************
   * This runnable creates a timer to update the UI.
   * Note that this is a low priority UI update for 
   * triggering a keep-alive signal to data service. 
   ****************************************************
   DEPRECATED!!
  private Runnable uiTimerTask = new Runnable() 
   {
   // Creates a Runnable which will be called after a delay, to keep sensors, etc, alive while we are in the settings screen:  
   public void run()  
     {
     uiTimer.removeCallbacks(uiTimerTask);               // ...Make sure there is no active callback already....     

Log.i(com.tumanako.ui.UIActivity.APP_TAG, " Settings -> Tick (Data service keepalive)." );

     // Send Keep Alive to data service, etc:
     dashMessages.sendData(DataService.DATA_SERVICE_KEEPALIVE, null, null, null, null);
     dashMessages.sendData(VehicleData.VEHICLE_DATA_KEEPALIVE, null, null, null, null);
     uiTimer.postDelayed(uiTimerTask, UPDATE_EVERY);  // ...Callback later!
     } 
   };

*/
  
  
  
  }  // Class
