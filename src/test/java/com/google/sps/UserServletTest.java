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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.servlets.AuthServlet;
import com.google.sps.servlets.EventServlet;
import com.google.sps.servlets.UserServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({URL.class, UserServiceFactory.class})
public final class UserServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final Gson gson = new Gson();
  private AuthServlet testAuthServlet;
  private EventServlet testEventServlet;
  private UserServlet testUserServlet;
  private MockedUserService mockService;

  private String activeUrl;

  /**
   * Use the current url to login/logout
   *
   * @param email If logging in, will log into this user's account.
   */
  private void toggleLogin(String email) throws MalformedURLException, IOException {
    URL mockurl = PowerMockito.mock(URL.class);
    when(mockurl.openConnection())
        .thenReturn(mockService.evaluateURL(AuthServletTest.makeLoginURL(activeUrl, email)));
    mockurl.openConnection();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);
    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    activeUrl = result.url;
  }

  /** Checks for equality and order between two entity lists without checking the keys */
  private void assertListsEqual(List<Entity> goalEntityList, List<Entity> resultingEntities) {
    assertEquals(goalEntityList.size(), resultingEntities.size());
    for (int i = 0; i < goalEntityList.size(); i++) {
      Entity goal = goalEntityList.get(i);
      Entity resultEntity = resultingEntities.get(i);
      Set<String> goalProperties = goal.getProperties().keySet();
      Set<String> resultProperties = goal.getProperties().keySet();
      // checks along each of entity properties
      assertEquals(goalProperties.size(), resultProperties.size());
      for (String s : goalProperties) {
        assertEquals(goal.getProperty(s), resultEntity.getProperty(s));
      }
    }
  }

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    PowerMockito.mockStatic(UserServiceFactory.class);
    mockService = new MockedUserService();
    when(UserServiceFactory.getUserService()).thenReturn(mockService);
    testEventServlet = new EventServlet();
    testAuthServlet = new AuthServlet();
    testUserServlet = new UserServlet();

    // get the initial login url
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);
    testAuthServlet.doGet(request, response);
    out.flush();

    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    activeUrl = result.url;
  }

  @After
  public void tearDown() {
    helper.tearDown();
    activeUrl = "";
  }

  @Test
  public void notLoggedIn() throws IOException {
    // post the events to datastore
    postEventsSetup();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testUserServlet.doGet(request, response);
    out.flush();
    List<Entity> resultingEntities =
        gson.fromJson(out.toString(), new TypeToken<ArrayList<Entity>>() {}.getType());

    // create the expected resulting search results
    Entity goalEntity = createLakeCleanupEvent();
    Entity goalEntity2 = createBlmProtestEvent();
    Entity goalEntity3 = createBookDriveEvent();

    // goal list, should be sorted in ascending order by name
    List<Entity> goalEntityList = new ArrayList<>();
    goalEntityList.add(goalEntity2);
    goalEntityList.add(goalEntity3);
    goalEntityList.add(goalEntity);

    assertListsEqual(goalEntityList, resultingEntities);
  }

  @Test
  public void getCreatedEvents() throws IOException {
    // login as test@example.com and make sure method returns correct events

    postEventsSetup();
    toggleLogin("test@example.com");

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("get")).thenReturn("created");

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testUserServlet.doGet(request, response);
    out.flush();
    List<Entity> resultingEntities =
        gson.fromJson(out.toString(), new TypeToken<ArrayList<Entity>>() {}.getType());

    Entity goalEntity = createLakeCleanupEvent();
    Entity goalEntity2 = createBlmProtestEvent();

    List<Entity> goalEntityList = new ArrayList<>();
    goalEntityList.add(goalEntity2);
    goalEntityList.add(goalEntity);

    assertListsEqual(goalEntityList, resultingEntities);
  }

  // TODO: no tests yet for saved events (no means of saving events yet)

  /** Logs in and out a few times, posting events to datastore */
  private void postEventsSetup() throws IOException {
    // posted by test@example.com
    toggleLogin("test@example.com");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("all-tags")).thenReturn("['environment']");
    testEventServlet.doPost(request, response);

    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    when(request.getParameter("event-name")).thenReturn("BLM Protest");
    when(request.getParameter("event-description")).thenReturn("Fight for racial justice!");
    when(request.getParameter("street-address")).thenReturn("Main Street");
    when(request.getParameter("city")).thenReturn("Los Angeles");
    when(request.getParameter("state")).thenReturn("California");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("13:00");
    when(request.getParameter("all-tags")).thenReturn("['blm']");
    testEventServlet.doPost(request, response);
    toggleLogin("test@example.com");

    // posted by another@example.com
    toggleLogin("another@example.com");
    when(request.getParameter("event-name")).thenReturn("Book Drive");
    when(request.getParameter("event-description")).thenReturn("Let's donate books for kids");
    when(request.getParameter("street-address")).thenReturn("School Drive");
    when(request.getParameter("city")).thenReturn("Los Angeles");
    when(request.getParameter("state")).thenReturn("California");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("10:00");
    when(request.getParameter("all-tags")).thenReturn("['education']");
    testEventServlet.doPost(request, response);

    // logout
    toggleLogin("another@example.com");
  }

  // entities to compare against postSetup() method
  private static Entity createLakeCleanupEvent() {
    Entity entity = new Entity("Event");
    entity.setProperty("eventName", "Lake Clean Up");
    entity.setProperty("eventDescription", "We're cleaning up the lake");
    entity.setProperty("streetAddress", "678 Lakeview Way");
    entity.setProperty("city", "Lakeside");
    entity.setProperty("state", "Michigan");
    entity.setProperty("date", "2020-17-05");
    entity.setProperty("startTime", "14:00");
    entity.setProperty("endTime", "");
    entity.setProperty("coverPhoto", "");
    entity.setProperty("tags", "['environment']");
    entity.setProperty("creator", "test@example.com");

    return entity;
  }

  private static Entity createBlmProtestEvent() {
    Entity entity = new Entity("Event");
    entity.setProperty("eventName", "BLM Protest");
    entity.setProperty("eventDescription", "Fight for racial justice!");
    entity.setProperty("streetAddress", "Main Street");
    entity.setProperty("city", "Los Angeles");
    entity.setProperty("state", "California");
    entity.setProperty("date", "2020-17-05");
    entity.setProperty("startTime", "13:00");
    entity.setProperty("endTime", "");
    entity.setProperty("coverPhoto", "");
    entity.setProperty("tags", "['blm']");
    entity.setProperty("creator", "test@example.com");

    return entity;
  }

  private static Entity createBookDriveEvent() {
    Entity entity = new Entity("Event");
    entity.setProperty("eventName", "Book Drive");
    entity.setProperty("eventDescription", "Let's donate books for kids");
    entity.setProperty("streetAddress", "School Drive");
    entity.setProperty("city", "Los Angeles");
    entity.setProperty("state", "California");
    entity.setProperty("date", "2020-17-05");
    entity.setProperty("startTime", "10:00");
    entity.setProperty("endTime", "");
    entity.setProperty("coverPhoto", "");
    entity.setProperty("tags", "['education']");
    entity.setProperty("creator", "another@example.com");

    return entity;
  }

  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}