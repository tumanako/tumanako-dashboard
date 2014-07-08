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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;



/**

 Text box with Label <p>

 Creates a simple compound control with a text box and a smaller label. <p> 
 
 To Use: Place something like this in the XML layout file, preferably inside a
 horizontal LinearLayout:  <p>
  
  <ul>
    <com.tumanako.ui.TextWithLabel 
                 android:id="@+id/demoTextWithLabel"
                 android:layout_width="fill_parent"
                 android:layout_height="wrap_content"
                 android:gravity="center_horizontal"
                 android:textSize="18pt"
                 app:textbox_text="0.0"                              
                 app:label_text="kph"
                 app:label_size="10" />
                 

 The above example creates a text box with 18pt text reading "0.0" and the label "kph".
 The label font size is set to 10 pt. <p> 
  
 Note that there must also be a values\attrs.xml file which defines the custom 
 attributes:

  <ul>
   <li>textbox_text           - Initial text for the main part of the text box
   <li>label_text             - Initial text for the label 
   <li>label_size             - Label font size
   <li>textbox_default_text   - Text to display when 'reset()' method is called. If omitted, "" is used. 
   <li>update_action          - Action string which this text box should respond to for updates. 
                                If an intent is received with this action, it should contain a string
                                value with the new contents for the text box (i.e. stringData should not 
                                be null). See DashMessages class for details.    
  
  </ul>
  
  It should look like this: <p> 

  <pre>
    <?xml version="1.0" encoding="utf-8"?>
     <resources>
      <attr name="update_action"  format="string" />

      <declare-styleable name="TextBox">
          <attr name="textbox_text"           format="string" />
          <attr name="textbox_default_text"   format="string" />
      </declare-styleable>
      
       <declare-styleable name="Label">
          <attr name="label_text" format="string" />
          <attr name="label_size" format="integer" />
      </declare-styleable>
    </resources>  
  </pre> <p>

 @author Jeremy Cole-Baker / Riverhead Technology
 
*/
public class TextWithLabel extends TextBox
  {
  private final TextView labelView;
  private int labelSize = 10; 
  
  /**** Constructor: ***************************************
   * Called when this view is created, probably from inflating
   * an XML layout file.  Context and attributes are passed on
   * to super class constructor for basic creation of the view,
   * then custom components are added. 
   *  
   * @param context
   * @param attrs
   * 
   *********************************************************/
  public TextWithLabel(Context context, AttributeSet attrs)
    {
    
    // Call the super class constructor to create a basic Textbox: 
    super(context, attrs);
    
    // Generate new TextView for the label: 
    labelView = new TextView(context, attrs);
    
    // Get custom attributes from XML file:
    getCustomAttributes(attrs);

    
    /**** Set some attributes: **********************
     * Could add more attributes?
     ************************************************/
    labelView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PT, labelSize);
    labelView.setGravity( android.view.Gravity.RIGHT );
 
    /**** Set text colour... ************************/
    //Resources res = getResources();
    //int color = res.getColor(R.color.normal_text);
    //labelView.setTextColor(color);
    //itemView.setTextColor(color);
    
    // Add the new text boxes to this layout: 
    addView(labelView);
    
    }

  
  
  
  
  /*********** Extract custom attributes: **************************
   * Given a set of attributes from the XML layout file, extract
   * the custom attributes specific to this control: 
   * @param attrs - Attributes passed in from the XML parser 
   *****************************************************************/
  private void getCustomAttributes(AttributeSet attrs)
    {
    String labelText;
    TypedArray a = getContext().obtainStyledAttributes( attrs, R.styleable.Label );
    labelText = a.getString(R.styleable.Label_label_text);
    labelSize = a.getInt(R.styleable.Label_label_size, 10);
    a.recycle();    
    
    if (labelText != null) setLabel(labelText);
    else                   setLabel("");
    
    }
  

  
  
  
  
  
  /********* Set Text: *****************
   * Sets the main text of the control.
   * @param text - Text to display.
   *************************************
  public void setText(String text)
    {  
    //itemView.setText(text);
    super.setText(text);
    }  */
  
  
  /********* Set Label: *****************
   * Sets the label of the control.
   * @param label - Label to display
   *************************************/
  public void setLabel(String label)
    {  labelView.setText(label);  }
  
  
  /********* Get Text: *****************
   * Returns the current text in the control.
   * @return Current text
   *************************************
  public String getText()
    {  return String.valueOf(itemView.getText());  }
  */
  
  /********* Get Label: *****************
   * Returns the current label in the control.
   * @return Current label
   **************************************/
  public String getLabel()
    {  return String.valueOf(labelView.getText());  }
  
  
  
  /********* Reset: ********************
   * Sets the text box to its default value.
   *************************************
  public void reset()
    {  
    if (defaultText != null) itemView.setText(defaultText);
    else                     itemView.setText("");
    }  */

  
  
  
  }  // Class
