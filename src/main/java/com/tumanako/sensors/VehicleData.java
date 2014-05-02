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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.tumanako.dash.DashMessages;
import com.tumanako.dash.IDashMessages;
import com.tumanako.ui.UIActivity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/****************************************************************
 *  Tumanako Vehicle Data Input:
 *  -------------------------------
 *
 *  This class is designed to connect to get a stream of data from
 *  the vehicle electronics.
 *
 *  The stream is decoded as necessary and sent to the UI as
 *  an Intent with a bundle of data (see DashMessages).
 *
 *  Connection uses Bluetooth. The class extends Thread and
 *  launches the bluetooth connection in a new thread so that
 *  it can listen for incomming data without stalling the UI.
 *
 *  A 'Watchdog' mechanism is used to keep the connection alive.
 *  This class implements IDashMessages and registers to listen
 *  for intents directed to "VEHICLE_DATA".
 *
 *  During normal operation, a timer (watchdogTimer) is triggered
 *  on an interval (1s) and increments a watchdog counter.
 *  If this counter passes a maximum value, we assume that the
 *  BT connection is no longer required (i.e. UI has closed),
 *  and the thread stops itself and carries out cleanup (closes
 *  bluetooth connections, etc).
 *
 *  While the UI is active, it should send intent messages to
 *  the vehicle data class. When this class receives an
 *  intent message through its DashMessages object, it resets
 *  the watchdog counter, thereby keeping itself alive.
 *
 *  Note that the above applies when the parent of this class
 *  is a persistent service (e.g. DataService) which stays
 *  alive when the UI is closed / suspended. The bluetooth
 *  keepalive timer should be set up to be shorter than the
 *  DataService keepalive timer so that bluetooth connection
 *  can close and exit cleanly before the service which created
 *  it is stopped.
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************************************/

