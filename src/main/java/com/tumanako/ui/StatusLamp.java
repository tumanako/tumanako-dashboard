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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Status Lamp.
 *
 *  This class provides a simple 'Lamp' UI element. The lamp has two states
 *  defined as 'On' and 'Off', each of which is displayed with a bitmap image in the
 *  UI.
 *
 *  The states don't have to be literally On or Off - it could be any binary indicator,
 *  e.g. 'Connected' / 'Disconnected', 'On Fire' / 'Not On Fire', etc.
 *
 * To use, suitable XML should be added to the layout file, e.g.:
 * {@code
      <com.tumanako.ui.StatusLamp
               android:id="@+id/demoStatusLamp"
               android:layout_width="32dp"
               android:layout_height="32dp"
               app:on_bitmap="@drawable/lamp_on"
               app:off_bitmap="@drawable/lamp_off"
               app:initial_status="true"         />
}
 * In the above example, the lamp is a 32x32 bitmap. Bitmaps called
 * 'lamp_on.png' and 'lamp_off.png' should be located in the appropriate res\drawable
 * folder.
 *
 * Note that there must also be a values\attrs.xml file which defines the custom
 * attributes:
 *   on_bitmap      - Bitmap resource to display when lamp is 'On'
 *   off_bitmap     - Bitmap resource to display when lamp is 'Off'
 *   initial_status - Should the lamp be on or off initially? ("on" or "off").
 *
 *   It should look like this:
 * {@code
    <?xml version="1.0" encoding="utf-8"?>
     <resources>
       <declare-styleable name="StatusLamp">
          <attr name="on_bitmap" format="string" />
          <attr name="off_bitmap" format="string" />
          <attr name="initial_status" format="string" />
      </declare-styleable>
    </resources>
}
 *
 * To access the lamp from code and turn it on or off, use something like this:
 * {@code
    private StatusLamp demoStatusLamp;
    demoStatusLamp = (StatusLamp) findViewById(R.id.demoStatusLamp);
    demoStatusLamp.setStatus(true);
}
 *
 * Easy as that!
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class StatusLamp extends ImageView
{

  /**
   * true = Lamp On; false = Lamp Off.
   */
  private boolean lampState = false;

  // Bitmaps to represent lamp state (on / off):
  private Bitmap bitmapLampOn;
  private Bitmap bitmapLampOff;

  /**
   * Called when this view is created, probably from inflating
   * an XML layout file.  Context and attributes are passed on
   * to super class constructor for basic creation of the view,
   * then custom components are added.
   *
   * @param context
   * @param attrs
   */
  public StatusLamp(Context context, AttributeSet attrs)
  {
    // Call the super class constructor to create a basic ImageView
    super(context, attrs);

    // Get attributes from XML file and load bitmaps for the lamp
    getCustomAttributes(attrs);

    setLampBitmap(lampState);
  }

  /**
   * Extracts custom attributes.
   * Given a set of attributes from the XML layout file,
   * extract the custom attributes specific to the StatusLamp.
   * This method also loads the 'On' and 'Off' bitmaps for the lamp.
   * @param attrs - Attributes passed in from the XML parser
   */
  private void getCustomAttributes(AttributeSet attrs)
  {
    TypedArray a = getContext().obtainStyledAttributes( attrs, R.styleable.StatusLamp );

    // Look up the resource ID of the bitmaps named for 'off' and 'on' in the XML.
    // If they can't be found, a default is used. (probably means we made a typo in the XML...)
    int idBitmapLampOn  = a.getResourceId(R.styleable.StatusLamp_on_bitmap, R.drawable.greenglobe_on);
    int idBitmapLampOff = a.getResourceId(R.styleable.StatusLamp_off_bitmap, R.drawable.greenglobe_off);
    String initialStatus = a.getString(R.styleable.StatusLamp_initial_status);

    // Recycle the TypedArray.
    a.recycle();

    // Load the bitmaps for the lamp:
    bitmapLampOn   = BitmapFactory.decodeResource( getResources(), idBitmapLampOn  );
    bitmapLampOff  = BitmapFactory.decodeResource( getResources(), idBitmapLampOff );

    // Set initial status (note that default is Off)
    if ((initialStatus != null) && initialStatus.equals("on")) {
      // Turn on the lamp if initial_status is "on"!
      lampState = true;
    }
  }

  /**
   * Set visual lamp state.
   * @param on whether to display the on or the off bitmap
   */
  private void setLampBitmap(boolean on)
  {
    setImageBitmap(on ? bitmapLampOn : bitmapLampOff);
  }

  /**
   * Sets the current lamp state.
   * @param on <code>true</code> = Lamp is on; <code>false</code> = lamp is off.
   */
  public void setState(boolean on)
  {
    setLampBitmap(on);
    lampState = on;
  }

  /**
   * Returns the current lamp state.
   * @return <code>true</code> = Lamp is on; <code>false</code> = lamp is off.
   */
  public boolean getState()
  {
    return lampState;
  }
}
