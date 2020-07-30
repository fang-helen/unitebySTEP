// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.GeoPt;
import com.google.gson.Gson;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

public class Utils {

  private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
  private static String MAPS_API_KEY = getApiKey();
  private static final GeoApiContext context =
      new GeoApiContext.Builder().apiKey(MAPS_API_KEY).build();

  private static String getApiKey() {
    String key = null;
    try {
      key = SecretHandler.getApiKey();
    } catch (IOException e) {
      LOGGER.warning(e.getMessage());
    }
    return key;
  }

  /**
   * Converts an Object instance into a JSON string using the Gson library.
   *
   * @param o Object to be converted to JSON
   * @return String containing converted JSON
   */
  public static String convertToJson(Object o) {
    Gson gson = new Gson();
    String json = gson.toJson(o);
    return json;
  }

  /**
   * Gets a parameter from an HTTP request or returns the default value.
   *
   * @param request HTTP request to get the parameter from
   * @param name String containing the name of the parameter to get
   * @param defaultValue String containing a default value to return if there is no parameter
   * @return the request parameter, or the default value if the parameter was not specified by the
   *     client
   */
  public static String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /**
   * Gets the latitude and longitude of a location using the Google Maps API.
   *
   * @param location String containing the address of the location
   * @return the latitude and longitude of the location
   */
  public static LatLng getLatLng(String location) {
    GeocodingResult[] results = null;
    try {
      GeocodingApiRequest request = GeocodingApi.newRequest(context);
      request.address(location);
      results = request.await();
    } catch (ApiException e) {
      LOGGER.warning(e.getMessage());
    } catch (InterruptedException e) {
      LOGGER.warning(e.getMessage());
    } catch (IOException e) {
      LOGGER.warning(e.getMessage());
    }
    if (results == null || results.length == 0) {
      return null;
    }
    return new LatLng(results[0].geometry.location.lat, results[0].geometry.location.lng);
  }

  /** Converts a LatLng to a Geopt. */
  public static GeoPt getGeopt(LatLng location) {
    if (location == null) {
      return null;
    }
    return new GeoPt((float) location.lat, (float) location.lng);
  }

  /** Returns a location as a GeoPt. */
  public static GeoPt getGeopt(String location) {
    if (location == null || location.length() == 0) {
      return null;
    }
    return getGeopt(getLatLng(location));
  }

  /**
   * Calculates the distance in km between two locations using driving routes generated by the
   * Google Maps API.
   *
   * @param from Latitude and longitude of the beginning location
   * @param to Latitude and longitude of the ending location
   * @return the distance in km between the two locations
   */
  public static int getDistance(LatLng from, LatLng to) {
    DistanceMatrix result = null;
    try {
      DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(context);
      request.origins(from);
      request.destinations(to);
      result = request.await();
    } catch (ApiException e) {
      LOGGER.warning(e.getMessage());
    } catch (InterruptedException e) {
      LOGGER.warning(e.getMessage());
    } catch (IOException e) {
      LOGGER.warning(e.getMessage());
    }

    if (result == null || result.rows[0].elements[0].status.toString().equals("ZERO_RESULTS")) {
      return -1;
    }
    int distance = (int) (result.rows[0].elements[0].distance.inMeters / 1000);
    return distance;
  }

  /**
   * Calculates the distance in km between two locations using driving routes generated by the
   * Google Maps API.
   *
   * @param from The name of the beginning location.
   * @param to The name of the ending location.
   * @return the distance in km between the two locations.
   */
  public static int getDistance(String from, String to) {
    if (from.length() == 0 || to.length() == 0) {
      return -1;
    }
    LatLng start = getLatLng(from);
    LatLng end = getLatLng(to);
    if (start == null || end == null) {
      return -1;
    }
    return getDistance(start, end);
  }

  /** Orders a map from greatest to least based off its values. */
  public static final Comparator<Map.Entry<String, Integer>> ORDER_MAP_GREATEST_TO_LEAST =
      new Comparator<Map.Entry<String, Integer>>() {
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
          return (o2.getValue()).compareTo(o1.getValue());
        }
      };

  // comparators to apply sort to results
  public static final Comparator<Entity> ORDER_BY_NAME =
      new Comparator<Entity>() {
        @Override
        public int compare(Entity a, Entity b) {
          if (!a.getKind().equals("Event") || !b.getKind().equals("Event")) {
            throw new IllegalArgumentException("must be event items");
          }
          return a.getProperty("eventName")
              .toString()
              .compareTo(b.getProperty("eventName").toString());
        }
      };

  /** Format time to standard format. */
  public static String formatTime(String time) {
    DateFormat inFormat = new SimpleDateFormat("HH:mm");
    DateFormat outFormat = new SimpleDateFormat("h:mm a");

    Date unformattedTime = null;
    String formattedTime = "";
    try {
      unformattedTime = inFormat.parse(time);
    } catch (ParseException e) {
      LOGGER.info("Could not parse time " + e);
    }

    if (unformattedTime != null) {
      formattedTime = outFormat.format(unformattedTime);
    }

    return formattedTime;
  }

  /** Format date to fit Month Day, Year format. */
  public static String formatDate(String date) {
    DateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat outFormat = new SimpleDateFormat("EEEE, MMMMM dd, yyyy");

    Date unformattedDate = null;
    String formattedDate = "";
    try {
      unformattedDate = inFormat.parse(date);
    } catch (ParseException e) {
      LOGGER.info("Could not parse date " + e);
    }

    if (unformattedDate != null) {
      formattedDate = outFormat.format(unformattedDate);
    }

    return formattedDate;
  }
}
