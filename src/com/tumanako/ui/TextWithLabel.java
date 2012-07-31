package com.tumanako.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;



/*************************************************************************************
 * 
 * Text and Label:
 * 
 * Creates a simple compound control with a text box and a smaller label. 
 *
 * To Use: Place something like this in the XML layout file, preferably inside a
 * horizontal LinearLayout: 
 * 
    <com.tumanako.ui.TextWithLabel 
                 android:id="@+id/demoTextWithLabel"
                 android:layout_width="fill_parent"
                 android:layout_height="wrap_content"
                 android:gravity="center_horizontal"
                 android:textSize="18pt"             
                 android:layout_weight="0" />

 *
 * To access from code, use something like this: 
 * 
 *    private TextWithLabel  demoTextWithLabel;
 *    demoTextWithLabel = (TextWithLabel) findViewById(R.id.demoTextWithLabel);
 *    demoTextWithLabel.setText("Hello World!");
 *    
 * Other public methods:
 *   setText(String)
 *   setLabel(String)
 *   String getText()
 *   String getLabel()
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ************************************************************************************/




public class TextWithLabel extends LinearLayout
  {
 
  private final TextView itemView;
  private final TextView labelView;
  
  
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
    // Call the super class constructor to create a basic layout: 
    super(context, attrs);
    
    // Generate new TextViews for the the text and label: 
    itemView = new TextView(context, attrs);
    labelView = new TextView(context, attrs);
    
    /**** Set some attributes: **********************
     * Right now, we just use whatever parameters were specified in the
     * XML, except we set the font size of the label to 10pt and add padding. 
     * TO DO: recognise more attributes... e.g., add custom attributes
     * so that text alignment of label can be specified? 
     * Scale label text size to a proportion of main text size?  
     ************************************************/
    labelView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PT, 10);
    labelView.setGravity( android.view.Gravity.RIGHT );
    //labelView.setPadding(10,0,10,0);
 
    // Set default text: 
    setText("");
    setLabel("");
 
    // Add the new text boxes to this layout: 
    addView(itemView);
    addView(labelView);
    
    }

  
  
  
  
  /********* Set Text: *****************
   * Sets the main text of the control.
   * @param text - Text to display.
   *************************************/
  public void setText(String text)
    {  itemView.setText(text);  }
  
  
  /********* Set Label: *****************
   * Sets the label of the control.
   * @param label - Label to display
   *************************************/
  public void setLabel(String label)
    {  labelView.setText(label);  }
  
  
  /********* Get Text: *****************
   * Returns the current text in the control.
   * @return Current text
   *************************************/
  public String getText()
    {  return String.valueOf(itemView.getText());  }
  
  
  /********* Get Label: *****************
   * Returns the current label in the control.
   * @return Current label
   **************************************/
  public String getLabel()
    {  return String.valueOf(labelView.getText());  }
  
  
  
  }  // Class
