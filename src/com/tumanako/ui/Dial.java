package com.tumanako.ui;

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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;



/*************************************************************************************
 * 
 * Dial: Experimental! Derived from RenderedGauge. See RenderedGauge.java for attribute definitions.  
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ************************************************************************************/



public class Dial extends RenderedGauge
  {

  private boolean isFirstDraw = true; 
  
  // Calculated internal values (based on actual size of dial, and generated at runtime): 
  private float needleLength = 0f;  // Length of needle in screen coordinates

  // Runtime Data Values: 
  private float needleAngle = 0f;   // The angle of the needle used to represent the above value 
  
  private Path needlePath;

  private Paint needlePaint;
  
    
  // ********** Constructor: ***************************
  public Dial(Context context, AttributeSet atttibutes)
    {
    super(context, atttibutes);

    // Create re-usable drawing path used to draw the needle: 
    needlePath = new Path();  
  
    // --DEBUG!-- Log.i( UIActivity.APP_TAG, "  Dial -> Constructor ");
    
    // Paint for needle: 
    needlePaint = new Paint();
    needlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
    needlePaint.setStrokeWidth(3);
    needlePaint.setColor(0xA0F00000);
    needlePaint.setAntiAlias(true);
    
    }
  
  
  
  
  /******* Calculate run-time parameters for drawing scale, etc: ***************/
  private void calcDial()
    {


    // Calculate the needle length in screen coordinates: (Initially specified as a % of guage width):
    needleLength = (float)drawingWidth * fNeedleLength;   
    
    // Loop through the requested number of scale steps and calculate stuff...
    float scaleAngle = minAngle;   // Angle from origin to initial point on scale in Radians (0 = vertical up)
    float scaleAngleStep = (maxAngle - minAngle) / (float)(numberDivisions-1);   // Angle step for each scale step
    for (int n=0; n<numberDivisions; n++)
      {
      slabelX[n] = needleX(scaleAngle);
      slabelY[n] = needleY(scaleAngle);
      scaleAngle = scaleAngle + scaleAngleStep;
      }
    needleLength = needleLength * 0.9f;
    
    invalidate();    
    }

  
  
  
  /***** Set the value displayed on the dial: *******************
   * @param value Value to set the needle to
   */
  @Override
  public void setValue(float value)
    {
    super.setValue(value);
    needleAngle = minAngle + (((value - scaleMin) / deltaScale) * deltaAngle);
    invalidate();
    }
  


  
  
  
  /******* Calculate screen X coordinate: *********
   * Given a needle angle (radians), calculate the corresponding screen
   * x coordinate for a point at the end of the needle. 
   * Uses originX, originY and needleLength which should already be set
   * (see setupDial method).  
   * 
   * @param thisAngle Needle angle in radians. 0 = Vertical Up. 
   * @return x screen coordinate
   * 
   */
  private float needleX(float thisAngle)
    {
    return originX + (needleLength * FloatMath.sin(thisAngle)); 
    }

  
  
  
  /******* Calculate screen Y coordinate: *********
   * Given a needle angle (radians), calculate the corresponding screen
   * y coordinate for a point at the end of the needle. 
   * Uses originX, originY and needleLength which should already be set
   * (see setupDial method).  
   * 
   * @param thisAngle Needle angle in radians. 0 = Vertical Up. 
   * @return y screen coordinate
   * 
   */
  private float needleY(float thisAngle)
    {
    return originY - (needleLength * FloatMath.cos(thisAngle)); 
    }
  
  
  
  
  
  
  private void makeNeedle()
    {
    // Make a line to represent the needle: 
    float x = needleX(needleAngle); 
    float y = needleY(needleAngle);
    needlePath.reset();
    needlePath.moveTo(originX, originY);             // ...Start point!
    needlePath.lineTo(x,y);                          // ...End point!
    }


  
  
  @Override 
  protected void onDraw(Canvas canvas)
    {
   
    super.onDraw(canvas);
   
    // Determine the chart dimensions, if we haven't already done so: 
    if (isFirstDraw)
      {
      isFirstDraw = false;
      calcDial();
      }

    // *** Draw the needle: ****
    makeNeedle();
    canvas.drawPath(needlePath, needlePaint);

    }
  
  }  // [Class]
  

