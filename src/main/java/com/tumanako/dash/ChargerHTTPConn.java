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

package com.tumanako.dash;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.tumanako.ui.UIActivity;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HTTP Connection Class:
 *
 * This class handles an HTTP connection to a server, using a new thread.
 *
 * @author Jeremy Cole-Baker / Riverhead Technology
 */
public class ChargerHTTPConn extends Thread implements IDashMessages
{

  /* Intent Filter and Message ID constants: ***********************************/
  /** An Intent with this Action is generated by the UI to send messages to this class. */
  public static final String HTTPCONN_INTENT_IN  = "com.tumanako.dash.httpconnection";

  //public static final int HTTP_DATA    = CHARGE_HTTPCON_ID + 1;
  public static final int CONN_ERROR   = CHARGE_HTTPCON_ID + 99;

  private static final String USE_CHARSET = "UTF-8";

  private final String connectTo;
  private final Bundle postData;
  private final Bundle cookieDataToSend;

  private final String responseIntent;
  /** We'll need to generate intents so we can interact with the rest of the application. */
  private final DashMessages comthreadMessages;

  /** If we get a 'Redirect' header back from the server, should we follow it? */
  private final boolean followRedirects;
  //
  private int responseCode = 0;
  private final int responseMessage;

  /**
   * Flag to say that the com thread has been run (i.e. the 'Run' method has been called).
   * If the thread .isAlive is false, and isRun is true, it means the thread has been started
   * and then finished (as opposed to not started yet).
   */
  private volatile boolean isRun = false;

  /************** Thread Constructor: **************************************************/
  public ChargerHTTPConn(WeakReference<Context> weakContext,
                        String thisResponseIntent,
                        String thisConnectTo,
                        Bundle thisPostData,
                        Bundle thisCookieData,
                        boolean thisFollowRedirects,
                        int thisResponseMessage)
  {
    // Save the specified connectTo URL and bundle of POST data.
    // These will be used when the thread is launched.
    connectTo = thisConnectTo;
    followRedirects = thisFollowRedirects;
    responseMessage = thisResponseMessage;
    if (thisPostData != null) {
      postData = new Bundle( thisPostData );
    } else {
      postData = null;
    }
    if (thisCookieData != null) {
      cookieDataToSend = new Bundle( thisCookieData );
    } else {
      cookieDataToSend = null;
    }
    responseIntent = thisResponseIntent;
    comthreadMessages = new DashMessages((Context)weakContext.get(), this, HTTPCONN_INTENT_IN);
  }

  /**
   * Can be checked by other classes to find out whether the thread has been started yet
   */
  public boolean isRun()
  {
    return isRun;
  }

  /**
   * Extracts cookie requests from the server (sent in the HTTP headers).
   */
  private Bundle getCookies(HttpURLConnection serverConn)
  {
    Bundle cookies = new Bundle();
    Map<String, List<String>> httpHeaders = serverConn.getHeaderFields();
    Set<String> headerKeys = httpHeaders.keySet();
    for (String headerKey : headerKeys) {
      // --DEBUG!-- Log.i(UIActivity.APP_TAG, " CommThread -> Header: " + headerKey + "=" + serverConn.getHeaderField(headerKey) );
      if (headerKey.equals("Set-Cookie")) {
        String cookie = serverConn.getHeaderField(headerKey);
        cookie = cookie.substring(0, cookie.indexOf(";"));
        String cookieName = cookie.substring(0, cookie.indexOf("="));
        String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
        cookies.putString(cookieName, cookieValue);
        // --DEBUG!-- Log.i(UIActivity.APP_TAG, " CommThread -> Set-Cookie: " + cookieName + "=" + cookieValue );
      }
    }
    return cookies;
  }

