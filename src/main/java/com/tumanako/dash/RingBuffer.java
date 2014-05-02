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


/************************************************************
 *
 * Float Ring Buffer Class:
 *
 * Create a ring buffer to store some numeric values of type 'float':
 * This will make an array of the values, and track the last value
 * updated using an array index pointer. The buffer pointer will
 * loop around the buffer, thereby overwriting the oldest value when
 * a new value is added.
 *
 * The class also includes capability to maintain a rolling average
 * of values in the buffer.
 *
 * Note that each data point in the buffer contains an array of values.
 * The buffer size and number of fields is set by the constructor.
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ************************************************************/

public class RingBuffer
  {

  private final int bufferSize;              // Number of data points in buffer.
  private final int bufferFieldCount;        // Number of values to store.
  private final boolean useAverage;          // Should we keep an average as data points are added?

  private final float[][] dataBuffer;  // Data buffer
  private float[] dataAverage;         // Average of the records in the data buffer

  private int dataPointer = 0;         // Index of current write point in buffer (increments AFTER data write).
  private int dataLength  = 0;         // The number of records in the buffer.

  /**************************************************************************************

   Note that dataLength is the number of records actually in the buffer, required because
   the buffer may not be full. dataLength starts at 0, and increments whenever a record
   is added.

   When the end of the buffer is reached, dataLength will equal bufferSize. At this point,
   the next call to AddPoint will overwrite the first element in the buffer. dataLength
   will remain equal to bufferSize. bufferSize and dataLength can be checked using
   'Get' methods (see below).

  **************************************************************************************/




  /*********** Class Constructor: **********************************************************
   *
   * @param thisBufferSize - Buffer size. After this many entries have been added, oldest entries are overwritten.
   * @param thisFieldCount - Number of fields to store for each entry. Each entry will be an array of type float of this size.
   * @param thisUseAverage - Should a running averave of the buffer contents be maintained?
   *
   *  This constructor sets up the buffer by creating an array of the appropriate size.
   *
   ****************************************************************************************/
  public RingBuffer(int thisBufferSize, int thisFieldCount, boolean thisUseAverage)
    {
    bufferSize       = thisBufferSize;
    bufferFieldCount = thisFieldCount;
    useAverage       = thisUseAverage;
    dataBuffer  = new float[bufferSize][bufferFieldCount];   // Creata a new data buffer.
    dataAverage = new float[bufferFieldCount];               // Create an array to track the average of field values.
    Clear();
    }



  /********** Get Buffer Size: **********
   * @return Buffer Size
   *************************************/
  public int GetSize()
    {  return bufferSize;  }


  /***** Get Actual number of entries used: **********
   * @return Number of buffer entries used
   **************************************************/
  public int GetLength()
    {  return dataLength;  }


  /***** Get number fields: *************************
   * @return Number of fields in each buffer entry
   **************************************************/
  public int GetFieldCount()
    {  return bufferFieldCount;  }


  /********** Reset the buffer: ********************
   * This method replaces the buffer content with 0s.
   ************************************************/
  public void Clear()
    {
    // Resets data buffer pointers back to start of buffer.
    dataPointer = 0;
    dataLength  = 0;
    // Reset averages array:
    int n;
    for (n=0; n<bufferFieldCount; n++) dataAverage[n] = 0f;
    }



  /******** Pre Fill the buffer: **************************
   * Pre-fill the buffer with the specified array of values, as if
   * AddPoint had been called repetedly with those values until
   * the buffer was full.
   *
   * @param theseValues - Values to use when filling the buffer.
   *
   */
  public void PreFill( float[] theseValues )
    {
    int n;
    for (n=0; n<bufferSize; n++) dataBuffer[n] = theseValues;
    dataAverage = theseValues;
    dataPointer = 0;
    dataLength  = bufferSize;
    }



  /******* Add a datum: ***************************************
   *  This adds a datum to the buffer, which causes several things to happen:
   *   * Oldest datum is overwritten with the new values
   *   * Buffer pointer is advanced
   *   * Rolling average is updated (if used).
   *
   * @param theseValues - The array of fields (float values) to be added
   *
   ***********************************************************/
  public void AddPoint( float[] theseValues )
    {
    // Add a point to the buffer:
    if (useAverage)
      {
      // We are maintaining an average, so we'd better update it:
      // The average is updated by deleting a portion of the oldest value, and
      // adding a portion of the new value.
      // See: http://en.wikipedia.org/wiki/Rolling_average
      // Note that there is a special case if we haven't filled the buffer yet.
      // In this case, it's a cumulative average of all values received so far.
      int n;
      for (n=0; n<bufferFieldCount; n++)
        {
        if (dataLength == bufferSize)
          {
          // The buffer is full, so update the rolling average:
          // Note that dataPointer is currently pointintg to the OLDEST value
          // in the buffer, i.e. the one we are about to overwrite.
          dataAverage[n] = dataAverage[n]
                           - (dataBuffer[dataPointer][n] / dataLength)
                           + (theseValues[n] / dataLength);
          }
        else
          {
          // Special case: Buffer not yet full. Use cumulative average instead:
          dataAverage[n] = dataAverage[n] + ( (theseValues[n]-dataAverage[n]) / (dataLength+1) );
          }
        }
      }  // [if (useAverage)]
    // Insert an array of values at the buffer data pointer:
    dataBuffer[dataPointer] = theseValues.clone();
    dataPointer = dataPointer + 1;
    if (dataPointer >= bufferSize) dataPointer = 0;            // Max number of points reached; Wrap.
    if (dataLength < bufferSize) dataLength = dataLength + 1;  // Increment the number of records.
    }  // method




  /****** Get back a datum: ******************************************************
   * Gets back an arbitrary entry from the buffer. See notes below.
   *
   * @param pointIndex - Index of point to retrieve. 0 = most recent; 1 = next most recent, etc.
   * @return Array of float values representing the fields from the requested buffer entry.
   *
   ******************************************************************************/
  public float[] GetPoint(int pointIndex)
    {
    // Retrieves a specific point. pointIndex is a number indicating
    // the point to get, such that:
    //
    //  0 = Most recent;
    //  1 = 2nd most recent;
    //  2 = 3rd most recent; Etc.
    //
    // If n exceeds the length of the buffer, the oldest value is returned.
    // NOTE that if less than the maximum number of records have been entererd
    // into the buffer, the length of the buffer will be less than the maximum size.
    // In this case, n will only go back to the earliest record, no further.
    // In other words, if only 2 records have actually been placed in the buffer,
    // values of n greater than 1 will still return the 1'th entry.
    //
    // If no data have been added, the method returns null!!
    // User should check GetLength if there is any doubt!.
    //
    int tempIndex = pointIndex;         // Local copy of the requested index (so we can check bounds and change if required)
    if (tempIndex < 0) tempIndex = 0;                             // Data index must be 0 or positive.
    if (tempIndex > (dataLength-1)) tempIndex = (dataLength-1);   // Data index must be less than the available number of records!
    int thisPointer = dataPointer - (pointIndex + 1);             // Temporary data pointer.
    if (thisPointer < 0) thisPointer = thisPointer + bufferSize;  // Buffer loops around. Add bufferSize to get to correct position.
    return dataBuffer[thisPointer].clone();                       // Return a copy of the requested data point.
    }



  /**** Get Buffer Average Method: ******************************************
   * This method returns an array of numbers which is the average of the current buffer contents.
   * Note that the average is actually updated in AddPoint whenever a point is added to the buffer.
   *
   * The average is only available if thisUseAverage was set to True in the constructor call.
   * If thisUseAverage was false, this method will always return an array of 0 values.
   * Note that the average is always a Float even if the buffer is storing Integers.
   *
   * @return Array containing average of each field across current buffer contents.
   *
   **************************************************************************/
  public float[] GetAverage()
    {  return dataAverage.clone();  }


  }  // Class
