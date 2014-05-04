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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Rendered Gauge - Base Class:
 * This is the basis for the rendered dial and rendered bar controls,
 * which have a lot in common!
 *
 * Uses custom attributes, which are set in the Layout XML to
 * control the appearance of the control. The following are recognised:

 *  minimum_scale    - float: Lowest scale value
 *  scale_step       - float: Size of scale step
 *  number_divisions - integer: How many divisions on the scale.* See Note.
 *  scale_tick_step  - float: Size of step for tick marks between major scale steps. Should be a division of scale_step.
 *  label_format     - string: Format string for numeric scale labels. (See String.format function) e.g. "%.1f"
 *  colours          - string: Comma-seperated list of colour values to apply to the scale / bar. See Note.
 *  gauge_label      - string: Specify a static label to show on the control.
 *  label_x          - float: x position of gauge label (as a fraction of view width)
 *  label_y          - float: y position of gauge label (as a fraction of view height)
 *
 * These attributes apply only to 'Dial' gauges derived from this class:
 *  minimum_angle    - integer: Needle angle for lowest scale value, in DEGREES; 0 = vertical up; -90 = horizontal to left, etc.
 *  maximum_angle    - integer: Needle angle for highest scale value, as above.
 *  origin_x         - float: x position of needle origin (as a fraction of view width)
 *  origin_y         - float: y position of needle origin (as a fraction of view height)
 *  needle_length    - float: Length of needle, as a fraction of VIEW WIDTH.

 * These attributes apply only to 'Bar' gauges derived from this class:
 *  orientation      - string: "vertical" or "horizontal"
 *  scale_position   - string: "left" or "right". Specifies the position of the scale relative to the bar for a bar plot. Use "left" for top if the bar is horizontal.
 *
 * NOTE:
 *  Scale Maximum:  Highest scale value is determined from (scale_step * number_divisions) +  minimum_scale
 *
 *  E.g.: minimum_scale = 10;  scale_step = 5; number_divisions = 4;
 *        Scale will be 10 - 15 - 20 - 25. (4 steps, 10 - 25 range.)
 *
 * Scale Colours (Also set the bar segment colours for bar gauges):
 *  The 'colours' attribute specifies a list of colours, as follows:
 *    "0xFFFFC0FF,0xC0FFC0FF,0xA0FFC0FF"
 *
 *  Each Hex number represents a colour (RRGGBBAA). These are applied to each Tick Step on the scale; i.e.
 *  the first colour is applied to the first division, second to the second, etc. If there are too few colours,
 *  the last colour is applied to all remaining steps.
 *
 * Note that there must also be a values\attrs.xml file which defines the custom
 * attributes.  It should look like this:
 * {@code
 <?xml version="1.0" encoding="utf-8"?>
  <resources>
   <!-- These attributes are used by the Dial and Bar controls: -->
   <declare-styleable name="RenderedGauge">
      <!-- Both: -->
      <attr name="minimum_scale"     format="float" />
      <attr name="scale_step"        format="float" />
      <attr name="number_divisions"  format="integer" />
      <attr name="scale_tick_step"   format="float" />
      <attr name="label_format"      format="string" />
      <attr name="colours"           format="string" />
      <attr name="gauge_label"       format="string" />
      <attr name="label_x"           format="float" />
      <attr name="label_y"           format="float" />
      <!-- Dial Only: -->
      <attr name="minimum_angle"     format="integer" />
      <attr name="maximum_angle"     format="integer" />
      <attr name="origin_x"          format="float" />
      <attr name="origin_y"          format="float" />
      <attr name="needle_length"     format="float" />
      <!-- Bar Only: -->
      <attr name="orientation"       format="string" />
      <attr name="scale_position"    format="string" />
    </declare-styleable>
  </resources>
}
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class RenderedGauge extends View
{

  protected int drawingWidth = 0;
  protected int drawingHeight = 0;

  // Internal constants to remember the guage attributes:
  protected float scaleMin = 0f;
  protected float scaleMax = 0f;
  protected float deltaScale = 0f;
  protected float scaleStep = 0f;
  protected int numberDivisions = 0;
  protected int numberScaleTicks = 0;
  protected float tickStep = 0f;
  protected int tickSize = 1;
  protected float fOriginX = 0f;
  protected float fOriginY = 0f;
  protected String gaugeLabel;
  protected float fLabelX = 0f;
  protected float fLabelY = 0f;
  protected int[] scaleColours;
  protected int numberColours = 0;
  protected String labelFormat;
  protected boolean showScale = true;
  protected boolean showTicks = true;
  protected boolean showGaugeLabel = true;

  // Internal constants to remember the gauge attributes (Dial Specific):
  protected float minAngle = 0f;
  protected float maxAngle = 0f;
  protected float deltaAngle = 0f;
  protected float fNeedleLength = 0f;

  // Internal constants to remember the gauge attributes (Bar Specific):
  protected boolean isVertical = true;
  protected boolean isScaleLeft = true;
  protected float fBarAcross = 0f;
  protected float fBarLong = 0f;
  protected int barSegmentGap = 1;

  // Calculated internal values (based on actual size of guage, and generated at runtime):
  protected float originX = 0f;       // Needle Origin in screen coordinates inside the canvas of the control
  protected float originY = 0f;       //
  protected float labelX = 0f;        // Dial label Origin in screen coordinates inside the canvas of the control
  protected float labelY = 0f;        //
  protected float[] slabelX;          // } Will contain the X and Y coordinates of the scale labels
  protected float[] slabelY;          // }   once they have been calculated by setupDial method.
  protected String[] scaleLabels;     //   Will contain the labels for the scale once they have been generated by setupDial.
  protected float[] tickX;            // } Will contain locations of scale tick marks once they are known.
  protected float[] tickY;            // }

  // Runtime Data Values:
  protected float gaugeValue = 0f;   // The value we are currently representing

  // Paint for scale text:
  protected Paint scalePaint = new Paint();
  // Paint for scale ticks:
  protected Paint tickPaint = new Paint();
  // Paint for gauge label:
  protected Paint guageLablelPaint = new Paint();

  protected static final int DEFAULT_BAR_COLOUR = 0xFF00C000;
  protected static final int DEFAULT_TICK_COLOUR = 0xA0F00000;

  //protected Context uiContext;

  private final ScaledFont fontScale;

  public RenderedGauge(Context context, AttributeSet atttibutes)
  {
    super(context, atttibutes);

    //uiContext = context;

    // Get Font Scale:
    fontScale = new ScaledFont(context);

    // Load custom attributes from XML:
    getCustomAttributes(atttibutes);

    // Create the arrays to store pre-calculated scale data:
    slabelX = new float[numberDivisions];
    slabelY = new float[numberDivisions];
    scaleLabels = new String[numberDivisions];
    tickX = new float[numberScaleTicks];
    tickY = new float[numberScaleTicks];

    // Default paint for scale:
    scalePaint.setColor(Color.BLACK);
    scalePaint.setTextSize(fontScale.getFontScale() * 12);
    scalePaint.setTextAlign(Paint.Align.CENTER);
    scalePaint.setTypeface(Typeface.DEFAULT_BOLD);
    scalePaint.setAntiAlias(true);

    // Default paint for scale ticks:
    tickPaint.setColor(DEFAULT_TICK_COLOUR);
    tickPaint.setAntiAlias(true);

    // Default paint for gauge label:
    guageLablelPaint.setColor(Color.BLACK);
    guageLablelPaint.setTextSize(14);
    guageLablelPaint.setTextAlign(Paint.Align.CENTER);
    guageLablelPaint.setTypeface(Typeface.DEFAULT_BOLD);
    guageLablelPaint.setAntiAlias(true);
  }


  /**
   * Extracts custom attributes.
   * Given a set of attributes from the XML layout file, extract
   * the custom attributes specific to this control.
   * Also calculates some derived values based on teh specified
   * attributes.
   *
   * @param attrs - Attributes passed in from the XML parser
   */
  private void getCustomAttributes(AttributeSet attrs)
  {
    TypedArray a = getContext().obtainStyledAttributes( attrs, R.styleable.RenderedGauge );

    // ---- Attributes common to all derived controls: -------------------------
    scaleMin         = a.getFloat(R.styleable.RenderedGauge_minimum_scale, 0f);
    scaleStep        = a.getFloat(R.styleable.RenderedGauge_scale_step , 1f);
    numberDivisions  = a.getInt(R.styleable.RenderedGauge_number_divisions,5);
    tickStep         = a.getFloat(R.styleable.RenderedGauge_scale_tick_step, 0.5f);
    numberScaleTicks = (int)((scaleStep / tickStep) * (numberDivisions-1)) + 1;
    tickSize         = (int)(a.getFloat(R.styleable.RenderedGauge_scale_tick_size,2.0f) * fontScale.getFontScale());
    fOriginX         = a.getFloat(R.styleable.RenderedGauge_origin_x , 0.5f);
    fOriginY         = a.getFloat(R.styleable.RenderedGauge_origin_y , 0.5f);
    // Gauge label and position:
    fLabelX = a.getFloat(R.styleable.RenderedGauge_label_x , 0.5f);
    fLabelY = a.getFloat(R.styleable.RenderedGauge_label_y , 0.3f);
    gaugeLabel = a.getString(R.styleable.RenderedGauge_gauge_label);
    if (gaugeLabel == null) gaugeLabel = "";
    // Scale labels - format and colours:
    labelFormat = a.getString(R.styleable.RenderedGauge_label_format);
    if (labelFormat == null) labelFormat = "%.0f";
    String sScaleColours = a.getString(R.styleable.RenderedGauge_colours);
    if (sScaleColours == null) sScaleColours = "";
    scaleColours = extractColours(sScaleColours);
    numberColours = scaleColours.length;
    // What to show:
    showScale = a.getBoolean(R.styleable.RenderedGauge_show_scale, true);
    showTicks = a.getBoolean(R.styleable.RenderedGauge_show_ticks, true);
    showGaugeLabel = a.getBoolean(R.styleable.RenderedGauge_show_gauge_Label, true);

    // ---- Attributes for the Dial: -----------------------------------
    minAngle   = ((float)a.getInt(R.styleable.RenderedGauge_minimum_angle , -90) / 180f) * (float)Math.PI;  // NOTE: Angle attribute in Degrees; But store Radians.
    maxAngle   = ((float)a.getInt(R.styleable.RenderedGauge_maximum_angle ,  90) / 180f) * (float)Math.PI;  //
    deltaAngle = maxAngle - minAngle;
    fNeedleLength = a.getFloat(R.styleable.RenderedGauge_needle_length , 0.35f);

    // ---- Attributes for the bar guage: -----------------------------------
    String gaugeOrientation = a.getString(R.styleable.RenderedGauge_orientation);
    if (gaugeOrientation == null) gaugeOrientation = "vertical";
    if (gaugeOrientation.equals("horizontal")) isVertical = false;

    String scalePosition = a.getString(R.styleable.RenderedGauge_scale_position);
    if (scalePosition == null) scalePosition = "left";
    if (scalePosition.equals("right")) isScaleLeft = false;

    fBarAcross = a.getFloat(R.styleable.RenderedGauge_bar_across , 0f);
    fBarLong   = a.getFloat(R.styleable.RenderedGauge_bar_long , 0f);
    barSegmentGap = a.getInt(R.styleable.RenderedGauge_segment_gap, 1);

    // Recycle the TypedArray:
    a.recycle();
  }



  /**
   * Extract Colours from attribute value:
   *
   * The 'colour' attribute specifies a list of colours to use for the scale
   * divisions. The attribute is a string, and should be in the following format:
   *
   *  "colour0,colour1,colour2..."
   *
   *  Each 'colourX' value is a string representation of a number which can be converted
   *  to a 32 bit integer colour value. The string must be a Hex number, e.g.:
   *
   *        FFC0D0F0
   *
   *   ...where each pair of digits represents a level (00-FF) of each channel:
   *
   *        AaRrGgBb (Aa = Alpha, Rr = Red, Gg = Green, Bb = Blue).
   *
   * This method takes the attribute string and extracts the colours. An array of
   * integers is returned.
   *
   * If:
   *  *String is null, empty or can't be interpreted as a number:
   *    -Returns an array with a single entry containing a default colour
   *
   *  *One or more string elements are found (i.e. data separated by commas):
   *    -Returns an array of integers with matching number of entries
   *
   *  *One or more of the elements in the input string can't be converted to an integer:
   *    -The invalid entry will contain the default colour in the returned string.
   *
   *
   * @param colourData - String containing colour data (e.g. "FFC0D0F0,FFCFDFFF,FFFF0000")
   * @return integer array containing the colour values.
   */
  private int[] extractColours(String colourData)
  {
    // Create a default colour array: This is an array with only one entry (the default colour):
    int[] defaultColours = { DEFAULT_BAR_COLOUR };
    // Use String.split to split the string at each "," character. Makes an array of sub-strings.
    String[] splitColours = colourData.split(",");
    if (splitColours.length > 0) {
      // Length of split array greater than 0, so we found at least one comma seperated string element:
      // Create an array to return integer colour values.
      int[] theseColours = new int[splitColours.length];  // Same number of integer entries as there are string elements.
      // Loop through the array of string elements we found, and try to extract the colour value from each:
      for (int n=0; n<splitColours.length; n++) {
        // Use a Try / Catch block, and try to interpret the colour value with "Integer.parseInt".
        // If this fails, it should throw an exception. In that case, we'll just use the default colour
        // for the corresponding colour entry:
        try {
          theseColours[n] = (int)Long.parseLong(splitColours[n],16);
        } catch (NumberFormatException e) {
          theseColours[n] = DEFAULT_BAR_COLOUR;
        }
      }
      return theseColours;
    } else {
      return defaultColours;     // No strings found... return default (one-entry array).
    }
  }

  /** Calculate run-time parameters for drawing scale, etc. */
  protected void calcGauge()
  {

    // Calculate the gauge origin in screen coordinates:
    // For dials, this is the rotation point of the needle.
    // For bars, this is the bottom left corner of the bar (vertical orientation)
    // or top left corner (horizontal orientation).
    originX = fOriginX * (float) drawingWidth;
    originY = fOriginY * (float) drawingHeight;

    // Calculate the gauge label position in screen coordinates:
    labelX = fLabelX * (float) drawingWidth;
    labelY = fLabelY * (float) drawingHeight;

    // Loop through the requested number of scale steps and calculate stuff...
    float scaleValue = scaleMin;     // Value of initial point on scale.
    for (int n=0; n<numberDivisions; n++) {
      scaleLabels[n] = String.format(labelFormat,scaleValue);
      scaleValue = scaleValue + scaleStep;
    }
    scaleMax = scaleValue - scaleStep;              // Max scale value, used later.
    deltaScale = (float)scaleMax - (float)scaleMin; // Scale range.
    if (deltaScale == 0f) deltaScale = 1f;          // Oops. ??

    setValue(scaleMin);

    // ------- Set a good font size for labels, based on screen resolution. ------------
    guageLablelPaint.setTextSize(fontScale.getFontScale() * 14.0f );

    invalidate();
  }

  /**
   * Sets the value displayed on the gauge.
   * @param value Value to set the needle to
   */
  public void setValue(float value)
  {
    // Set the new value:
    gaugeValue = value;
    // Clamp the new value to make sure it's within the scale range:
    if (gaugeValue > scaleMax) gaugeValue = scaleMax;
    if (gaugeValue < scaleMin) gaugeValue = scaleMin;
    // Invalidate the view so that it will be redrawn:
    invalidate();
  }

  /**
   * Returns the current needle value.
   * @return Needle value
   */
  public float getValue()
  {
    return gaugeValue;
  }

  /**
   * Checks a supplied width or height spec, and decide whether to override it.
   */
  private int measureThis(int measureSpec)
  {
    int result;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
    if (specMode == MeasureSpec.EXACTLY) {
      // We were told how big to be, so we'll go with that!
      result = specSize;
    } else {
      // We might have been given an indication of maximum size:
      result = 320;   // We'll try 320 pixels.
      if (specMode == MeasureSpec.AT_MOST) result = specSize;
         // Respect AT_MOST value if that was what is called for by measureSpec
    }
    return result;
  }

  /**
   * We're being told how big we should be.
   * Compute our dimensions.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    // The widthMeasureSpec and heightMeasureSpec contain width and height specifications packed into an integer.
    // We need to carefully check and decode these. We'll use a couple of helper functions to do this:
    setMeasuredDimension(measureThis(widthMeasureSpec),  measureThis(heightMeasureSpec));
  }

  @Override
  protected void onLayout (boolean changed, int left, int top, int right, int bottom)
  {
    // Layout Time: We should now have measurements for our view area, so we can calculate positions
    // and sizes for gauge elements:
    // **** Get actual layout parameters: ****
    if (changed) {
      // --DEBUG!--
      Log.i( UIActivity.APP_TAG, "  RenderedGauge -> onLayout Changed!! ");
      drawingWidth = this.getWidth();
      drawingHeight = this.getHeight();
      calcGauge();                      // Calculate various generic values applying to all gauge types.
    }
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    super.onDraw(canvas);

    // Draw the gauge label (if required):
    if (showGaugeLabel) canvas.drawText( gaugeLabel, labelX, labelY, guageLablelPaint);
      //  --DEBUG!-- canvas.drawText( String.format("%.1f", gaugeValue), labelX, labelY, guageLablelPaint);

    // Draw the scale text (if required):
    if (showScale) {
      for (int n=0; n<numberDivisions; n++) {
        canvas.drawText(scaleLabels[n], slabelX[n], slabelY[n], scalePaint);
      }
    }

    // Override this in derived classes to draw more widgets...
    //
    // Note: Derived classes must also call this method with 'super.onDraw()' to make sure
    // the View onDraw is called and that drawing width and height are calculated
    // during the first draw (see above).
  }
}
