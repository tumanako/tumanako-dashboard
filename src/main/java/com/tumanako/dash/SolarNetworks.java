package com.tumanako.dash;


/************************************************************************************
 Tumanako - Electric Vehicle and Motor control software
 
 Copyright (C) 2013 Jeremy Cole-Baker <jeremy@rhtech.co.nz>

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


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.*;

import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;


/*********************************************************
 * SolarNetworks API Support:
 *  
 * This class contains some methods ported from the SolarNetworks API
 * to allow the generation of security tokens compatible with the 
 * SolarNetworks web services.  
 *   
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ********************************************************/


public class SolarNetworks
  {

  /******** Constructor: ********/ 
  public SolarNetworks()
     { }
  
  
  
  
  
  /**
   * Generate the authorization header value for a set of request parameters.
   * 
   * <p>This returns just the authorization header value, without the scheme. For 
   * example this might return a value like 
   * <code>a09sjds09wu9wjsd9uya:6U2NcYHz8jaYhPd5Xr07KmfZbnw=</code>. To use
   * as a valid <code>Authorization</code> header, you must still prefix the
   * returned value with <code>SolarNetworkWS</code> (with a space between
   * that prefix and the associated value).</p>
   * 
   * <p>Note that the <b>Content-MD5</b> and <b>Content-Type</b> headers are <b>not</b>
   * supported.</p>
   * 
   * @param {Object} params the request parameters
   * @param {String} params.method the HTTP request method
   * @param {String} params.date the formatted HTTP request date
   * @param {String} params.path the SolarNetworkWS canonicalized path value
   * @param {String} params.token the authentication token
   * @param {String} params.secret the authentication token secret
   * @return {String} the authorization header value
   */
  public static String generateAuthorizationHeaderValue
       ( 
       String method,
       String contentType,
       String date,
       String path,
       String token,
       String secret
       )
    {
    String msg = 
      method.toUpperCase() + "\n\n" + 
      contentType + "\n" + 
      date +"\n" + 
      path;
//----------------------    
//    String hash = CryptoJS.HmacSHA1(msg, secret);
//    String authHeader = token +':' +CryptoJS.enc.Base64.stringify(hash);
//-------
    
    Mac mac;
    //PBEKeySpec passwordSpec = new PBEKeySpec(secret.toCharArray());

    try
      {
      mac = Mac.getInstance("HmacSHA1");
      SecretKeySpec secretSpec = new SecretKeySpec(secret.getBytes(), mac.getAlgorithm());
      mac.init(secretSpec);
      } 
    catch (Exception e)
      {
      e.printStackTrace();
      Log.i("HTTPConn", "ERROR: " + e.getMessage() + ";\n" + e.toString() + ";\n" + e.getCause());
      return "";
      }
   
    byte[] digest = mac.doFinal(msg.getBytes());
    String encDigest = token + ":" + Base64.encodeToString(digest, 0);
    return encDigest.substring(0, encDigest.length()-1); 

  }

  
  
  
  
  
  
  /**
   * Parse the query portion of a URL string, and return a parameter object for the
   * parsed key/value pairs.
   * 
   * <p>Multiple parameters of the same name are <b>not</b> supported.</p>
   * 
   * @param {String} search the query portion of the URL, which may optionally include 
   *                        the leading '?' character
   * @return {Object} the parsed query parameters, as a parameter object
   */
  public static Bundle parseURLQueryTerms( String search ) 
    {
    Bundle params = new Bundle();
    String pairs[];
    String pair[];
    Integer i, len;
    if ( search.length() > 0 ) 
      {
      // Remove any leading ? character:
      if ( search.startsWith("?") ) search = search.substring(1);
      pairs = search.split("&");
      for ( i = 0, len = pairs.length; i < len; i++ ) 
        {
        pair = pairs[i].split("=", 2);
        if ( pair.length == 2 ) 
          {
          params.putString( Uri.decode(pair[0]),  Uri.decode(pair[1]) );
          }
        }  // [for ( i = 0, len = pairs.length; i < len; i++ )]
      }  // [if ( search.length() > 0 )]
    return params;
    }

  
  
  
  
  
  
  
  
  
  
  /**
   * Generate the SolarNetworkWS path required by the authorization header value.
   * 
   * <p>This method will parse the given URL and then apply the path canonicalization
   * rules defined by the SolarNetworkWS scheme.</p>
   * 
   * @param {String} url the request URL
   * @param {String} data String containing POST data (e.g. "x=1&y=2"
   * @return {String} path the canonicalized path value to use in the SolarNetworkWS 
   *                       authorization header value
   */
  public static String authURLPath( String url, String data )
    {
//    var a = document.createElement('a');
//    a.href = url;
    Uri thisUri  = Uri.parse(url);
    String path = thisUri.getPath();
    // If POST data is supplied, it overrides any "Querystring" data supplied in the URL. 
    // Nb: This behaviour means we can't send both query params AND POST data; but this seems to be the logic of the SN system.
    String paramString;
    if (data.length() == 0)  paramString = thisUri.getQuery();
    else                     paramString = data;

    // Handle query / POST params, which must be sorted:
    if (paramString != null)
      {
      Bundle params = parseURLQueryTerms(paramString);
      //Set<String> paramKeys = params.keySet();
      List<String> sortedKeys = new ArrayList<String>();
      sortedKeys.addAll(params.keySet());
      Collections.sort(sortedKeys);
      Integer i, len;
      boolean isFirst = true;
      len = sortedKeys.size(); 
      if ( sortedKeys.size() > 0 ) 
        {
        path += "?";
        for ( i = 0; i < len; i++ ) 
          {
          if ( isFirst ) isFirst = false;
          else           path += "&";
          path +=  sortedKeys.get(i);
          path += "=";
          path += params.getString(sortedKeys.get(i));
          }
        }
      }  // [if (paramString != null)]
    return path;
  }

  
  
  
  
  
  
  
  
  
  /**
   * Make an HTTP request to invoke the web service URL, adding the required SolarNetworkWS authorization
   * headers to the request.
   * 
   * <p>This method will construct the <code>X-SN-Date</code> and <code>Authorization</code>
   * header values needed to invoke the web service.
   * 
   * It returns a Bundle containing the extra headers which need to be added to the HTTP request. 
   * 
   * @param {String} url the web service URL to invoke
   * @param {String} method the HTTP method to use; e.g. GET or POST
   * @param {String} data the HTTP data to send, e.g. for POST
   * @param {String} contentType the HTTP content type; defaults to 
                                 <code>application/x-www-form-urlencoded; charset=UTF-8</code>
   * @return {Bundle} Extra Headers (name/value pairs).
   */
  public static Bundle getAuthorisationHeaders( 
      String url, 
      String method, 
      String contentType,
      String data,
      String token,
      String secret )
    {
    // method e.g.      'GET'
    // contentType e.g. 'application/x-www-form-urlencoded; charset=UTF-8'
    // Date Format e.g. 'Sun, 09 Jun 2013 20:11:03 GMT'
    
    SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.US);  // Use US locale for formatting (ensure normal letters and didgits!)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));                // Convert local time to UTC time zone
    
    String date = sdf.format(new Date()) + " GMT";
    String path = authURLPath(url, data); 
    
    String auth = generateAuthorizationHeaderValue(
        method,
        contentType,
        date,
        path,
        token,
        secret  );  

    // Put the new headers in a bundle:
    Bundle newHeaders = new Bundle();
    newHeaders.putString("X-SN-Date", date);
    newHeaders.putString("Authorization", "SolarNetworkWS " + auth);
newHeaders.putString("X-PATH", path);  // DEBUG!!
    return newHeaders;
    }
  
  
  
  } // Class









