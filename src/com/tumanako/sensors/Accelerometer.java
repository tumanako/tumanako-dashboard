package com.tumanako.sensors;

import com.tumanako.dash.RingBuffer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;


/****************************************************************
 *  
 *  Acceleration Sensor:
 *  
 *  This class uses a SensorManager to get data from the device
 *  accelerometer.
 *  
 *  Orientation Calibration: 
 *  
 *  The class can measure the device orientation and store
 *  the 'Gravity' vector, and from that it can derive unit
 *  vectors representing the axes of the vehicle. 
 *  One 'calibrated' in this way, the gravity is subtracted
 *  from the measured acceleration, and the residual 
 *  acceleration is rotated to provide acceleration
 *  along the three axes of the vehicle:
 *   x = Lateral (e.g. cornering)
 *   y = Longitudinal (e.g. accelerating / braking)
 *   z = Vertical (e.g. bumps, crests, etc). 
 *    
 *  NOTE: Orientation calibration assumes that the Y axis
 *  (i.e. Up on the screen) is parallel to the vehicle 
 *  longitudal axis. The maths can't really handle rotation
 *  about the device X and Z axes at the moment!     
 *  
 *  Average and Peak Acceleration:
 *  
 *  The class includes the use of a 'ring buffer' 
 *  (see RingBuffer in com.tumanako.dash) to maintain
 *  a 1 second rolling average of acceleration measurements. 
 *  This provides smoothing of the results. Peak acceleration
 *  is also tracked in each axis. See getSmoothedAccelerationXYZ 
 *  and getPeakAccelerationXYZ.
 *    
 *    
 *  USAGE:
 * 
 *  -Create a new instance (see Construtor for details):
 *  
 *     mAcceleration = new Accelerometer(this, mMsgHandler);
 *  
 *  -Restore a previously measured gravity vector (if available):  
 *     
 *    mAcceleration.setGravityXYZ(new float[] {gravityX,gravityY,gravityZ});
 *    
 *   If SetGravityXYZ is not used (e.g. gravity has not been previously measured),
 *   the gravity vector defaults to 'z down', i.e. device is horizontal in vehicle. 
 *        
 *  -Initialise / Start the sensor:
 *  
 *    mAcceleration.resume(); 
 *    
 *  -Check whether the sensor is running: 
 *  
 *    if (mAcceleration.isRunning()) ...
 *  
 *  -Carry out an orientation measurement (make sure vehicle is level first):
 *  
 *    mAcceleration.measureGravity();
 *    
 *    Note that this operation STARTS the calibration, then exits. The gravity 
 *    vector is averaged for approx. 3 seconds. Once finished, the gravity
 *    reference is automatically updated, and a ACCEL_GRAVITY_DONE message is 
 *    sent to the parent through the handler supplied to the constructor.
 *      
 *  -In the parent class, use a message handler to process messages, and 
 *   respond to the 'ACCEL_DATA_UPDATED' message. 'getAcceleragion...'
 *   can be used to retrieve the latest data from the Accelerometer.  
 *   
 *      @Override
 *      public void handleMessage(Message msg)  {
 *        switch (msg.what) {
 *
 *          case Accelerometer.ACCEL_DATA_UPDATED:
 *            // New Acceleration data!
 *            float[] newAccel = mAcceleration.getAccelerationXYZ();
 *            break;
 *            ...
              
 *  -IMPORTANT: When the UI activity is hidden or stopped, the sensors 
 *   must also be stopped to free resources and save battery. 
 *   The activity's onPause method should call the suspend method for the
 *   accelerometer: 
 *   
 *     @Override
 *     protected void onPause()  {
 *       super.onPause();
 *       mAcceleration.suspend();
 *       ... }
 *       
 *   resume() can be called from the Activity's onResume() method to restart
 *   the sensor when the application is restored to the foreground.   
 *  
 *  
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***************************************************************/


