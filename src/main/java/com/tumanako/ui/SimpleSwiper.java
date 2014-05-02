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
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

/**
 * Swipe Gesture Detector:
 * -----------------------
 *
 * Detect swipe left and right gestures. This is done as an extension
 * of the SimpleOnGestureListener class. We'll override the 'onFling'
 * call, which should get called when the OS detects a 'fling'
 * movement on the touch-screen.
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class SimpleSwiper extends SimpleOnGestureListener
{

  private static final int SWIPE_MIN_DISTANCE = 120;
  private static final int SWIPE_MAX_OFF_PATH = 250;
  private static final int SWIPE_THRESHOLD_VELOCITY = 200;

  private final Context uiContext;

  public SimpleSwiper(Context context)
  {
    uiContext = context;
  }

  @Override
  public boolean onDown(MotionEvent e)
  {
    return true;
  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
  {
    try {
      // Check for 'off path' swipe: the fling wasn't close enough to horizontal.
      if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
              return false;

      // *** Swipe Filtering: ***
      // Detects a Left or Right swipe if the 'fling' was long enough and
      // the velocity high enough.
      if      (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
        ((UIActivity)uiContext).nextScreen();
      } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
        ((UIActivity)uiContext).prevScreen();
      }
    } catch (Exception e) {
      // Don't worry about exceptions.
    }

    return false;
  }
}