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
import android.view.Display;
import android.view.WindowManager;

/**
 * UI Font Scaler Class.
 * This class looks up the display window size, and generates a font size
 * multiplier appropriate for the display.
 *
 * Sizing is based on fonts which look good on an HVGA phone, e.g.
 * small 480x320 display. This will give a multiplier of 1.0.
 * higher resolution displays needing larger fonts (e.g. for gauge
 * labels) will return a number > 1.0
 *
 * Calculations are carried out in the constructor. The multiplier can then be
 * requested using "getFontScale()"
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class ScaledFont
{

  private static final int REF_SIZE = 480;

  private final float myMultiplier;

  public ScaledFont(Context context)
  {

    // ------- Calculate a good font size multiplier for labels, based on screen resolution. ------------

    // First, find the window size:
    Display thisDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

    float windowWidth  = thisDisplay.getWidth();   // Width of the screen
    float windowHeight = thisDisplay.getHeight();  // Height of the Screen

    // Use the greater of the window width and height as an indication of screen size,
    // and scale font accordingly.
    // We use a 'reference' window size of 480 pixels, which corresponds to a
    // multiplier of 1.0:

    if (windowWidth > windowHeight)  myMultiplier = windowWidth / (float)REF_SIZE;
    else                             myMultiplier = windowHeight / (float)REF_SIZE;
  }

  public float getFontScale()
  {
    return myMultiplier;
  }
}
