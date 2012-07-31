package com.tumanako.dash;

import java.util.ArrayList;
import java.util.HashMap;



/***********************************************************
 * 
 * Tumanako Dashboard Data Class:
 * This class represents the collection of fields we are 
 * getting from the vehicle. 
 * 
 * It's designed to be easily extendable, so the fields are
 * stored in a HashMap. 
 * 
 * We also provide functionality return field names and field 
 * values as CSV formatted strings, so they can be stored in 
 * an output file or similar. 
 * 
 * Two types of data field can be used: 
 *  -Pre Defined: A list of fields is created by the constructor
 *                with calls to the private method "addField".
 *                For these fields, meta-data such as formatting
 *                and default value are stored internally. 
 *                This list is the 'official' list which is 
 *                returned (formatted) by the toString method. 
 *                
 *  -Additional:  Extra fields can be added with calls to 
 *                setField(...) and retrieved with getField(...).
 *                
 * In general, to change the list of fields we are using, change the 
 * constructor.    
 * 
 * THREAD SAFETY: 
 *  This class should be thread-safe, because the public methods to
 *  store and retrieve values are declared with 'synchronized'. 
 *  However, an issue may exist if an object was stored with 
 *  'setField(...)' since that would store a reference to the 
 *  object, which could still be modified outside this class.
 *  For that reason, this class should be used to store only 
 *  primitive values, ie. float, int, bool, etc. 
 * 
 * FIELD FORMATS: 
 *  Each pre-defined field has a 'Format' and a 'LogFormat'
 *  (see fieldFormats and fieldLogFormats arrays below). 
 *  
 *  Format is used for display on screen, LogFormat is 
 *  used if the field is saved to a log file (could allow 
 *  for higher precision in logged data).
 *  
 *  The formats for each fielt are a string containing a 
 *  format code suitable for use with String.format(...),
 *  e.g. "%.4f"
 *  
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ***********************************************************/

