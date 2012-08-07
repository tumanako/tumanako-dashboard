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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;


/****************************************************************
 *  Bluetooth Data Input:
 *  -------------------------------
 *
 *  This class is designed to connect to get a stream of data from 
 *  the vehicle electronics.
 *   
 *  The stream is decoded as necessary and data are stored in a 
 *  DashData object which a parent class can retrieve.
 *  
 *   Messages are passed to the parent class to inform it of
 *   status changes. 
 *  
 *  Internally, this class uses a Bluetooth serial connection
 *  to the actual vehicle hardware.    
 *   
 *  BTComm thread: 
 *   
 *  BT communications are carried out with a seperate thread,
 *  so that BT operations (e.g. waiting for data) do not block
 *  the UI. 
 *   
 *  BEWARE THREAD SAFETY! 
 *    Hopefully this class is thread safe (i.e. calling the 
 *    public methods from the UI thread won't cause any 
 *    problems even if the BTComm thread is doing something).  
 *   
 *  The thread uses a runnable internally to create a timer. 
 *  This timer is used to read the sensor every 0.5 second. 
 *  
 *  Watchdog Timer: 
 *  
 *  We want to automatically connect when the app is running. 
 *  To do this, the class uses a 'Watchdog timer'.
 *  The Watchdog is set up to run the mWDTimerTask runnable 
 *  every 10 seconds (or interval specified by WATCHDOG_TIMEOUT
 *  below). The mWDTimerTask checks to see whether the BTComm
 *  thread exists and is connected. If this is NOT the case, a 
 *  new connection is attempted.  
 *  
 *  
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************************************/


