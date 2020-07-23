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

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.io.IOException;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({FirebaseAuth.class, Firebase.class})
public final class FirebaseTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private static final Logger LOGGER = Logger.getLogger(FirebaseTest.class.getName());

  @Before
  public void setUp() throws IOException {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void userLoggedIn() throws IOException {
    boolean result = Firebase.isUserLoggedIn("test");
    assertEquals(true, result);
  }

  @Test
  public void userLoggedInEmpty() throws IOException {
    boolean result = Firebase.isUserLoggedIn("");
    assertEquals(false, result);
  }

  @Test
  public void authenticateUserValid() throws IOException {
    PowerMockito.mockStatic(FirebaseAuth.class);
    FirebaseToken decodedToken = mock(FirebaseToken.class);
    PowerMockito.mockStatic(Firebase.class);
    PowerMockito.when(FirebaseAuth.getInstance(any())).thenReturn(null);
    try {
      PowerMockito.when(FirebaseAuth.getInstance().verifyIdToken(anyString()))
          .thenReturn(decodedToken);
    } catch (FirebaseAuthException e) {
      LOGGER.warning(e.getMessage());
    }
    PowerMockito.when(decodedToken.getUid()).thenReturn("uid");

    String output = Firebase.authenticateUser("");

    assertEquals("uid", output);
  }
  /*
  @Test
  public void postOneEventToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "test@example.com");

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Assert only one Entity was posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(1, ds.prepare(new Query("Event")).countEntities(withLimit(10)));
    assertEquals(1, ds.prepare(new Query("Interaction")).countEntities(withLimit(10)));
  }

  @Test
  public void postMultipleEventsToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "test@example.com");

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    // Post three events to Datastore.
    testEventServlet.doPost(request, response);
    testEventServlet.doPost(request, response);
    testEventServlet.doPost(request, response);

    // Assert all three Entities were posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(3, ds.prepare(new Query("Event")).countEntities(withLimit(10)));
    Iterable<Entity> interactions = ds.prepare(new Query("Interaction")).asIterable();
    int size = 0;
    for (Entity e : interactions) {
      size++;
      assertEquals(Interactions.CREATE_SCORE, Integer.parseInt(e.getProperty("rating").toString()));
      assertEquals("test@example.com", e.getProperty("user").toString());
    }
    assertEquals(3, size);
  }

  @Test
  public void postEventWithAllFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    String creatorEmail = "test@example.com";
    TestingUtil.mockFirebase(request, creatorEmail);

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Create what the event Entity should look like, but do not post to
    // it to Datastore.
    Entity goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("address", "678 Lakeview Way, Lakeside, Michigan");
    goalEntity.setProperty("date", "Sunday, May 17, 2020");
    goalEntity.setProperty("startTime", "2:00 PM");
    goalEntity.setProperty("endTime", "3:00 PM");
    goalEntity.setProperty("coverPhoto", "/img-2030121");
    goalEntity.setIndexedProperty("tags", Arrays.asList(tags));
    goalEntity.setProperty("creator", creatorEmail);
    goalEntity.setProperty("attendeeCount", 0L);
    goalEntity.setProperty("eventKey", "agR0ZXN0cgsLEgVFdmVudBgBDA");

    // Retrieve the Entity posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = ds.prepare(new Query("Event")).asSingleEntity();

    // Assert the Entity posted to Datastore has the same properties as the
    // the goalEntity.
    assertEntitiesEqual(goalEntity, postedEntity);
  }

  @Test
  public void postEventWithEmptyOptionalFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "test@example.com");

    // This mock request does not include optional fields end-time and cover-photo.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Retrieve the Entity posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = ds.prepare(new Query("Event")).asSingleEntity();

    // Assert the Entity posted to Datastore has empty properties for the
    // parameters that were not in the request.
    assertEquals("", postedEntity.getProperty("endTime"));
    assertEquals("", postedEntity.getProperty("coverPhoto"));
  }

  @Test
  public void postEventWithoutLoggingIn() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "");

    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    try {
      testEventServlet.doPost(request, response);
      // doPost should throw an error because it is not logged in
      fail();
    } catch (IOException e) {
      // no entities should have been posted

      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      assertEquals(0, ds.prepare(new Query("Event")).countEntities());
    }
  }*/
}