public class DashData
  {

  
  // Create some ArrayList objects to store meta data about the data fields we want to store: 
  private final ArrayList<String> fieldNames       = new ArrayList<String>();    // A list of names for the pre defined fields. 
  private final ArrayList<String> fieldFormats     = new ArrayList<String>();    // Format codes for displaying the fields (one for each). 
  private final ArrayList<String> fieldLogFormats  = new ArrayList<String>();    // Format codes for storing the fields to a log file (one for each).
  private final ArrayList<Object> fieldDefaults    = new ArrayList<Object>();    // Default values for each field (one for each).
    
  
  
  // App Data:  
  public static final String TIME                = "Time";                   // Time in Milliseconds since 1 Jan 1970
  // Primary Driver Data: 
  public static final String CONTACTOR_ON        = "ContactorOn";            // Main contactor on (i.e. Inverter On!) (indicator light)
  public static final String FAULT               = "Fault";                  // Fault (Warning symbol light)
  public static final String MAIN_BATTERY_KWH    = "MainBatterykWh";         // Main Battery kWh remaining (fuel gauge dial)
  public static final String ACC_BATTERY_VLT     = "AccBatteryVolts";        // Accessory battery DC voltage (dial)
  public static final String MOTOR_RPM           = "MotorRPM";               // Motor Rpm (dial)
  public static final String MAIN_BATTERY_TEMP   = "TMainBattery";           // Main Battery Temperature (Bar)
  public static final String MOTOR_TEMP          = "TMotor";                 // Motor Temperature (Bar)
  public static final String CONTROLLER_TEMP     = "TController";            // Controller Temperature (Bar)
  // Secondary driver data:
  public static final String DRIVE_TIME          = "DriveTime";              // Drive time (Main battery time remaining to a minimum discharge point)
  public static final String DRIVE_RANGE         = "DriveRange";             // Drive range (estimated range given current fuel and efficiency figures)
  // Technical system data:
  public static final String PRECHARGE           = "PreCharge";              // pre-charge indicator
  public static final String MAIN_BATTERY_VLT    = "MainBatteryVolts";       // Main Battery Voltage
  public static final String MAIN_BATTERY_AH     = "MainBatteryAH";          // Main Battery Amp hour
  public static final String AIR_TEMP            = "TAir";                   // Aire Temperature
  // Position Data from GPS: 
  public static final String LAT                 = "Latitude";               // Dec. Deg  } WGS 84  
  public static final String LONG                = "Longitude";              // Dec. Deg  }
  public static final String SPEED               = "Speed";                  // kph

  
  // Create a Hashmap to store a list of data items:
  private HashMap<String, Object> dataPoint = new HashMap<String, Object>();    

  
  /********* Add a data field: *****************/
  private void addField(String name, String format, String formatLog, Object defaultValue)
    {
    fieldNames.add(name);
    fieldFormats.add(format);
    fieldLogFormats.add(formatLog);
    fieldDefaults.add(defaultValue);
    }
  
  
  /******** Constructor: ************************
   */
  public DashData()
    {   

    /****** Pre-Defined Fields: *****************************
     * This is the 'Official' list of fields used by the 
     * dashboard app. Note that any Name/Value pair can 
     * be added to the data set and requested using the
     * setField and getField methods below, but if a list of 
     * field names and/or values is requested, these are the
     * ones we'll return, in the order specified. 
     *******************************************************/

    // General App Data: 
    addField("Time",             "%d",    "%d",   0     );       // Time in Milliseconds since 1 Jan 1970
    // Primary Driver Data:    
    addField("ContactorOn",      "%d",    "%d",   false );       // Main contactor on (i.e. Inverter On!) (indicator light)
    addField("Fault",            "%d",    "%d",   false );       // Fault (Warning symbol light)
    addField("MainBatterykWh",   "%.2f",  "%.2f", 0.0f  );       // Main Battery kWh remaining (fuel gauge dial)
    addField("AccBatteryVolts",  "%.2f",  "%.2f", 0.0f  );       // Accessory battery DC voltage (dial)
    addField("MotorRPM",         "%d",    "%d",   0     );       // Motor Rpm (dial)
    addField("TMainBattery",     "%.1f",  "%.1f", 0.0f  );       // Main Battery Temperature (Bar)
    addField("TMotor",           "%.1f",  "%.1f", 0.0f  );       // Motor Temperature (Bar)
    addField("TController",      "%.1f",  "%.1f", 0.0f  );       // Controller Temperature (Bar)
    // Secondary driver data: 
    addField("DriveTime",        "%.1f",  "%.1f", 0.0f  );       // Drive time (Main battery time remaining to a minimum discharge point)
    addField("DriveRange",       "%.1f",  "%.1f", 0.0f  );       // Drive range (estimated range given current fuel and efficiency figures)
    // Technical system data:
    addField("PreCharge",        "%d",    "%d",   false );       // pre-charge indicator
    addField("MainBatteryVolts", "%.2f",  "%.2f", 0.0f  );       // Main Battery Voltage
    addField("MainBatteryAH",    "%.2f",  "%.2f", 0.0f  );       // Main Battery Amp hour
    addField("TAir",             "%.1f",  "%.1f", 0.0f  );       // Air Temperature
    // Position Data from GPS: 
    addField("Latitude",         "%.5f",  "%.5f", 0.0f  );       // Dec. Deg  } WGS 84  
    addField("Longitude",        "%.5f",  "%.5f", 0.0f  );       // Dec. Deg  }
    addField("Speed",            "%.1f",  "%.1f", 0.0f  );       // kph
    
    // Call clearFields to set up data fields with default values:
    clearFields();
    
    }
  
  
  
  /********* Set a field value: ******************
   * Sets the value of a specified data field. 
   * If there's no field with this name, a new one is added.  
   * 
   * @param name - Name of the data item to add
   * @param value - Value of the data field
   * 
   **********************************************/
  public synchronized void setField(String name, Object value)
    {  dataPoint.put(name,value);  }
  

  
  
  /******** Get a specified field value: *************
   * If the field hasn't been set to any value, returns 
   * the default as specified for the field in the constructor, 
   * or 0.0 (float) if the specified fiend name is an extra field
   * (i.e. not one created in the constructor). 
   *
   * @param name - Name of the data field to look for / return
   * @return - Value of the field if it exists, or a default value.
   * 
   **************************************************/
  public synchronized Object getField(String name)
    {
    if (dataPoint.containsKey(name))  return dataPoint.get(name);
    else                              return 0.0f;
    }
  

  
  /******** Get a specified field as a String: *************
   * This method functions like getField (see above), but
   * returns the value as an appropriately-formatted string. 
   *
   * @param name - Name of the data field to look for / return
   * @return - Value of the field if it exists, or a default value.
   * 
   **************************************************/
  public synchronized String getFieldString(String name)
    {
    // Find the index of this field (if it exists) in the list of pre-defined fields: 
    int n = fieldNames.indexOf(name);
    if (n > -1)
      {
      // This is a pre-defined field!
      return String.format(  fieldFormats.get(n), dataPoint.get(name)  );       
      }
    else 
      {
      // Not a pre-defined field: 
      if (dataPoint.containsKey(name))  return dataPoint.get(name).toString();
      else                              return "0.0";
      }
    }

  
  
  
  /******** Clear Fields: *******************
   * Clears all current field data. 
   *****************************************/
  public synchronized void clearFields()
    {  
    // Clear all existing data: 
    dataPoint.clear();
    // Set all pre-defined fields to defaults: 
    for (int n = 0; n<fieldNames.size(); n++)  setField( fieldNames.get(n), fieldDefaults.get(n) );
    }
  
  
  
  
  /******* Get Field Names: *******************************
   * Returns a string containing a list of field names in CSV format.
   * @return Fieldnames (String)
   ********************************************************/
  public synchronized String getFieldNames()
    {
    StringBuffer fieldNameString = new StringBuffer();
    // Iterate through the list of fields, and add to a string buffer:    
    for (String thisName : fieldNames)
      {
      if (fieldNameString.length() > 0) fieldNameString.append(",");  // If this is not the first field name we've added, append a comma first! 
      fieldNameString.append(thisName);  
      }
    return fieldNameString.toString();
    }
  
  
  
  
  /********** toString Method: *************************************
   * Returns a string with a data summary in CSV format:
   * @return String representing the data 
   ******************************************************************/
  @Override
  public synchronized String toString()
    {
    StringBuffer valueString = new StringBuffer();
    // Iterate through the list of fields, and add formatted values to the string buffer:
    for (int n = 0; n<fieldNames.size(); n++)
      {
      if (valueString.length() > 0) valueString.append(",");  // If this is not the first field we've added, append a comma first!
      valueString.append(  String.format(  fieldLogFormats.get(n), dataPoint.get(fieldNames.get(n))  )   );  
      }
    return valueString.toString();
    }
    
  
  
  
  }  // Class
