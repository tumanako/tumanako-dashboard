package com.tumanako.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;

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



/*************************************************************************************
 * 
 * Bar Gauge: Experimental! Derived from RenderedGauge. See RenderedGauge.java for attribute definitions.  
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ************************************************************************************/

public class BarGauge extends RenderedGauge
  {

  private boolean measurementsValid = false;   // Set to true once we have measurments (after onLayout)
  
  // Calculated internal values (based on actual size of dial, and generated at runtime): 
  private int barLong = 0;
  private int barAcross = 0;
  private int numberBlocks = 0;

  private float blockValue = 1f;    // Will be the gauge value represented by one complete block.  
  private int blockLong = 1;        // Will be the pixel length of a single block on the bar. 
  
  private int lastDrawnBlock = -1;  // Index of last complete block on the scale.
  private Rect lastBlockRect;       // Will be a Rect used to draw the last bare block (smaller than a full block).
  
  private Paint barPaint;
  private Rect[] barRects;
  
      
  // ********** Constructor: ***************************
  public BarGauge(Context context, AttributeSet atttibutes)
    {
    super(context, atttibutes);

    // --DEBUG!-- Log.i( UIActivity.APP_TAG, "  Dial -> Constructor ");
    
    numberBlocks = numberScaleTicks-1;     
    
    // Array of rectangels for drawing bar: 
    barRects = new Rect[numberScaleTicks];  // Will be filled with actual coordinates during calcBar(). 
    
    // Paint for Bar:
    barPaint = new Paint();
    barPaint.setStyle(Paint.Style.FILL);
    barPaint.setStrokeWidth(0);
    barPaint.setColor(DEFAULT_BAR_COLOUR);  // Default colour.
    
    }
  
  
  
  
  /******* Calculate run-time parameters for drawing scale, etc: ***************/
  private void calcBar()
    {

    // NOTE: 
    // drawingWidth and drawingHeight are absolute, irrespective of the bar orientation. 
    // barLong is the 'length' of the bar between lowest and highest scale points; 
    // barAcross is the 'width' of the bar (which is actually height if it's a horizontal bar).
    //
    // So if it's a vertical bar, then barLong is calculated based on drawingHeight, while
    // for a horizontal bar, barLong is calculated based on drawingWidth. The opposite rule 
    // applies for barAcross.
    //
    blockValue = tickStep;

    int blockX = (int) originX;  // blockX and blockY used below to generate list of rectangles  
    int blockY = (int) originY;  // representing blocks on the bar. Start at Origin x/Origin y.
    float scaleLong;
    
    if (isVertical)
      {
      // Set up a VERTICAL bar gauge: 
      scalePaint.setTextAlign(Paint.Align.RIGHT);
      guageLablelPaint.setTextAlign(Paint.Align.CENTER);
      // Calculate the dimensions of the bar:
      barAcross = (int)(fBarAcross * drawingWidth);
      barLong = (int)(fBarLong * drawingHeight);
      blockLong = (int) (barLong / (numberScaleTicks-1));
      
      // Loop through the requested number of tick steps and calculate bar blocks...
      for (int n=0; n<numberScaleTicks; n++)
        {
        barRects[n] = new Rect(blockX,(blockY-blockLong)+barSegmentGap,(blockX+barAcross),blockY);
        blockY -= blockLong;
        }  // [for ...]

      // Loop through the requested number of scale steps and calculate scale label positions...
      blockX = (int) originX;
      blockY = (int) originY;
      scaleLong = (int) (barLong / (numberDivisions-1));
      for (int n=0; n<numberDivisions; n++)
        {
        slabelX[n] = blockX - 3;
        slabelY[n] = blockY + 5;
        blockY -= scaleLong;
        }  // [for ...]      
      
      }
    
    else  // [if (isVertical)]
      {
      // Set up a HORIZONTAL bar gauge:
      scalePaint.setTextAlign(Paint.Align.CENTER); 
      guageLablelPaint.setTextAlign(Paint.Align.RIGHT);
      // Calculate the dimensions of the bar:
      barAcross = (int)(fBarAcross * drawingHeight);
      barLong = (int)(fBarLong * drawingWidth);
      blockLong = (int) (barLong / (numberScaleTicks-1));
      
      // Loop through the requested number of tick steps and calculate bar blocks...      
      for (int n=0; n<numberScaleTicks; n++)
        {
        barRects[n] = new Rect(blockX,(blockY-barAcross),(blockX+blockLong)-barSegmentGap,blockY);
        blockX += blockLong;
        }  // [for ...]      

      // Loop through the requested number of scale steps and calculate scale label positions...
      blockX = (int) originX;
      blockY = (int) originY;
      scaleLong = (int) (barLong / (numberDivisions-1));
      for (int n=0; n<numberDivisions; n++)
        {
        slabelX[n] = blockX;
        slabelY[n] = (blockY-barAcross-3);
        blockX += scaleLong;      
        }  // [for ...]

      }  // [if (isVertical)]
    invalidate();    
    }

  
  
  
  /***** Set the value displayed on the bar: *******************
   * @param value Value to set 
   *************************************************************/
  @Override
  public void setValue(float value)
    {
    // First, call the 'RenderedGauge' version (sets value and clamps to scale).
    super.setValue(value);  

    // Figure out how many 'blocks' to draw on the bar. 
    // We'll draw all the complete blocks, then add another 
    // reduced-size block for the last part of the scale:
    // Note... we can't do this until after 'calcBar()' has been called, which sets up many parameters. 
    if (measurementsValid)
      {
      lastDrawnBlock = ( (int) ((gaugeValue - scaleMin) / blockValue) ) - 1;
      if (lastDrawnBlock < -1) lastDrawnBlock = -1;  //  ?? Shouldn't happen. 
    
      if (lastDrawnBlock < (numberBlocks-1))
        {
        // Haven't filled the last block... add a smaller one to finish off bar:
        int n = lastDrawnBlock + 1;
        float lastBlockLong = ( (  (gaugeValue - scaleMin - ((float)(lastDrawnBlock + 1) * blockValue))  ) / blockValue )  * blockLong;
        if (isVertical)
          {
          // VERTICAL bar: 
          lastBlockRect = new Rect( barRects[n].left,
                                    barRects[n].bottom - (int)lastBlockLong,
                                    barRects[n].right,
                                    barRects[n].bottom);
          }  // [if (isVertical)]
        else
          {
          // HORIZONTAL bar:
          lastBlockRect = new Rect( barRects[n].left,
                                    barRects[n].top,
                                    barRects[n].left + (int)lastBlockLong,
                                    barRects[n].bottom);          
          }  // [if (isVertical)]
        }  // [if (lastDrawnBlock < (numberBlocks-1))]
      else
        {
        if ( lastDrawnBlock > (numberBlocks-1) ) lastDrawnBlock = (numberBlocks-1);  // ??  Shouldn't happen.  
        lastBlockRect = null;  // No last block required.    
        }  // [if (lastDrawnBlock < (numberBlocks-1))]
      }
    else  // [if (!isFirstDraw)]
      {
      // Not set up yet. Set defaults. 
      lastDrawnBlock = -1;
      lastBlockRect = new Rect(0,0,0,0);
      }  // [if (!isFirstDraw)]
    }  // [function]
  
  
  

  @Override
  protected void onLayout (boolean changed, int left, int top, int right, int bottom)
    {
    super.onLayout(changed, left, top, right, bottom);
    // Layout Time: We should now have measurements for our view area, so we can calculate positions 
    // and sizes for dial elements: 
    // **** Get actual layout parameters: ****
    if (changed)
      {
      // -- DEBUG!! -- Log.i( UIActivity.APP_TAG, "  BarGauge -> onLayout Changed!! ");      
      calcBar();
      measurementsValid = true;    
      }
    }

  
  
  

  @Override 
  protected void onDraw(Canvas canvas)
    {
    super.onDraw(canvas);

    for (int n = 0; n <= lastDrawnBlock; n++)
      {
      if (n < numberColours) barPaint.setColor(scaleColours[n]);
      else                   barPaint.setColor(DEFAULT_BAR_COLOUR);
      canvas.drawRect( barRects[n], barPaint);
      }
    
    
    if (lastBlockRect != null)
      {
      int lastBlockColourIndex = lastDrawnBlock + 1; 
      if (lastBlockColourIndex < numberColours) barPaint.setColor(scaleColours[lastBlockColourIndex]);
      else                                      barPaint.setColor(DEFAULT_BAR_COLOUR);
      canvas.drawRect( lastBlockRect, barPaint);
      }

    super.onDraw(canvas);

    }
  
  
  
  
  
  
  }  // [Class]
  