  /**
   * Encodes cookie data that we wish to send back to the server.
   * @param data Bundle of cookie data we wish to send in the request (name/value pairs)
   * @return String containing encoded cookie data: e.g. "name1=value1; name2=value2", etc.
   */
  private String buildCookies(Bundle data)
  {
    if (data == null) {
      return "";
    } else {
      // Get a list of data keys in the bundle of submitted data.
      Set<String> keys = data.keySet();
      Iterator<String> myIterator = keys.iterator();
      String key;
      String value;
      StringBuilder cookieStrings = new StringBuilder("");
      while (myIterator.hasNext()) {
        key = myIterator.next();
        value = data.get(key).toString();
        cookieStrings.append(key);
        cookieStrings.append("=");
        try {
          cookieStrings.append(URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
          // we will never get here, because every JDK has to support UTF-8
          Log.w(UIActivity.APP_TAG, ex);
        }
        if (myIterator.hasNext()) cookieStrings.append("; ");
      }
      // --DEBUG!-- Log.i(UIActivity.APP_TAG, " CommThread -> Cookies: " +  cookieStrings.toString());
      return cookieStrings.toString();
    }
  }

  /**
   * Make POST Data.
   * Turn a bundle of key/value pairs into a string of POST data suitable for
   * use in the HTTP POST request.
   */
  private static String encodePostData(Bundle data)
  {
    // Get a list of data keys in the bundle of submitted data.
    Set<String> keys = data.keySet();
    Iterator<String> myIterator = keys.iterator();
    String key;
    String value;
    StringBuilder postData = new StringBuilder("");
    while (myIterator.hasNext()) {
      key = myIterator.next();
      value = data.get(key).toString();
      postData.append(key);
      postData.append("=");
      try {
        postData.append(URLEncoder.encode(value, "UTF-8"));
      } catch (UnsupportedEncodingException ex) {
        // we will never get here, because every JDK has to support UTF-8
        Log.w(UIActivity.APP_TAG, ex);
      }
      if (myIterator.hasNext()) postData.append("&");
    }
    return postData.toString();
  }

  @Override
  public void run()
  {
    isRun = true; // Signal that we have started work!
    int readChr = 1;
    URL serverURL;

    // if there is data to send to the server, encode it:
    String postString = "";
    if (postData != null) postString = encodePostData(postData);

    // Get output data size:
    int postDataSize = postString.length();

    try {
      // ******** Connect to the server: ******************************************************
      serverURL = new URL(connectTo);
      HttpURLConnection serverConn = (HttpURLConnection) serverURL.openConnection();
      serverConn.setInstanceFollowRedirects(followRedirects);
      serverConn.setRequestProperty("Cookie", buildCookies(cookieDataToSend) );
      //--- Sent HTTP POST data if there is any: -----------------
      if (postDataSize > 0) {
        // We have data to send
        serverConn.setRequestMethod("POST");
        serverConn.setDoOutput(true);
        serverConn.setRequestProperty("Accept-Charset", USE_CHARSET);
        serverConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + USE_CHARSET);
        OutputStream streamOut = serverConn.getOutputStream();
        // ******** HTTP Connection Debug Info: **********************************
        Log.i("HTTPConn", "URL:" + connectTo);
        Log.i("HTTPConn", "POST Data:" + postString);
        // ***********************************************************************
        streamOut.write(postString.getBytes(USE_CHARSET));
        streamOut.close();
        Thread.sleep(0); // Give up CPU time (allows for slow data rate).
      } else {
        serverConn.connect();
      }

      // See if the server sent any cookies
      Bundle requestCookies = getCookies(serverConn);


      // ********* Create input stream and read the response sent by the server ********************
      StringBuilder httpReceivedData = new StringBuilder(""); // Empty string buffer. We'll add data we receive by HTTP to this.
      Bundle dataBundle = new Bundle(); // Empty data bundle. We'll put any HTML, XML or JSON data in this to return it to the parent.
      InputStream streamIn = new BufferedInputStream(serverConn.getInputStream());
      // ----- Read data that the server sent to us (HTTP Response): ---------
      while (readChr > -1) {
        //if (this.isCancelled()) return 0;
        readChr = streamIn.read();
        if (readChr > -1) httpReceivedData.append((char)readChr);
        Thread.sleep(0);  // Give up CPU time (allows for slow data rate).
      }

      responseCode = serverConn.getResponseCode(); // Get the response code.

      // *********** HTTP Connection Debug Info: **********************************
      Log.i("HTTPConn", "Code:" + responseCode );
      Log.i("HTTPConn", "Message:" + serverConn.getResponseMessage() );
      Log.i("HTTPConn", "Redirect To: " + serverConn.getHeaderField("Location") );
      //Log.i("HTTPConn", "Response:" + httpReceivedData );
      // **************************************************************************

      /* ********** SolarNode Login Hack: *****************************************
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
       ***************************************************************************/
      if (responseCode == 302) responseCode = 200;                                         // For a start, just change redirects to "OK".
      if ((serverConn.getHeaderField("Location") != null) && (serverConn.getHeaderField("Location").contains("login.do"))) {
        // Login Failed! Change to our own error.
        responseCode = 999;
      }
      // ***************************************************************************

      streamIn.close();
      serverConn.disconnect();
      // Add any cookies from the request to any data we have:
      dataBundle.putBundle("Cookies", requestCookies);
      // Add the HTTP Response code to the data:
      dataBundle.putInt("ResponseCode", responseCode);
      // Send a message with HTTP data in the string field, and dataBundle in the data field (contains cookies and response code):
      comthreadMessages.sendData(responseIntent, responseMessage,  null, httpReceivedData.toString(), dataBundle);
    } catch (Exception e) {
      Log.w("HTTPConn", e);
      comthreadMessages.sendData(responseIntent, CONN_ERROR, null, e.getMessage() , null);
      // --DEBUG!!--Log.i("HTTPConn", "HTTP Error!");
    }
  }

  public void messageReceived(String action, int message, Float floatData, String stringData, Bundle data)
  {
    // We don't respond to any incomming messages.
  }
}