public class Accelerometer implements SensorEventListener,IDroidSensor
  {
  
  private Context parent;                     
     // Stores the context of the parent class which created this Accelerometer instance. 
     // Used to get a SensorManager for the main activity context.  

  private final Handler messageHandler;  // A reference to a Handler which is used to pass messages back to the UI. See Constructor.
  
  private SensorManager mSensorManager;  // Our internal SensorManager instance
  private Sensor sensorAcceleration;     // Our internal acceleration sensor
  private boolean isRunning;             // Flag to indicate whether the sensor is currently running.
  
  private float[] accelerationXYZ = new float[] {0F,0F,0F};          // Will store latest acceleration (with gravity removed once gravity has been established)
  private float[] accelerationSmoothedXYZ = new float[] {0F,0F,0F};  // Acceleration, filtered with a rolling average
  private float[] accelerationPeakXYZ = new float[] {0F,0F,0F};      // Peak acceleration (this is reset whenever it is read).
  
  private float[] gravityXYZ      = new float[] {0F,0F,0F};    // Will store a calibration measurement of gravity. 
  
  private float[] xUnitXYZ = new float[] {1F,0F,0F};          // X-Axis reference unit vector (=Vehicle Cornering).
  private float[] yUnitXYZ = new float[] {0F,1F,0F};          // Y-Axis reference unit vector (=Vehicle Acceleration).  
  private float[] zUnitXYZ = new float[] {0F,0F,1F};          // Z-Axis / Gravity reference unit vector.

  private float g = 9.81F;                        // Gravitational constant according to our sensor. (Note: Not constant - we will adjust it when we do a calibration!
  
  // Constants: 
  private static final int AVERAGE_TIME = 4000;   // Average gravity for this long (mS)
  private static final int READ_TIME    = 200;    // mS per gravity reading 
 
  private int averageCounter = 0;        // This is a counter which is used to count the number of measurements 
                                         // we have averaged when doing an average gravity measurement.

  private static final int AVERAGE_STOP_AT = AVERAGE_TIME / READ_TIME;         // Stop averaging when averageCounter equals this. 

  // ****** Moving Average: *******
  // We want an n-point moving average of the 
  // data to provide 'smoothing'. This will 
  // smooth the last 1 second of data. To do that,
  // we need an array containing the data points recorded
  // over this time. Each average calculation involves removing 
  // the oldest entry in the array and adding a new value. 
  // For efficience, we'll implement this as a ring buffer. 
  private static final int MA_WINDOW  = 1000 / READ_TIME;    // Number of points to use for moving average.

  
  // Data Buffer: (used to generate a rolling average of acceleration):   
  private RingBuffer dataAvgBuffer = new RingBuffer(MA_WINDOW,3,true);
  

  
  // ** Message Type Indicators: **
  // One of these constants will be supplied as a 'what' parameter whenever a 
  // message is sent to the parent class. Indicates what the callback is for.
  // Sensor IDs to identify individual sensor types are defined in the IDroidSensor interface.
  public static final int ACCEL_GRAVITY_DONE  = ACCEL_SENSOR_ID + 1;
  public static final int ACCEL_DATA_UPDATED  = ACCEL_SENSOR_ID + 2;
  public static final int ACCEL_ERROR         = ACCEL_SENSOR_ID + 10;
 
  
  
  
  /******** Constructor: ******************************
   * Set up new Accelerometer sensor:
   *  
   * @param thisParent - A reference to the parent UI activity (context). Used to create a SensorManager class. 
   * @param thisMessageHandler - A reference to a message handler provided by the parent UI activity. Messages are passed to this handler to indicate when new data are available, etc.
   * 
   ***************************************************/
  public Accelerometer(Context thisParent, Handler thisMessageHandler)
    {
    parent = thisParent;
    messageHandler = thisMessageHandler;
    isRunning = false;
    // ** Create a sensor manager and connect to the acceleration sensor: **
    mSensorManager     = (SensorManager) parent.getSystemService(Context.SENSOR_SERVICE);
    sensorAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
  
  
  
  
  
  /********* Public methods to access properties: *************************/
  
  /*** isOK *******************
   * True if a sensor manager was successfully created in the constructor. 
   * @return isOK 
   ****************************/
  public boolean isOK()
    {
    if (sensorAcceleration != null) return true;
    else                            return false;
    }


  /***** isRunning: ***********************
   * True if the sensor is collecting data.
   * @return isRunning
   ***************************************/
  public boolean isRunning()
    {  return isRunning;  }

  
  /***** Acceleration Data: ***********************
   * The following methods return acceleration data as an array of float. 
   * This is the residual acceleration, i.e. with gravity removed and
   * rotated so that the Y value represents acceleration along the vehicle's
   * longitudinal axis.   
   *
   *   acceleration[0] = x; 
   *   acceleration[1] = y; 
   *   acceleration[2] = z;
   * 
   ***********************************************/
  
  
  
  /****** getAcceleration: ***************
   * returns a three element array of floats representing the most recent data (see above).
   * @return Acceleration Array (float[])
   ***************************************/
  public float[] getAccelerationXYZ()
    {  return accelerationXYZ.clone();  }

  
  /****** getSmoothedAcceleration: *******
   * @return Acceleration Array (float[])
   ***************************************/
  public float[] getSmoothedAccelerationXYZ()
    {  return accelerationSmoothedXYZ.clone();  }

  
  /****** getPeakAcceleration: ***********
   * @return Acceleration Array (float[])
   ***************************************/
  public float[] getPeakAccelerationXYZ()
    {  return accelerationPeakXYZ.clone();  }


  /****** getGravity: *********************
   * This returns the gravity vetor, in 
   * Device Coordinates (i.e. y = up on screen).
   * @return Acceleration Array (float[])
   ***************************************/
  public float[] getGravityXYZ()
    {  return gravityXYZ.clone();  }
  
  
  /****** Set Gravity Reference: **************
   * This method is used to restore a previous gravity calibration
   * when the app is restarted.  
   * To use, first carry out a gravity measurement with measureGravity().
   * Once that is completed, the gravity may be retrieved with getGravityXYZ(),
   * and saved (e.g. during the 'onSaveInstanceState()' UI event). 
   * When the app is restarted, the previous gravity measurement is then 
   * restored by calling this method. 
   *   
   * @param thisGravity - Previously measured gravity vector (float [])
   *******************************************/
  public void setGravityXYZ(float[] thisGravity)
    {  
    gravityXYZ = thisGravity.clone(); 
    setGravityReference();      
    }
  
  
  /******* Reset Peak Acceleration: *************
   * This method resets the 'Peak' acceleration measurement 
   * on each axis to 0.    
   **********************************************/
  public void resetPeakAcceleration()
    {
    accelerationPeakXYZ[0] = 0F; 
    accelerationPeakXYZ[1] = 0F;
    accelerationPeakXYZ[2] = 0F;
    }
  

  
  /*************************************************
   * Suspend and Resume: 
   * These are called when the parent activity is 
   * Suspended or Resumed (respectively).
   * The effect is to stop the sensor service, 
   * so as not to waste battery. 
   *************************************************/
  public void suspend()
    {
    isRunning = false;
    mSensorManager.unregisterListener(this);  
    }
  
  public void resume()
    { 
    isRunning = true;
    mSensorManager.registerListener(this, sensorAcceleration, SensorManager.SENSOR_DELAY_NORMAL);  //SensorManager.SENSOR_DELAY_UI);
    }
     
  
  
  
  /*************** Gravity Calibration: *******************************************************************
  * This method starts the gravity measurement process, which finds a vector representing gravity in 
  * the devices current orientation. This should be done with the device mounted in a suitable orientation
  * and stationary. Once done, this 'calibration' gravity measurement can be used as the gravity vector
  * without needing to rely on the sensor-based gravity determination each time we need to compute a 
  * real-world corrected acceleration. 
  *
  * This method starts the process, which involves averaging the gravity vector for n seconds. 
  *  -Averages acceleration readings for n seconds (uses onSensorChanged() so doesn't block)
  *  -When sufficient readings have been collected, 
  *     -Computes reference unit vectors used to correct acceleration readings
  *     -Sends message to parent (see setGravityReference)
  * 
  ********************************************************************************************************/
  public void measureGravity()
    {
    // **** Set up the class to carry out an average: ****
    gravityXYZ = new float[] { 0F,0F,0F };               // Reset the cumulative average gravity
    averageCounter = 1;                                  // This will tell the onSensorChanged method that we need to start averaging.    
    // **** Activate the acceleration sensor if it isn't already: ******
    resume(); 
    }
  
  
  
  /********** toString Method: *************************************
   * Returns a string with a data summary (useful for debugging):
   * @return String representing class data 
   ******************************************************************/
  @Override
  public String toString()
    {
    String thisDump = "";
    thisDump = String.format( "A:   %5.2f  %5.2f  %5.2f \n" + 
                              "G:   %5.2f  %5.2f  %5.2f \n" +
                              "xU:  %5.2f  %5.2f  %5.2f \n" +
                              "yU:  %5.2f  %5.2f  %5.2f \n" +
                              "zU:  %5.2f  %5.2f  %5.2f \n" + 
                              "g:   %5.2f",
                              accelerationXYZ[0], accelerationXYZ[1], accelerationXYZ[2],
                              gravityXYZ[0], gravityXYZ[1], gravityXYZ[2],
                              xUnitXYZ[0], xUnitXYZ[1], xUnitXYZ[2],
                              yUnitXYZ[0], yUnitXYZ[1], yUnitXYZ[2],
                              zUnitXYZ[0], zUnitXYZ[1], zUnitXYZ[2], g );
    return thisDump;
    }
  
  
  
  /********* Set gravity reference vector: ***********/ 
  private void setGravityReference()
    {
    // This method uses the gravityXYZ vector representing gravity 
    // in device coordinates (as measured by measureGravity() ). 
    // This vector is converted into a unit vector and saved in 
    // zUnitXYZ. 
    //
    // zUnitXYZ is then rotated by -90 degrees about the device X axis
    // to give the Y-axis unit vector. 
    // 
    // This all assumes that the device is rotated some arbtrary angle about
    // its X axis, but the X axis is alligned with the vehicle X axis. 
    // Compensating for other rotations of the device relative to the vehicle 
    // is more difficult!
    //
    // Find length of gravity vector: 
    double lengthGravity =  Math.sqrt( Math.pow(gravityXYZ[0],2) + Math.pow(gravityXYZ[1],2) + Math.pow(gravityXYZ[2],2) );
    if (lengthGravity == 0)
      {
      // Gravity vector has no length; We can't find a unit vector with this! 
      xUnitXYZ = new float[] {1F,0F,0F};  //
      yUnitXYZ = new float[] {0F,1F,0F};  // Use defaults.
      zUnitXYZ = new float[] {0F,0F,1F};  // 
      return;
      }
    // Create z-axis unit vector by dividing gravity by its length:
    zUnitXYZ[0] = (float)(gravityXYZ[0] / lengthGravity);
    zUnitXYZ[1] = (float)(gravityXYZ[1] / lengthGravity);
    zUnitXYZ[2] = (float)(gravityXYZ[2] / lengthGravity);
    // Now find Y-axis unit vector: 
    yUnitXYZ[0] = zUnitXYZ[0];       // x =  xG  
    yUnitXYZ[1] = zUnitXYZ[2];       // y =  zG
    yUnitXYZ[2] = -1 * zUnitXYZ[1];  // z = -yG
    // ...and X-Axis unit vector: 
    // Actually, we assume this is same as device X-Axis at the moment, so don't need to change.
    // Should be (1,0,0).
    g = (float)lengthGravity;  // Update value of g. 
    }
  
  
  
  
  /****** Dot Product Method: *************************
   * Calculates dot product of two vectors.  
   * @param vector1 (x,y,z)
   * @param vector2 (x,y,z)
   * @return Dot Product (float)
   ***************************************************/
  private float dotProduct( float[] vector1, float[] vector2)
    {
    return (vector1[0]*vector2[0]) + (vector1[1]*vector2[1]) + (vector1[2]*vector2[2]); 
    }


  
  
  
  
  
  
  
  
  /******************************************************************************************************************
   * Methods from SensorEventListener Interface: 
   *****************************************************************************************************************/
  
  /***** On Accuracy Changed: Don't really care. *************************/
  public final void onAccuracyChanged(Sensor sensor, int accuracy) 
    {   }

  
  
  /**** On Sensor Changed: This means we have new data! **********************************/
  public final void onSensorChanged(SensorEvent event) 
    {
    // Accel. Sensor Change... Update internal values:
    if (averageCounter > 0)
      {
      // We are currently averaging gravity data for the purpose of calibration. 
      // Update the cumulative average: 
      for (int n = 0; n < 3; n++) gravityXYZ[n] = ( (event.values[n]-gravityXYZ[n]) / (float)averageCounter ) + gravityXYZ[n];
        // ...For each component (x,y,z), add a value to the comulative average (see Wikipedia for formula).
      averageCounter = averageCounter + 1;
      if (averageCounter > AVERAGE_STOP_AT)
        {
        // We've averaged readings for long enough!!
        averageCounter = 0;
        setGravityReference();              // Use the measured gravity to create reference unit vectors for gravity correction.
        // Inform the parent activity that we have finished measuring gravity:
        messageHandler.sendMessage(messageHandler.obtainMessage(ACCEL_GRAVITY_DONE));
        }
      }
    
    // Update internal values: These are corrected for device rotation about x axis, and have gravity removed.  
    accelerationXYZ[0] = dotProduct( event.values, xUnitXYZ );      // Take the dot product of the measured acceleration with
    accelerationXYZ[1] = dotProduct( event.values, yUnitXYZ );      //  the unit vectors for each vehicle axis (x,y and z/g), to 
    accelerationXYZ[2] = dotProduct( event.values, zUnitXYZ ) - g;  //  give the magnitude of the acceleration on each of these axes.
    
    // Calculate smoothed acceleration (uses 1s rolling average):
    dataAvgBuffer.AddPoint(accelerationXYZ);
    accelerationSmoothedXYZ = dataAvgBuffer.GetAverage();
    
    // Adjust peak acceleration (if required):
    for (int n=0; n<3; n++) 
      if (  Math.abs(accelerationXYZ[n]) > Math.abs(accelerationPeakXYZ[n])  ) 
        accelerationPeakXYZ[n] = accelerationXYZ[n]; 

    // Inform parent that new values are available:
    messageHandler.sendMessage(messageHandler.obtainMessage(ACCEL_DATA_UPDATED));
    
    }
  
  /******************************************************************************************************************/
  
 
  
  

  }  //  Class