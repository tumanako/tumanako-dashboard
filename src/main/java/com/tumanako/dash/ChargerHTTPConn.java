package com.tumanako.dash;

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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;



/***********************************************************************************************
 * HTTP Connection Class: 
 * 
 * This class handles an HTTP connection to a server, using a new thread. 
 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 **********************************************************************************************/


public class ChargerHTTPConn extends Thread implements IDashMessages
  {

  public static final String HTTP_ERROR = "com.tumanako.httpconn.error";
  
  private static final String USE_CHARSET = "UTF-8";
    
  private final String connectTo;
  private final String host;
  private final Bundle postData;
  private final Bundle cookieDataToSend;
  
  private final String responseAction; 
  private DashMessages comthreadMessages;  // We'll need to generate intents so we can interract with the rest of the app.  
 
  private final boolean followRedirects;  // If we get a 'Redirect' header back from the server, should we follow it?
  private int responseCode = 0;
  
  private volatile boolean isRun = false;     // Flag to say that the comm thread has been run (i.e. the 'Run' method has been called).
                                              // If the thread .isAlive is false, and isRun is true, it means the thread has been started 
                                              // and then finished (as opposed to not started yet). 
  
  private boolean isStop = false;            // Set to true if this is a special "STOP" HTTPConn item. 
                                             // If so, this item won't actually do anything, but isStop will return True. 

  private final String token;   // Authorisation security token
  private final String secret;  // Authorisation secret / password
  
  
  /************** Thread Constructor: **************************************************/
  public ChargerHTTPConn(WeakReference<Context> weakContext, 
                        String thisResponseAction,
                        String thisConnectTo,
                        String thisHost,
                        Bundle thisPostData, 
                        Bundle thisCookieData,
                        boolean thisFollowRedirects, 
                        String thisToken, 
                        String thisSecret ) 
    {
    // Save the specified connectTo URL and bundle of POST data. 
    // These will be used when the thread is launched.
    if (thisResponseAction.equals("STOP")) isStop = true;
    
    connectTo       = thisConnectTo;
    host            = thisHost;
    followRedirects = thisFollowRedirects;
    
    if (thisPostData != null) postData = new Bundle( thisPostData );
    else                      postData = null;
    
    if (thisCookieData != null) cookieDataToSend = new Bundle( thisCookieData );
    else                        cookieDataToSend = null;
    
    responseAction    = thisResponseAction;
    comthreadMessages = new DashMessages( (Context)weakContext.get(),this, null );
    token             = thisToken;
    secret            = thisSecret;
    }

  
  // isRun method: Can be checked by other classes to find out whether the thread has been started yet: 
  public boolean isRun()   
    {  return isRun;  }
  
 
  // isStop method: Can be checked by other classes to find out whether this is a special "STOP QUEUE" item. 
  public boolean isStop()   
    {  return isStop;  }
 
  
  
  
  /******************* Private Methods **************************************************/
  


  
  
  /********** Extract Cookies: **********************************************
   * This method extracts cookie requests from the server (sent in the HTTP headers)
   **************************************************************************/
  private Bundle getCookies(HttpURLConnection serverConn)
    {
    Bundle cookies = new Bundle();
    Map<String, List<String>> httpHeaders = serverConn.getHeaderFields();
    Set<String> headerKeys = httpHeaders.keySet();
    for (String headerKey : headerKeys)
      {
      // --DEBUG!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " CommThread -> Header: " + headerKey + "=" + serverConn.getHeaderField(headerKey) );      
      if (headerKey.equals("Set-Cookie")) 
        {                  
        String cookie = serverConn.getHeaderField(headerKey);
        cookie = cookie.substring(0, cookie.indexOf(";"));
        String cookieName = cookie.substring(0, cookie.indexOf("="));
        String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
        cookies.putString(cookieName, cookieValue);
        // --DEBUG!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " CommThread -> Set-Cookie: " + cookieName + "=" + cookieValue );           
        }
      }
    return cookies;
    }
  
  
  
  /********************* Encode Cookies: ******************************************
   * This method encodes cookie data that we wish to send back to the server. 
   * @param data Bundle of cookie data we wish to send in the request (name/value pairs)
   * @return String containing encoded cookie data: e.g. "name1=value1; name2=value2", etc. 
   ********************************************************************************/
   private String buildCookies(Bundle data)
     {
     if (data == null)
       { return "";  }
     else
       {
       Set<String> keys = data.keySet();                 // Get a list of data keys in the bundle of submitted data. 
       Iterator<String> myIterator = keys.iterator();    // This is an iterator to iterate over the list.
       String key;
       String value;
       StringBuffer cookieStrings = new StringBuffer("");
       while (myIterator.hasNext())
         {
         key = myIterator.next();
         value = data.get(key).toString();
         cookieStrings.append(key);
         cookieStrings.append("=");
         cookieStrings.append(URLEncoder.encode(value));
         if (myIterator.hasNext()) cookieStrings.append("; ");
         }
       // --DEBUG!-- Log.i(com.tumanako.ui.UIActivity.APP_TAG, " CommThread -> Cookies: " +  cookieStrings.toString());       
       return cookieStrings.toString();
       }
     }
  
  
   
   
   
  /***************** Make POST Data: **********************************************
  * Turn a bundle of key/value pairs into a string of POST data suitable for 
  * use in the HTTP POST request:
  *********************************************************************************/ 
  private String encodePostData(Bundle data)
    {
    Set<String> keys = data.keySet();                 // Get a list of data keys in the bundle of submitted data. 
    Iterator<String> myIterator = keys.iterator();    // This is an iterator to iterate over the list.
    String key;
    String value;
    StringBuffer postData = new StringBuffer("");
    while (myIterator.hasNext())
      {
      key = myIterator.next();
      value = data.get(key).toString();
      postData.append(key);
      postData.append("=");
      postData.append(value);
      if (myIterator.hasNext()) postData.append("&");
      }
    return postData.toString();
    }

  
  
  
  
  
  
  
  
  
  
  
  
  /*************************************************************************************
   ******** RUN THREAD: ****************************************************************
   *************************************************************************************/
  public void run()
    {
    isRun = true;               // Signal that we have started work! 
    int readChr = 1;
    URL serverURL;

    // if there is data to send to the server, encode it: 
    String postString = "";
    if (postData != null) postString = encodePostData(postData);
    
    // Get output data size: 
    int postDataSize = postString.length();
    String method      = "GET";
    String contentType = "";
    if (postDataSize > 0) 
      {
      method      = "POST";
      contentType = "application/x-www-form-urlencoded;charset=" + USE_CHARSET;
      }
    
    // Build Cookies String: 
    String cookie = buildCookies(cookieDataToSend);
    
    // Build authorisation headers:
    Bundle authHeaders = SolarNetworks.getAuthorisationHeaders( connectTo, method, contentType, postString, token, secret );
    
    try 
      {
      
      /******** Connect to the server: *******************************************************/
      serverURL = new URL(connectTo);
      HttpURLConnection serverConn = (HttpURLConnection) serverURL.openConnection();
      serverConn.setInstanceFollowRedirects(followRedirects);
      serverConn.setRequestProperty("Host", host);                                   // Set the Hostname header (required for HTTP 1.1).

      if (cookie.length() > 0) serverConn.setRequestProperty("Cookie", cookie);      // Set the Cookies header if required.
     
      // --- Add authorisation headers: ----
      if (authHeaders.containsKey("X-SN-Date"))
          serverConn.setRequestProperty("X-SN-Date", authHeaders.getString("X-SN-Date") );
      
      if (authHeaders.containsKey("Authorization"))
          serverConn.setRequestProperty("Authorization", authHeaders.getString("Authorization") );

      /*********** HTTP Connection Debug Info: **********************************/
      Log.i("HTTPConn", "-----------------");      
      Log.i("HTTPConn", "URL:    " + connectTo);  
      Log.i("HTTPConn", "METHOD: " + method);
      Log.i("HTTPConn", "HOST:   " + host);
      Log.i("HTTPConn", "-----------------");
      Log.i("HTTPConn", "POST Data:\n" + postString);        
      Log.i("HTTPConn", "-----------------");
      Log.i("HTTPConn", "DATE:   " + authHeaders.getString("X-SN-Date") );
      Log.i("HTTPConn", "AUTH:   " + authHeaders.getString("Authorization") );
      Log.i("HTTPConn", "COOKIE: " + cookie);
      if (authHeaders.containsKey("X-PATH"))
      Log.i("HTTPConn", "PATH:   " + authHeaders.getString("X-PATH") );
      Log.i("HTTPConn", "-----------------");          
      /**************************************************************************/

      /********************************************************************************************
      List<String> titems;
      int i;
      Log.i("HTTPConn", "-- Request Properties: -- ");
      Map<String,List<String>> requestProperties = serverConn.getRequestProperties();
      for (String tkey : requestProperties.keySet() )
        {
        titems = requestProperties.get(tkey);
        for (i=0; i<titems.size(); i++)
          {
          Log.i("HTTPConn", tkey + ":" + titems.get(i) );
          }
        }
      Log.i("HTTPConn", "-- Header Fields: -- ");
      Map<String,List<String>> headerFields = serverConn.getHeaderFields();
      for (String tkey : headerFields.keySet() )
        {
        titems = headerFields.get(tkey);
        for (i=0; i<titems.size(); i++)
          {
          Log.i("HTTPConn", tkey + ":" + titems.get(i) );
          }
        }
      ********************************************************************************************/      
      
      //--- Send HTTP POST data if there is any: -----------------
      if (postDataSize > 0)
        {
        // We have data to send: 
        Log.i("HTTPConn", "POST: HTTP Connect: ");
        serverConn.setRequestMethod("POST");
        serverConn.setRequestProperty("Accept-Charset", USE_CHARSET ); 
        serverConn.setRequestProperty("Content-Type", contentType );
        serverConn.setDoOutput(true);
        OutputStream streamOut = serverConn.getOutputStream();
        Log.i("HTTPConn", "POST: Send data: ");
        streamOut.write(postString.getBytes(USE_CHARSET));
        streamOut.close();
        Thread.sleep(0);  // Give up CPU time (allows for slow data rate).
        Log.i("HTTPConn", "--OK! ");
        }
      else
        {  
        // No POST data: This means we'll just do a basic GET request. 
        Log.i("HTTPConn", "GET: HTTP Connect: ");
        serverConn.connect();
        Log.i("HTTPConn", "--OK! ");
        }
      

      /********* Create input stream buffer: ***********************************/ 
      StringBuffer httpReceivedData = new StringBuffer("");       // Empty string buffer. We'll add data we receive by HTTP to this.
      Bundle dataBundle = new Bundle();                           // Empty data bundle. We'll put any HTML, XML or JSON data in this to return it to the parent.      

      
      /******* Check the server's response code: **********************/ 
      responseCode = serverConn.getResponseCode();  // Get the response code.
      Log.i("HTTPConn", "RESPONSE CODE: " + responseCode);

      /******* Open a stream to get the response. *******************/
      /* Note that we use getInputStream() for normal results, but getErrorStream() if there was an error. */
      InputStream streamIn;
      if (responseIsError(responseCode))
        {
        // -- Client or server error: -----------
        // Use getErrorStream() to get error details for debugging. 
        streamIn = new BufferedInputStream(serverConn.getErrorStream());
        }
     
      else
        {
        // -- Normal response: ------------------
        streamIn = new BufferedInputStream(serverConn.getInputStream());
        }
      
      /***** Read data that the server sent to us (HTTP Response): **************/
      while (readChr > -1)
        {
        //if (this.isCancelled()) return 0;
        readChr = streamIn.read();
        if (readChr > -1) httpReceivedData.append((char)readChr);
        Thread.sleep(0);  // Give up CPU time (allows for slow data rate). 
        }

      /******* See if the server sent any cookies: ***********************/ 
      Bundle requestCookies = getCookies(serverConn);
            
      /*********** HTTP Connection Debug Info: **********************************/           
      Log.i("HTTPConn", "Message:" + serverConn.getResponseMessage() );
      Log.i("HTTPConn", "Redirect To: " + serverConn.getHeaderField("Location") );
      Log.i("HTTPConn", "Response:" + httpReceivedData );
      /**************************************************************************/
      
      /*********** OLD SolarNode Login Hack: *****************************************
       * The following code decides whether this is a successful result when connecting to a 
       * SolarNode station. We receive: 
       *   Login OK:   Redirect to "add.json"
       *                       Or: "/solarreg/u/home"
 
       *   Login FAIL: Redirect to "login.do"
       *   
       * In either case, we don't want to follow the redirect, just set the status accordingly.
       * We'll use 200 for OK (HTTP OK) and 999 for login failed. 
       *  
       * "PING" data requests seem to return Status 200 (OK) and JSON data even if not logged in...
       * 
       ***************************************************************************
      if (responseCode == 302) responseCode = 200;                                         // For a start, just change redirects to "OK".  
      if ((serverConn.getHeaderField("Location") != null) && (serverConn.getHeaderField("Location").contains("login.do"))) responseCode = 999;  // Login Failed! Change to our own error. 
      ***************************************************************************/
             
      streamIn.close();
      serverConn.disconnect();
      serverConn = null;
      // Add any cookies from the request to any data we have: 
      dataBundle.putBundle("Cookies", requestCookies);
      // Add the HTTP Response code to the data: 
      dataBundle.putInt("ResponseCode", responseCode);
      Log.i("HTTPConn", "Sending Intent with Response. Action=" + responseAction );      
      // Send a message with HTTP data in the string field, and dataBundle in the data field (contains cookies and response code):
      if (responseIsError(responseCode))
        comthreadMessages.sendData(HTTP_ERROR, null,  null, httpReceivedData.toString(), dataBundle);
      else
        comthreadMessages.sendData(responseAction, null,  null, httpReceivedData.toString(), dataBundle);
      }   // try...
    
    catch (Exception e)
      {
      e.printStackTrace();
      Log.i("HTTPConn", "ERROR: " + e.getMessage() + ";\n" + e.toString() + ";\n" + e.toString() );
      comthreadMessages.sendData(HTTP_ERROR, null,  null, e.getMessage() , null);
      // --DEBUG!!--Log.i("HTTPConn", "HTTP Error!");
      }
    
    }  // [run()]


  

private boolean responseIsError(int code)
  {
  // Check an HTTP response code to see if it represents an error. For now, we just use ">399". 
  // NB: "Success" code is 200; 
  //        400-499 = Client error (i.e. invalid request); 
  //        500-599 = Server error.
  //        300-399 = Redirection: Handling this is a grey area. If "FollowRedirects" is specified in the 
  //                              constructor, the HTTPConn class should redirect automatically and we 
  //                              won't see this response anyway. 
  if (code > 399) return true;
  else            return false;
  }
 
  
  
  
 public void messageReceived(String action, Integer intData, Float floatData, String stringData, Bundle bundleData)
   {  } 
    // We don't respond to any incomming messages. 

 
  }  // Class


       

