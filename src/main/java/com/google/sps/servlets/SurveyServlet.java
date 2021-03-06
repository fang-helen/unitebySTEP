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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.sps.Firebase;
import com.google.sps.Interactions;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/submit-survey")
public class SurveyServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SurveyServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    String userToken = request.getParameter("userToken");
    if (!Firebase.isUserLoggedIn(userToken)) {
      response.getWriter().print("false");
      return;
    }
    String userID = Firebase.authenticateUser(userToken);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key userKey = KeyFactory.createKey("User", userID);
    Entity userEntity;
    try {
      userEntity = datastore.get(userKey);
    } catch (EntityNotFoundException e) {
      userEntity = Interactions.makeUserEntity(userID, true);
      LOGGER.info("No entity found for " + userID + ", creating one now.");
      response.getWriter().print("false");
      return;
    }
    if (userEntity.getProperty("surveyCompleted") != null
        && userEntity.getProperty("surveyCompleted").equals("true")) {
      response.getWriter().print("true");
    } else {
      response.getWriter().print("false");
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // store survey results
    response.setContentType("text/html");

    String userToken = request.getParameter("userToken");
    if (Firebase.isUserLoggedIn(userToken)) {
      String userID = Firebase.authenticateUser(userToken);
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key userKey = KeyFactory.createKey("User", userID);

      // make sure there is a datastore entry for the user
      Entity userEntity;
      try {
        userEntity = datastore.get(userKey);
      } catch (EntityNotFoundException e) {
        userEntity = Interactions.makeUserEntity(userID, true);
        LOGGER.info("No entity found for " + userID + ", creating one now.");
      }

      // save score of each survey metric as an entity property
      for (String param : Interactions.metrics) {
        String score = request.getParameter(param);
        if (score == null) {
          throw new IOException("incomplete survey");
        }
        userEntity.setProperty(param, score);
      }
      userEntity.setProperty("surveyCompleted", "true");
      datastore.put(userEntity);
      response.sendRedirect("/index.html");
      return;
    } else {
      throw new IOException("Cannot take survey while not logged in");
    }
  }
}
