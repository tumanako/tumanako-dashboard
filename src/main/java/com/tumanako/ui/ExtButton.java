package com.tumanako.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

import com.tumanako.dash.DashMessages;
import com.tumanako.dash.IDashMessages;



/************************************************************************************

Tumanako - Electric Vehicle and Motor control software <p>

Copyright (C) 2014 Jeremy Cole-Baker <jeremy@rhtech.co.nz> <p>

This file is part of Tumanako Dashboard. <p>

Tumanako is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. <p>

Tumanako is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details. <p>

You should have received a copy of the GNU Lesser General Public License
along with Tumanako.  If not, see <http://www.gnu.org/licenses/>. <p>

@author Jeremy Cole-Baker / Riverhead Technology <jeremy@rhtech.co.nz> <p>

*************************************************************************************/





/**
 Extended Button Class <p>
 
 This class creates a UI button and is basically a wrapper for the standard "Button"
 widget. It implements the IDashMessages interface so that it can send intent messages,
 and allows a custom attribute to specify the intent action to use when the user
 presses the button (onClick event).  <p>
  
 
 To Use: Place something like this in the XML layout file: <p>
 
  <pre>
    <com.tumanako.ui.ExtButton
            android:id="@+id/buttonMyButton"
            android:layout_width="0dp"
            android:layout_height="fill_parent"
            android:layout_weight="2"
            android:text="My Button"
            app:click_action="CLICK_MYBUTTON"   />
  </pre> <p>
                  
 The above example creates a button with the label "My Button". When the user clicks
 the button, an intent is sent with the action "CLICK_MYBUTTON". Other parts of the 
 app can be set to listen for this intent.  <p>
 
 Note that there must also be a values\attrs.xml file which defines the custom 
 attributes: <p>

  <ul>
   <li>click_action      - Action string for the intent which this button send when clicked.  
  </ul> <p>                  

   It should look like this: <p> 

  <pre>
    <?xml version="1.0" encoding="utf-8"?>
     <resources>
     
        <declare-styleable name="App">  
           <attr name="click_action"  format="string" />
        </declare-styleable>
                     
    </resources>  
  <pre> <p>
 
  
 @author Jeremy Cole-Baker / Riverhead Technology

***************************************************************************************/

public class ExtButton extends Button implements OnClickListener, IDashMessages
  {

  private String clickAction;          // Intent action to send when button is clicked. Read from custom attributes (see below). 
 
  private DashMessages dashMessages;   // Dash Messages class - allows us to send intents
  

  
  /**
   Constructor - Extended Button
   
   @param context
   @param attrs
   
   */
  public ExtButton(Context context, AttributeSet attrs)
    {
    super(context, attrs);
   
    // Get custom attributes from XML file:
    getCustomAttributes(attrs);
    
    // Set up a DashMessages class to send intents:
    dashMessages = new DashMessages( context, this, null );

    // Connect our 'onClick' listner to our button: 
    this.setOnClickListener(this);
    
    }

  
  
 
  
  
  
  


  
  /**
   Extract custom attributes from XML layout <p>
   
   Given a set of attributes from the XML layout file, extract
   the custom attributes specific to this control.
    
    @param attrs  Attributes passed in from the XML parser
     
  */
  private void getCustomAttributes(AttributeSet attrs)
    { 
    TypedArray a = getContext().obtainStyledAttributes( attrs, R.styleable.App);
    clickAction = a.getString(R.styleable.App_click_action);
    a.recycle();    // Recycle the TypedArray.
    }

  
  

  /**
   Click Event Handler
   
   @param v Reference to the view which received the click.
   
   */
  public void onClick(View v)
    {
    // If a click_action has been specified in the custom attributes, send 
    // an intent with the specified action: 
    if (null != clickAction)
      {
      dashMessages.sendData(clickAction, null, null, null, null);
      }
    }

  
  
  
  
  
  
  /**
   Intent Message Handler. Not used. 
   */
  public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
    {
    // Unused... we don't process any incomming intents. 
    }








  
  
  }  // Class
