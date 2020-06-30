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

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.AbstractMap;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    List<String> searchTags = new ArrayList<String>(Arrays.asList(request.getParameter("tags")));

    Filter tagsFilter =
        new FilterPredicate("tags", FilterOperator.IN, searchTags);
    Query query =
        new Query("Event").setFilter(propertyFilter);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    // PreparedQuery that contains all the comments inside it
    PreparedQuery results = datastore.prepare(query);

    List<Entity> events = new ArrayList<Entity>(results.asList());
    

    //get location
    //filter by location and cutoff outside it
    
    //get tags
    //drop all without first tag
    //those with most tags in common with search go to top
    //those closest to the user go to the top
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }
}