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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.sps.servlets.AuthServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/** */
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserServiceFactory.class)
public final class AuthServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final Gson gson = new Gson();
  private AuthServlet testAuthServlet;
  private MockedUserService mockService;

  /**
   * opens a url and performs a doGet request
   *
   * @param authUrl The url generated by the UserService to login/logout with
   * @param email The email of the intended user to login as, or logout from
   * @return A LoginObject created from the JSON returned by the request
   */
  private LoginObject openUrlAndDoGet(String authUrl, String email) throws IOException {
    mockService.evaluateUrl(makeLoginURL(authUrl, email));

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);
    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    return result;
  }

  @Before
  public void setUp() {
    helper.setUp();
    testAuthServlet = new AuthServlet();

    PowerMockito.mockStatic(UserServiceFactory.class);
    mockService = new MockedUserService();
    when(UserServiceFactory.getUserService()).thenReturn(mockService);
  }

  @After
  public void tearDown() {
    helper.tearDown();
    mockService = null;
  }

  @Test
  public void loggedOut() throws IOException {
    // check for the logged out state (default)
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);

    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    assertEquals(false, result.loggedIn);
    assertTrue(result.url.contains("login"));
  }

  @Test
  public void doLogin() throws IOException {
    // login from a logged out state

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);
    out.flush();
    LoginObject resultObj = gson.fromJson(out.toString(), LoginObject.class);

    String authUrl = resultObj.url;

    try {
      LoginObject result = openUrlAndDoGet(authUrl, "test@example.com");
      assertEquals(true, result.loggedIn);
      assertTrue(result.url.contains("logout"));
      assertEquals("test@example.com", mockService.getCurrentUser().getEmail());

      // make sure exactly 1 user has been added to datastore
      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      assertEquals(1, ds.prepare(new Query("User")).countEntities());

      // make sure that the posted user has the correct id and empty saved list
      Entity postedEntity = ds.prepare(new Query("User")).asSingleEntity();
      assertEquals("test@example.com", postedEntity.getKey().getName());
      assertEquals("test@example.com", postedEntity.getProperty("id"));
    } catch (IOException e) {
      fail();
    }
  }

  @Test
  public void loginTwice() throws IOException {
    // login, logout, then login again

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);
    testAuthServlet.doGet(request, response);
    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    String authUrl = result.url;

    try {
      // use the link to login

      String currentEmail = "test@example.com";
      result = openUrlAndDoGet(authUrl, currentEmail);

      // use the link to logout
      authUrl = result.url;
      result = openUrlAndDoGet(authUrl, currentEmail);

      // now login again and make another request
      authUrl = result.url;
      result = openUrlAndDoGet(authUrl, currentEmail);
      assertEquals(true, result.loggedIn);
      assertTrue(result.url.contains("logout"));
      assertEquals(currentEmail, mockService.getCurrentUser().getEmail());

      // make only 1 user has been added to datastore
      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      assertEquals(1, ds.prepare(new Query("User")).countEntities());
    } catch (IOException e) {
      fail();
    }
  }

  @Test
  public void loginDifferent() throws IOException {
    // login, logout, then a different user logs in

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);
    testAuthServlet.doGet(request, response);
    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    String authUrl = result.url;

    try {
      // use the link to login
      String currentEmail = "test@example.com";
      result = openUrlAndDoGet(authUrl, currentEmail);

      // use the link to logout
      authUrl = result.url;
      result = openUrlAndDoGet(authUrl, currentEmail);

      // now login again with a different id and make another request
      currentEmail = "newtest@example.com";
      authUrl = result.url;
      result = openUrlAndDoGet(authUrl, currentEmail);
      assertEquals(true, result.loggedIn);
      assertTrue(result.url.contains("logout"));
      assertEquals(currentEmail, mockService.getCurrentUser().getEmail());

      // make sure 2 different users have been added to datastore
      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      assertEquals(2, ds.prepare(new Query("User")).countEntities());
    } catch (IOException e) {
      fail();
    }
  }

  /** makes a dummy URL using a url "base" with an intended user id */
  public static String makeLoginURL(String url, String user) {
    return url + "email=" + user;
  }

  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}