public class VehicleData extends Thread implements IDashMessages
  {

  private final Handler watchdogTimer = new Handler();      // Watchdog timer: checks bluetooth status.

  /***** Bluetooth constants and objects: **************************/
  private static final int BT_WATCHDOG_TIME     = 1000;      // Check bluetooth connection every n mSec
  private static final int BT_WATCHDOG_MAXCOUNT = 2;         // Close down bluetooth after BT_WATCHDOG_TIME x n without a 'KeepAlive' message from UI.
  /*****************************************************************/
  private static final int BT_READ_SIZE         = 200;         // Max Number of characters we read per stream read. Not too critical.
  private static final int BT_STREAM_OVERFLOW   = 600;         // If there are more than this many bytes left in the BT input stream after processing, we should dump some.
  /*****************************************************************/
  private final BluetoothAdapter bluetoothAdapter;
  private BluetoothDevice  btVehicleSensor;
  private BluetoothSocket  btSocket;
  private InputStream      btStreamIn;
  private OutputStream     btStreamOut;
  private final UUID       myUUID;
  /*****************************************************************/
  private volatile boolean isBTConnected = false;       // Internal flag which indicates when the BT connection is established.
  private volatile boolean isFinished = false;          // Internal flag which signals when the comms loop has ended and the BT connection has been closed.
  private volatile boolean isAddressChanged = false;    // Internal flag which signals when the BT device address has been changed by another part of the app.
  /*****************************************************************/


  /****** Vehicle Data Message Intent Filter: *********/
  public static final String VEHICLE_DATA = "com.tumanako.sensors.vehicledata";
       // We will catch any intents with this identifier.

  /****** Message types we recognise: ******************
   * Used in the 'message' field of intents sent to us:
   * ***************************************************/
  public static final int VEHICLE_DATA_KEEPALIVE         = IDashMessages.VEHICLE_DATA_ID + 1;   // A 'KeepAlive' message, telling us that the UI is still active and the connection is still required.
  public static final int VEHICLE_DATA_BTADDRESS_CHANGE  = IDashMessages.VEHICLE_DATA_ID + 2;   // Bluetooth address change! (I.e. user selected different BT device). New address will be included in stringData field of message.


  private final DashMessages dashMessages;
  private int watchdogCounter = 0;
  private final Context vehicledataContext;


  /*********** TEMP DEBUG ********************************
   * This update timer can be used to trigger output of
   * debug info during BT operations.
   * Warning! May not stop timer on thread completion!!
   ******************************************************/
  private volatile boolean isPing = false;
  private Handler uiTimer = new Handler();
  private static final int UI_UPDATE_EVERY = 500;   // Update the UI every n mSeconds.
  private Runnable uiTimerTask = new Runnable()
    {
    public void run()
      {
      uiTimer.removeCallbacks(uiTimerTask);
      isPing = true;
    //  uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);  // ...Callback later!
      }
    };
  /*******************************************************/


  // ************** Constructor: *****************************************
  public VehicleData(Context context)
    {

    // --DEBUG!!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> Constructor; ");

    vehicledataContext = context;

    /*********** Bluetooth Init: *******************/
    myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    // Build a UUID for a RfComm connection (used when connecting)
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();                   // Create a Bluetooth adaptor object
    isBTConnected = false;

    dashMessages = new DashMessages(context, this, VEHICLE_DATA);    // We are extending the 'DashMessages' class, and we need to call its Constructor here.
    dashMessages.resume();

    /****** Setup Bluetooth Watchdog Timer: ********/
    watchdogTimer.postDelayed(watchdogTimerTask, BT_WATCHDOG_TIME);      // ...Callback in n milliseconds!


    /********* TEMP DEBUG **************************/
    //uiTimer.removeCallbacks(uiTimerTask);               // ...Make sure there is no active callback already....
    //uiTimer.postDelayed(uiTimerTask, UI_UPDATE_EVERY);  // ...Callback later!
    /********* TEMP DEBUG **************************/

    this.start();     // Launch a new thread to connect to the vehicle with Bluetooth!

    }  // Constructor






 /********** Dash Message Received: *************************************
  * Called when we receive an intent message via our Dashmessage object.
  ***********************************************************************/
 public void messageReceived(String action, int message, Float floatData, String stringData, Bundle data )
   {
   // Message Intent Received: Check type of message.
   // We respond to 'keep alive' messages and bluetooth address changes.
   watchdogCounter = 0;     // Whatever the type of message, treat it as a 'keep alive' event and reset watchdog counter.

   if (message == VEHICLE_DATA_BTADDRESS_CHANGE)
     {
     // Bluetooth device address has changed! Note: We'll only do this if we've been sent a string (should be new address).
     isAddressChanged = true;    // This flag tells the connection thread to reconnect with the new address.
     }
   }



  /******** Methods to return status: ***************************/

 public boolean isConnected()    // Is the bluetooth socket connected?
    {  return isBTConnected;  }

  public boolean isFinished()    // Has the bluetooth thread finished and terminated? (Usually caused by loss of connection or watchdog timeout).
    {  return isFinished;  }





  /******** Cleanup: ****************************************
   * This is Called when the thread is to be terminated.
   * Stops all BT activity and closes the socket:
   **********************************************************/
  private void stopVehicleData()
    {
    // *** Stop the vehicle sensor... ***
    // --DEBUG!!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> stopVehicleData(); ");
    btClose();   // Close the BT connection
    // Stop the update timer if it's running:
    watchdogTimer.removeCallbacks(watchdogTimerTask);       // Stop timer.
    dashMessages.suspend();                                 // Stop the DashMessages object (unregisters intent listener)
    isBTConnected = false;                                  // This will tell the BT connection thread to exit.
    }










  /******* Watchdog Timer: **********************************************/
  private Runnable watchdogTimerTask = new Runnable()
   {
   public void run()
     {
     watchdogTimer.removeCallbacks(watchdogTimerTask);                    // ...Make sure there is no active callback already....
     watchdogCounter++;

     if (watchdogCounter > BT_WATCHDOG_MAXCOUNT) stopVehicleData();                                                   // Watchdog Counter Overflow! We haven't been told to keep going, so stop the BT thread:
     else                                        watchdogTimer.postDelayed(watchdogTimerTask, BT_WATCHDOG_TIME);      // ...Callback in n milliseconds!

     }
   };















  /************* Data Decode / Send: *****************************
   * This method decodes a string of data received from the input stream
   * and fills in the various data fields in vehicleData.
   * @param thisData - A string containing encoded vehicle data.
   *
   *  Data Format:
   *  TDV1:3670,54,52,32,375,138,214,1,0
   *
   *  Which equates to:
   *    RPM = 3670
   *    motorTemp = 54 deg C
   *    inverterTemp = 52 deg C
   *    packTemp = 32 deg C
   *    pack volt = 375 V
   *    acc volt = 13.8 V
   *    kWhr = 21.4
   *    Contactor = ON
   *    Fault = OFF (i.e. no fault)
   *
   *    Note that acc volt and kWhr are multiplied by 10.
   *
   ***************************************************************/

  private void decodeAndSend(String thisData)
    {
    if ((!thisData.startsWith("TDV1:")) ||
        (thisData.length() < 20)) return;  // Line doesn't start with the 'TDV1' tag, or it's too short. Give up.

    String dataPart = thisData.substring(5);   // Get the part AFTER the tag.
    String[] splitData = dataPart.split(",");  // Split the data at the comma characters.

    float motorRPM     = 0f;
    float tMotor       = 0f;
    float tController  = 0f;
    float tPack        = 0f;
    float voltPack     = 0f;
    float voltAcc      = 0f;
    float kWh          = 0f;
    float contactorOn  = 0f;
    float faultOn      = 0f;
    float motorReverse = 0f;

    // Extract each value from the string (now expanded to an array).
    // For simplicity, all values are sent as floating point.
    // We'll wrap this in a tyy / catch block, so that corrupt data
    // won't crash the app (Float.parseFloat will fail if the string
    // it's given isn't a valid number).
    try
      {
      motorRPM    = Float.parseFloat(splitData[0]);
      tMotor      = Float.parseFloat(splitData[1]);
      tController = Float.parseFloat(splitData[2]);
      tPack       = Float.parseFloat(splitData[3]);
      voltPack    = Float.parseFloat(splitData[4]);
      voltAcc     = Float.parseFloat(splitData[5]) / 10;
      kWh         = Float.parseFloat(splitData[6]) / 10;
      contactorOn = Float.parseFloat(splitData[7]);
      faultOn     = Float.parseFloat(splitData[8]);
      }
    catch (NumberFormatException e)
      { }

    motorReverse = (motorRPM < 0) ? 1f : 0f;  // This turns on the reverse indicator lamp if the RPM is negative.
    motorRPM = Math.abs(motorRPM);            // Convert negative RPM into positive for display.

    // Make the data up into a 'Bundle', using the data type indicators
    // defined above as 'keys':
    Bundle vehicleData = new Bundle();
    vehicleData.putFloat("DATA_CONTACTOR_ON",      contactorOn       );
    vehicleData.putFloat("DATA_FAULT",             faultOn           );
    vehicleData.putFloat("DATA_MAIN_BATTERY_KWH",  kWh               );
    vehicleData.putFloat("DATA_ACC_BATTERY_VLT",   voltAcc           );
    vehicleData.putFloat("DATA_MOTOR_RPM",         motorRPM          );
    vehicleData.putFloat("DATA_MOTOR_REVERSE",     motorReverse      );
    vehicleData.putFloat("DATA_MAIN_BATTERY_TEMP", tPack             );
    vehicleData.putFloat("DATA_MOTOR_TEMP",        tMotor            );
    vehicleData.putFloat("DATA_CONTROLLER_TEMP",   tController       );
    vehicleData.putFloat("DATA_PRECHARGE",         0f                );
    vehicleData.putFloat("DATA_MAIN_BATTERY_VLT",  voltPack          );
    vehicleData.putFloat("DATA_MAIN_BATTERY_AH",   0f                );
    vehicleData.putFloat("DATA_AIR_TEMP",          0f                );
    vehicleData.putFloat("DATA_DATA_OK",           1f                );
    vehicleData.putFloat("DATA_DRIVE_TIME",        0f                );
    vehicleData.putFloat("DATA_DRIVE_RANGE",       0f                );

    // Now transmit the data to the UI by sending a message!
    dashMessages.sendData( UIActivity.UI_INTENT_IN, IDashMessages.VEHICLE_DATA_ID, null, null, vehicleData );

    }











   /********** Open Bluetooth connection! ****************************************
    * This mmethod tries to establish a bluetooth connection.
    * @return true on success, false if an error occurs.
    ******************************************************************************/
   private boolean btOpen()
     {

     /***** Make sure the bluetooth adaptor is on: *******/
     if ((bluetoothAdapter == null) || (bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON))
       {
       //dashMessages.sendData( UIActivity.UI_INTENT_IN, UIActivity.UI_TOAST_MESSAGE, null, "Bluetooth Adaptor Not Available!", null );
       return false;
       }
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, "             -> Adaptor is ON! ");

     /****** Get a bluetooth device for the vehicle sensor: ***************/
     //  We need to retrieve the device address of the selected bluetooth device from the stored app preferences:
     SharedPreferences settings = vehicledataContext.getSharedPreferences(UIActivity.PREFS_NAME, 0);
     String btDeviceAddress = settings.getString("btDeviceAddress", "");
     // Check the address:
     if (!BluetoothAdapter.checkBluetoothAddress(btDeviceAddress)) return false;   // Invalid address. Give up.
     btVehicleSensor = bluetoothAdapter.getRemoteDevice(btDeviceAddress);          // "00:12:05:17:91:65" = MDFlyBTSerial bluetooth serial device (for testing).
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, "             -> Got remote device OK! " );

     /************* Try to establish a BT Connection: ***********************************/
     try
       {
       if (btSocket != null) btSocket.close();                                 // If there's already a BT Socket open, close it.
       btSocket = btVehicleSensor.createRfcommSocketToServiceRecord(myUUID);   // Create a new BT Socket for RF Comm connection.
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, "             -> Socket Created. " );
       bluetoothAdapter.cancelDiscovery();                                     // Cancel any bluetooth discovery that's running (in case another app started it...) According to the docs, we should do this...
       btSocket.connect();                                                     // Attempt to connect!!!
       }

     catch (IOException ioe)
       {
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, "             -> Connection Attempt Generated Error! Trying Workaround... " );
       // Our attempt to open a BT connection caused an IO exception. This could be due to a bug in
       // the bluetooth class. Try again using a call to an internal method in createRfcommSocket class:
       // (See http://stackoverflow.com/questions/4444235/problems-connecting-with-bluetooth-android )
       Method m;
       try
         {
         m = btVehicleSensor.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
         if (btSocket != null) btSocket.close();    // If there's already a BT Socket open, close it.
         btSocket = (BluetoothSocket)m.invoke(btVehicleSensor, 1);
         btSocket.connect();
         }
       catch (Exception e)
         {
         // Still having errors connecting! Give up.
         Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Com Thread: Comm Error Persisted. Giving up. ");
         Log.w(com.tumanako.ui.UIActivity.APP_TAG, e);
         return false;  // Give up.
         }
       }

     // Connected! Try to open streams...
     try
       {
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, "             -> CONNECTED. " );
       btStreamIn  = btSocket.getInputStream();                 // Get input and output streams
       btStreamOut = btSocket.getOutputStream();                //  for communication through the socket.
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, "             -> IO Streams Open! " );
       return true;                                  // SUCCESS!!
       }

     catch (IOException e)
       {
       // An error occurred during BT comms setup:
       Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Com Thread: Error opening IO Streams... ");
       Log.i(com.tumanako.ui.UIActivity.APP_TAG, e.getMessage());
       return false;  // Give up.
       }

     }








   /************** Bluetooth socket close / cleanup: ******************************************************/
   private void btClose()
     {
     // --DEBUG!!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> btClose(); ");
     try
       {
       if (btStreamIn != null)  btStreamIn.close();
       if (btStreamOut != null) btStreamOut.close();
       if (btSocket != null)    btSocket.close();
       }
     catch (IOException e)
       {  Log.w(com.tumanako.ui.UIActivity.APP_TAG, e);  }  // If an error occurs here, print a stack trace (debug only) but otherwise quietly ignore.
     }















  /**********************************************************************************************************************
   *************** Bluetooth Communications Thread: *********************************************************************
   **********************************************************************************************************************/

  @Override
  public void run()
      {
      byte[] byteBuffer = new byte[BT_READ_SIZE];
      StringBuffer btRawData = new StringBuffer();
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Com Thread Run ");
      // Try to open a BT connection:
      if (!btOpen())
        {
        // Connection failed! Close any open objects and exit.
        stopVehicleData();
        isFinished = true;
        return;
        }
      isBTConnected = true;
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Com Thread Connected. ");



      // ******* BT Connection should now be open! Keep listening to the InputStream while connected: *********************************
      int bytesRead;
      while (isBTConnected)
          {

          // Check to see if the bluetooth address has changed:
          if (isAddressChanged)
            {
            // To change BT address, we shut down the existing connection and reopen with new address:
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Address Change! ");
            isBTConnected = false;
            isAddressChanged = false;
            btClose();     // Close existing connection.
            if (!btOpen()) break;   // If we failed to open a connection, exit BT loop.
            isBTConnected = true;
//Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Reconnected OK. ");
            }
/****** TEMP DEBUG ************************************/
//if (isPing)
//  {
//try  {  Log.i("BT_STREAM", String.format("%d",btStreamIn.available()));  } catch (IOException e) {  e.printStackTrace();  }
//isPing = false;
//  }
/****** TEMP DEBUG ************************************/
          try
            {
            // Read bytes from the InputStream:
            bytesRead = btStreamIn.read(byteBuffer,0,100);  // Reads up to 100 bytes.
            /*********** Buffer Overflow Check: *******************************************
             * It is very important that the UI remain up-to-date (this is more important than
             * trying to process ALL data).
             * Therefore, if the input stream is filling with data faster than we are processing
             * it, we have to dump data.
             * Note that this relies on the behaviour of the StreamIn.available() method
             * to report the number of bytes available. The documentation indicates that
             * available() may not be reliablie, and in particular, "may be significantly
             * smaller than the actual number of bytes available." However, testing shows
             * that it works well. The following code will work fine so long as available()
             * doesn't return /more/ than the number of bytes available, which it shouldn't!
             ******************************************************************************/
            int bytesInStream = btStreamIn.available();
            if (bytesInStream > BT_STREAM_OVERFLOW) btStreamIn.skip(bytesInStream - BT_STREAM_OVERFLOW);
            /******************************************************************************/
            if (bytesRead > 0)  // ACTUAL number of bytes read.
              {
              for (int n = 0; n < bytesRead; n++)
                {
                // Process the incomming aray of bytes:
                if (byteBuffer[n] == 0x0D)
                  {
                  // End of line! Send record:
                  decodeAndSend(btRawData.toString());
                  btRawData = new StringBuffer();        // Reset the line buffer.
                  }
                else
                  {
                  if (byteBuffer[n] != 0x0A) btRawData.append((char)byteBuffer[n]);  // If this is not a LF character, add it to the line buffer.
                  }
                }  // [for (int n = 0; n < bytesRead; n++)]
              }  // [if (bytesRead > 0)]

            Thread.sleep(1);  // Give up CPU time (waiting for bluetooth characters is so tedious...)
            }  // try...

          catch (Exception e)
            {
            // An error occurred during BT comms operation:
            Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Com Thread: Error During Comms... ");
            isBTConnected = false;
            break;
            }

          }  // [while (true)]

       // Close down the input and ouptut streams and the bluetooth socket:
       stopVehicleData();
       isFinished = true;
       Log.i(com.tumanako.ui.UIActivity.APP_TAG, " VehicleData -> BT Com Thread Exit! ");

      }  // [run()]







  }  // [class VehicleData]
