/*
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
*/

package com.tumanako.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.tumanako.dash.DashMessages;
import com.tumanako.dash.DashMessageListener;
import com.tumanako.sensors.DataService;
import com.tumanako.sensors.VehicleData;
import java.util.Set;

public class SettingsActivity extends Activity implements OnItemClickListener, DashMessageListener
{

  /** Message Broadcaster to send Intents to data service */
  private DashMessages dashMessages;
  /** We will catch any intents with this identifier. */
  public static final String SETTINGS_ACTIVITY = "com.tumanako.ui.settings";

  /** Keep-alive Timer Handler */
  private Handler uiTimer = new Handler();
  /** Update the UI every n mSeconds. */
  private static final int UPDATE_EVERY = 500;

  private ListView deviceList;
  private String[] addressList;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    // We are extending the 'DashMessages' class, and we need to call its constructor here.
    dashMessages = new DashMessages(this, this, SETTINGS_ACTIVITY);
    dashMessages.resume();

    // Get a reference to the Bluetooth devices listview
    deviceList = (ListView) findViewById(R.id.listBluetoothDevices);

    // Populate the list of Bluetooth adaptors:
    ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
    if (pairedDevices.size() > 0) {
      addressList = new String[pairedDevices.size()];
      int i = 0;
      for (BluetoothDevice device : pairedDevices) {
        String deviceBTName = device.getName();
        listAdapter.add(deviceBTName);
        addressList[i] = device.getAddress();
        i++;
      }
    } else {
      // No paired bluetooth devices
      showPopupMessage("No paired devices found!");
    }
    deviceList.setAdapter(listAdapter);
    deviceList.setOnItemClickListener(this);

    // Set up update timer to keep services alive
    //   Make sure there is no active callback already
    uiTimer.removeCallbacks(uiTimerTask);
    //   Callback later!
    uiTimer.postDelayed(uiTimerTask, UPDATE_EVERY);
  }

  @Override
  public void messageReceived(String action, int message, Float floatData, String stringData, Bundle data)
  {
    // We don't need to process any messages.
  }

  /** Displays pop-up TOAST message */
  public void showPopupMessage(String ThisMessage)
  {
    Toast.makeText(getApplicationContext(), ThisMessage, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    switch (parent.getId()) {
      case R.id.listBluetoothDevices:
        String selectedDevice = deviceList.getItemAtPosition(position).toString();
        SharedPreferences settings = getSharedPreferences(UIActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("btDeviceName", selectedDevice);
        editor.putString("btDeviceAddress",addressList[position]);
        editor.commit(); // Commit the edits!
        // Send an intent message to the bluetooth connection
        // to tell it that the address has changed
        dashMessages.sendData(VehicleData.VEHICLE_DATA, VehicleData.VEHICLE_DATA_BTADDRESS_CHANGE, null, null, null);
        finish();
        break;
      default:
        // do nothing
    }
  }

  /**
   * UI Timer Handling Runnable.
   * Creates a Runnable which will be called after a delay,
   * to keep sensors, etc, alive while we are in the settings screen.
   * This runnable creates a timer to update the UI.
   * Note that this is a low priority UI update for
   * triggering a keep-alive signal to data service.
   */
  private Runnable uiTimerTask = new Runnable()
  {
    @Override
    public void run()
    {
      uiTimer.removeCallbacks(uiTimerTask); // Make sure there is no active callback already
      // Send Keep Alive to data service, etc
      dashMessages.sendData(DataService.DATA_SERVICE, DataService.DATA_SERVICE_KEEPALIVE, null, null, null);
      dashMessages.sendData(VehicleData.VEHICLE_DATA, VehicleData.VEHICLE_DATA_KEEPALIVE, null, null, null);
      //dashMessages.sendData(ChargeNode.CHARGE_NODE_INTENT, ChargeNode.CHARGE_NODE_KEEPALIVE, null, null, null);
      uiTimer.postDelayed(uiTimerTask, UPDATE_EVERY);  // Callback later!
    }
  };
}