public class VehicleDataBt implements IDroidSensor 
  {

  private final Handler messageHandler;                    // Used to send messages back to UI class (reference will be passed to the constructor).
  private final Handler updateTimer = new Handler();       // Message handler for sensor update timer
  private final Handler watchdogTimer = new Handler();     // Message handler for connection watchdog timer
  
  private BTCommThread mBTCommThread;              // Thread class to do blocking work for us (e.g. waiting for data from bluetooth).
  
  private String lastData = "";                   // Stores the last full data record (for debug, etc)
  private volatile boolean isRunning = false;     // Internal flag to indicate whether vehicle data input is in a 'running' state (i.e. data being received) or not.  
  private final UUID myUUID;                      // UUID of BT Serial Adaptor
  
  private static final int RESULT_LENGTH = 6;                          // This is the number of useful result bytes we expect to receive from the sensor when we request data.
  private static final int LAST_BUFFER_INDEX = RESULT_LENGTH - 1;      // Pre-calculate the maximum index allowed in the data buffer. 
  
  private static final int READ_EVERY = 500;                          // Read the sensor every n milliseconds
  private static final int WATCHDOG_TIMEOUT = 10000;                  // Watchdog timer: If a bluetooth connection isn't established in n milliseconds, retry...
  
    
  // ** Gemeral Message Type Indicators: **
  // One of these constants will be supplied as a 'what' parameter whenever a 
  // message is sent to the parent class. Indicates what the callback is for.
  public static final int VEHICLE_DATA_UPDATED = VEHICLE_DATA_ID + 0;
  public static final int VEHICLE_DATA_ERROR   = VEHICLE_DATA_ID + 90;

  // *** Vehicle Data Message Type Indicators: ***
  // Primary Driver Data: 
  public static final int DATA_CONTACTOR_ON        = VEHICLE_DATA_ID +  1;        // Main contactor on (i.e. Inverter On!) (indicator light)
  public static final int DATA_FAULT               = VEHICLE_DATA_ID +  2;        // Fault (Warning symbol light)
  public static final int DATA_MAIN_BATTERY_KWH    = VEHICLE_DATA_ID +  3;        // Main Battery kWh remaining (fuel gauge dial)
  public static final int DATA_ACC_BATTERY_VLT     = VEHICLE_DATA_ID +  4;        // Accessory battery DC voltage (dial)
  public static final int DATA_MOTOR_RPM           = VEHICLE_DATA_ID +  5;        // Motor Rpm (dial)
  public static final int DATA_MAIN_BATTERY_TEMP   = VEHICLE_DATA_ID +  6;        // Main Battery Temperature (Bar)
  public static final int DATA_MOTOR_TEMP          = VEHICLE_DATA_ID +  7;        // Motor Temperature (Bar)
  public static final int DATA_CONTROLLER_TEMP     = VEHICLE_DATA_ID +  8;        // Controller Temperature (Bar)
  // Technical system data:
  public static final int DATA_PRECHARGE           = VEHICLE_DATA_ID +  9;        // pre-charge indicator
  public static final int DATA_MAIN_BATTERY_VLT    = VEHICLE_DATA_ID + 10;        // Main Battery Voltage
  public static final int DATA_MAIN_BATTERY_AH     = VEHICLE_DATA_ID + 11;        // Main Battery Amp hour
  public static final int DATA_AIR_TEMP            = VEHICLE_DATA_ID + 12;        // Air Temperature

  
  
  private static final byte[] READ_COMMAND = { '*' };


  
  // ---------------DEMO MODE CODE -------------------------------
  private boolean isDemo = false;  // Demo mode flag!
  private float kWh = 0f; 
  // ---------------DEMO MODE CODE -------------------------------  
  
  
  
  
  // ************** Constructor: *****************************************
  public VehicleDataBt(Handler thisHandler)
    {
    messageHandler = thisHandler;
    myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    // Build a UUID for a RfComm connection (used when connecting)
    }

  
    
  
  
  
  /********** Public Methods: *****************************************************************************/
  
  public boolean isOK()
    {  return isRunning;  }  // Basically, if we're not running, We're not OK (i.e. not yet connected). 

  public boolean isRunning()
    {  return isRunning;  }


  /********** toString Method: *************************************
   * Returns a string with a data summary (useful for debugging):
   * @return String representing class data 
   ******************************************************************/
  @Override  
  public String toString()
    {  return lastData;  }


  
  // ---------------DEMO MODE CODE -------------------------------
  public void setDemo(boolean thisIsDemo)
    {
    updateTimer.removeCallbacks(mUpdateTimerTask);   // Stops the sensor read timer.
    watchdogTimer.removeCallbacks(mWDTimerTask);     // Stops the connection watchdog timer.
    // Set the 'Demo' mode flag: 
    isDemo = thisIsDemo;
    watchdogTimer.postDelayed(mWDTimerTask, 2000);  // ...Callback in n milliseconds!    
    }
  // ---------------DEMO MODE CODE -------------------------------
  
  
  public void suspend()
    {
    // Stop the update timer if it's running:
    updateTimer.removeCallbacks(mUpdateTimerTask);   // Stops the sensor read timer.
    watchdogTimer.removeCallbacks(mWDTimerTask);     // Stops the connection watchdog timer.

    // ---------------DEMO MODE CODE -------------------------------
    isDemo = false;
    // ---------------DEMO MODE CODE -------------------------------    
    
    // Cancel the BTCommThread if it's running: 
    if (mBTCommThread != null) 
      {
      mBTCommThread.cancel();
      mBTCommThread = null;
      }
    }

  
  
  public void resume()
    {
    // Remove any existing timer callbacks: 
    updateTimer.removeCallbacks(mUpdateTimerTask);             // Clear Update Timer.    
    // *** Start or Restart the fuel sensor: ***
    // Start a new thread to setup the bluetooth, then returns (thus UI is not blocked while bluetooth starts).
    if (mBTCommThread == null)
      {
      // No BT Comm thread yet. Create one: 
      mBTCommThread = new BTCommThread();
      mBTCommThread.start();
      // Set up a watchdog timer: 
      // This will kill the connection thread if the connection is lost, forcing a retry.
      watchdogTimer.removeCallbacks(mWDTimerTask);               // Clear Watchdog Timer.      
      watchdogTimer.postDelayed(mWDTimerTask, WATCHDOG_TIMEOUT);  // ...Callback in n milliseconds!  
      }
    // Start an update timer: 
    updateTimer.postDelayed(mUpdateTimerTask, READ_EVERY);    // ...Callback in n milliseconds!
    }

  
  
  
  
  
  
  
  /************* Data Decode: ************************************
   * This method decodes a string of data received from the input stream 
   * and fills in the various data fields in vehicleData.
   * @param thisData - A string containing encoded vehicle data. 
   ***************************************************************/
  private void decodeVehicleData(String thisData)
    {
    // TO-DO: Figure out data format and write code to decode it!
    }
  
  
  

  
  /*********** Data Update Timer: ********************************************************************************/
  private Runnable mUpdateTimerTask = new Runnable() 
   {
   // Creates a Runnable which will be called after a delay, to carry out a read of vehicle data. 
   public void run()  
     {
     updateTimer.removeCallbacks(mUpdateTimerTask);                // ...Make sure there is no active callback already....
     if (mBTCommThread != null) mBTCommThread.write(READ_COMMAND); // Start a sensor read (when data come back, the UI will be informed.)
     updateTimer.postDelayed(mUpdateTimerTask, READ_EVERY);        // ...Callback in n milliseconds!
     } 
   };
   /***************************************************************************************************************/

   
   
   /*********** Watchdog Timer: ********************************************************************************/
  private Runnable mWDTimerTask = new Runnable() 
    {
    // Creates a Runnable which will be called after a delay, to check the connection status: 
    public void run()  
      {
      watchdogTimer.removeCallbacks(mWDTimerTask);                  // ...Make sure there is no active callback already
      
      // ---------------DEMO MODE CODE -------------------------------
      // Overrides normal BT operation in demo mode: 
      if (isDemo)
        {
        if (mBTCommThread != null)
          {
          mBTCommThread.cancel();
          mBTCommThread = null;
          }
        isRunning = true;  // Pretend we are running.

        /***** Generate some fake data  and send it to the UI: ***************/
        kWh = kWh + 1f;  if (kWh > 30f) kWh = 10f;
        float thisRPM = (float) ((Math.sin( (float)(System.currentTimeMillis() % 12000) / 1909  ) + 0.3) * 3000);
        boolean demoFault = false;
        if (thisRPM < -1500) demoFault = true; 
        if (thisRPM < 0) thisRPM = 0;
        boolean demoGreenGlobe = ((System.currentTimeMillis() % 300) > 100);   
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_CONTACTOR_ON,      (thisRPM > 1)   ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_FAULT,             demoFault ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_MAIN_BATTERY_KWH,  kWh ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_ACC_BATTERY_VLT,   12.6f  ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_MOTOR_RPM,         thisRPM   ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_MAIN_BATTERY_TEMP, kWh  ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_MOTOR_TEMP,        (thisRPM/4000)+20  ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_CONTROLLER_TEMP,   (thisRPM/4000)+15  ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_PRECHARGE,         demoGreenGlobe  ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_MAIN_BATTERY_VLT,  133.5f ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_MAIN_BATTERY_AH,   189.4f ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(DATA_AIR_TEMP,          19.6f  ) );
        messageHandler.sendMessage( messageHandler.obtainMessage(VEHICLE_DATA_UPDATED));
        /**********************************************************************/
       
        watchdogTimer.postDelayed(mWDTimerTask, 200);    // ....Demo update every 0.2 s.
        return;
        }
      // ---------------DEMO MODE CODE -------------------------------      
      
      if ( (mBTCommThread != null) && (!isRunning) )
        {
        // We have a BTCommThread, but it hasn't managed to connect. We should 
        // force it to retry: 
        mBTCommThread.cancel();
        mBTCommThread = null;
        }
      watchdogTimer.postDelayed(mWDTimerTask, WATCHDOG_TIMEOUT);    // ...Check again in n milliseconds!
      } 
    };
    /***************************************************************************************************************/
    

  
  
  


   
   
   
   
   
   
   
   
   
  /*********************************************************************************************************************
  *************** Bluetooth Communications: ****************************************************************************
  **********************************************************************************************************************/
  // This creates a new thread to send data to the bluetooth adaptor and listen for a response: 
  private class BTCommThread extends Thread 
    {

    private BluetoothAdapter mBluetooth;
    private BluetoothDevice  btFuelSensor;
    private BluetoothSocket  btSocket;
    private InputStream      btStreamIn;
    private OutputStream     btStreamOut;

    private byte[] byteBuffer = new byte[1];
    private byte[] dataBuffer = new byte[6];
    private int dataBufferPtr = 0;

    
    // ************** Thread Constructor: **************************************************
    public BTCommThread() 
      {
      // *** Get a reference to the bluetooth adapter: ***
      mBluetooth = BluetoothAdapter.getDefaultAdapter();
      }

 
    public void run()
      {

      /******* Bluetooth Connection: ***************************
       * This code assumes we are already paired to the Bluetooth device. 
       * Also, we connect using a hard-coded MAC address. 
       * TO DO: Add code to allow selection of a device. 
       *********************************************************/
      //messageHandler.sendMessage(messageHandler.obtainMessage(UIActivity.UI_TOAST_MESSAGE, "Looking for Vehicle Connection..."));
      // **** Get a bluetooth device for the fuel sensor: (NOTE: Hard coded address! Should be able to select...) ******
      if (mBluetooth != null) btFuelSensor = mBluetooth.getRemoteDevice("00:12:05:17:91:65");    // Should be MDFlyBTSerial bluetooth serial device.

      // Try to establish a BT Connection:  
      try
        {
        if (btSocket != null) btSocket.close();    // If there's already a BT Socket open, close it. 
        
        btSocket = btFuelSensor.createRfcommSocketToServiceRecord(myUUID);  // Create a new BT Socket for RF Comm connection. 
        btSocket.connect();                                                 // Try to connect...

        btStreamIn  = btSocket.getInputStream();   // Get input and output streams 
        btStreamOut = btSocket.getOutputStream();  // for communication through the socket. 
               
        isRunning = true;  // We got to here, so BT Comms should now be running!
        }

      catch (Exception e)
        {
        // An error occurred during BT comms setup:
        e.printStackTrace();
        // Signal the UI that there was an error (UI can choose to ignore...)
        isRunning = false;
        messageHandler.sendMessage(messageHandler.obtainMessage(VEHICLE_DATA_ERROR, e.getMessage()));
        return;  // Give up.
        }

      
      // ******* NOW: Keep listening to the InputStream while connected: *********************************      
      int bytesRead;
      while (true)
          {
          try 
            {
            // Read a byte from the InputStream:
            bytesRead = btStreamIn.read(byteBuffer,0,1);
            
            if (bytesRead > 0) 
              {
              // Data have arrived:
              if ( (byteBuffer[0] == 13) || (byteBuffer[0] == 10) ) 
                {
                // CR or Linefeed received: End of transmission from fuel computer!
                if (byteBuffer[0] == 13)
                  {
                  // Respond to CR; Ignore LF. 
                  // Send the obtained bytes to the UI Activity
                  if (dataBufferPtr == LAST_BUFFER_INDEX)
                    {
                    // Inform UI class that new values are available:
                    // NOTE: Might not be thread-safe!
                    //  We should ensure there is only one BTCommThread running at a time.
                    lastData = new String(dataBuffer);                 // Convert the data we have received into a string.                           
                    decodeVehicleData(lastData);                       // Decode the data.                                  
                    messageHandler.sendMessage(messageHandler.obtainMessage(VEHICLE_DATA_UPDATED));  // Inform the UI that we have new data.                     
                    }
                  else
                    {
                    // Incomplete Data Received: 
                    // Ignore this error... messageHandler.sendMessage(messageHandler.obtainMessage(FUEL_ERROR, "Incomplete Data!"));
                    }
                  dataBufferPtr = 0;
                  }  // [if (byteBuffer[0] == 13)]
                }  // [if ( (byteBuffer[0] == 13) || (byteBuffer[0] == 10) )]
              else
                {
                // Some other data byte received. Add to the data buffer: 
                dataBuffer[dataBufferPtr] = byteBuffer[0];
                if (dataBufferPtr < LAST_BUFFER_INDEX) dataBufferPtr++;   // Increment the buffer pointer. Note: Don't let it go outside the array!
                }
              }  // [if (bytesRead > 0)]
            }  // [try]
          
          catch (Exception e) 
            {
            // An error occurred during BT comms operation:
            // Signal the UI that there was an error (UI can choose to ignore...)
            messageHandler.sendMessage(messageHandler.obtainMessage(VEHICLE_DATA_ERROR, e.getMessage()));
            isRunning = false;
            break;
            }
          }  // [while (true)]
      }  // [run()]

 
    
    
    // ******** Write data to the BT Adaptor: *************************
    public synchronized void write(byte[] writeBuffer) 
      {
      // Reset the read buffer: 
      dataBufferPtr = 0;
      try 
        {
        if ( (btStreamOut != null) && (isRunning) ) btStreamOut.write(writeBuffer);  // Only try writing to the output stream if it's up and running!
        } 
      catch (Exception e) 
        {
        // An error occurred during BT comms operation:
        // Signal the UI that there was an error (UI can choose to ignore...)
        messageHandler.sendMessage(messageHandler.obtainMessage(VEHICLE_DATA_ERROR, e.getMessage()));
        }
      }  // [write()...]


    
    // ******** Cleanup: Called when the thread is terminated. Stops all BT activity and closes the socket: **************
    public void cancel()
      {
      // *** Stop the fuel sensor... ***
      isRunning = false;
      // Close down the input and ouptut streams and the bluetooth socket: 
      try
        {
        if (btStreamIn != null)  btStreamIn.close();
        if (btStreamOut != null) btStreamOut.close();
        if (btSocket != null)    btSocket.close();
        } 
      catch (Exception e)
        {  e.printStackTrace();  }  // If an error occurs here, print a stack trace (debug only) but otherwise quietly ignore.     
      
      } 

    
    }  // [class BTCommThread]  

  
  
  
  }  // [class VehicleDataBt]
