/*
 * Copyright 2016, 2025 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.gatekeeperserver.authentication;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocatorSpy;

public class AuthenticatorEndpointTest {
	private AuthenticatorEndpoint authenticatorEndpoint;
	private Response response;
	private GatekeeperLocatorSpy locator;
	private GatekeeperSpy gatekeeperSpy;

	@BeforeMethod
	public void setUp() {
		gatekeeperSpy = new GatekeeperSpy();
		locator = new GatekeeperLocatorSpy();
		locator.MRV.setDefaultReturnValuesSupplier("locateGatekeeper", () -> gatekeeperSpy);
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);

		authenticatorEndpoint = new AuthenticatorEndpoint();
	}

	@Test
	public void testDependenciesAreCalled() {
		String token = "someToken";
		response = authenticatorEndpoint.getUserForToken(token);
		locator.MCR.assertMethodWasCalled("locateGatekeeper");
		gatekeeperSpy.MCR.assertMethodWasCalled("getUserForToken");
	}

	@Test
	public void testGetUserWithActiveUser() {
		setGetUserForTokenWithActiveUser();

		response = authenticatorEndpoint.getUserForToken("someToken");

		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = """
				{
				  "children": [
				    {
				      "children": [],
				      "name": "userRole"
				    },
				    {"name": "activeStatus", "value": "active"}
				  ],
				  "name": "someId"
				}
				""";
		assertEquals(response.getEntity(), compactJson(expected));

	}

	private void setGetUserForTokenWithActiveUser() {
		User user = new User("someId");
		user.active = true;
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> user);
	}

	@Test
	public void testGetUserWithInactiveUser() {
		setGetUserForTokenWithInactiveUser();

		response = authenticatorEndpoint.getUserForToken("someToken");

		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = """
				{
				  "children": [
				    {
				      "children": [],
				      "name": "userRole"
				    },
				    {"name": "activeStatus", "value": "inactive"}
				  ],
				  "name": "someId"
				}
				""";
		assertEquals(response.getEntity(), compactJson(expected));
	}

	private void setGetUserForTokenWithInactiveUser() {
		User user = new User("someId");
		user.active = false;
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> user);
	}

	private void assertResponseStatusIs(Status responseStatus) {
		assertEquals(response.getStatusInfo(), responseStatus);
	}

	private void assertEntityExists() {
		assertNotNull(response.getEntity(), "An entity in json format should be returned");
	}

	@Test
	public void testNonAuthenticatedToken() {
		gatekeeperSpy.MRV.setAlwaysThrowException("getUserForToken",
				new AuthenticationException("token not valid"));

		response = authenticatorEndpoint.getUserForToken("dummyNonAuthenticatedToken");

		assertResponseStatusIs(Response.Status.UNAUTHORIZED);
	}

	@Test
	public void testGetUserForTokenWithUserRoles() {
		setGetUserForTokenWithUserWithRoles();

		response = authenticatorEndpoint.getUserForToken("someToken");

		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = """
				{
				  "children": [
				    {
				      "children": [
				        {
				          "children": [ {"name": "id", "value": "someRole1"} ],
				          "name": "permissionRole"
				        },
				        {
				          "children": [ {"name": "id", "value": "someRole2"} ],
				          "name": "permissionRole"
				        }
				      ],
				      "name": "userRole"
				    },
				    {"name": "activeStatus", "value": "active"}
				  ],
				  "name": "someId"
				}""";
		assertEquals(response.getEntity(), compactJson(expected));
	}

	private void setGetUserForTokenWithUserWithRoles() {
		User user = new User("someId");
		user.active = true;
		user.roles.add("someRole1");
		user.roles.add("someRole2");
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> user);
	}

	@Test
	public void testNoTokenShouldBeGuest() {
		setGetUserForTokenWithGuestUser();

		response = authenticatorEndpoint.getUserForToken(null);

		assertResponseIsCorrectGuestUser();
	}

	private void setGetUserForTokenWithGuestUser() {
		User user = new User("guestUser");
		user.active = true;
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> user);
	}

	@Test
	public void testGetUserWithoutTokenShouldBeGuest() {
		setGetUserForTokenWithGuestUser();

		response = authenticatorEndpoint.getUserWithoutToken();

		assertResponseIsCorrectGuestUser();
	}

	private void assertResponseIsCorrectGuestUser() {
		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = """
				{
				  "children": [
				    {
				      "children": [],
				      "name": "userRole"
				    },
				    {"name": "activeStatus", "value": "active"}
				  ],
				  "name": "guestUser"
				}""";
		assertEquals(response.getEntity(), compactJson(expected));
	}

	@Test
	public void testGetUserWithPermissionUnits() {
		setGetUserForTokenWithUserWithPermissionUnits();

		response = authenticatorEndpoint.getUserForToken("someToken");

		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = """
				{
				  "children": [
				    {
				      "children": [],
				      "name": "userRole"
				    },
				    {"name": "permissionUnit", "value": "somePermissionUnit001"},
				    {"name": "permissionUnit", "value": "somePermissionUnit002"},
				    {"name": "activeStatus", "value": "active"}
				  ],
				  "name": "someId"
				}
				""";
		assertEquals(response.getEntity(), compactJson(expected));

	}

	private void setGetUserForTokenWithUserWithPermissionUnits() {
		User user = new User("someId");
		user.active = true;
		user.permissionUnitIds.add("somePermissionUnit001");
		user.permissionUnitIds.add("somePermissionUnit002");
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> user);
	}

	@Test
	public void testGetUserWithUserWithAllFieldsSet() {
		setGetUserForTokenWithAllFieldsSet();

		response = authenticatorEndpoint.getUserForToken("someToken");

		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = """
				{
				  "children": [
				    {
				      "children": [
				        {
				          "children": [ {"name": "id", "value": "someRole1"} ],
				          "name": "permissionRole"
				        },
				        {
				          "children": [ {"name": "id", "value": "someRole2"} ],
				          "name": "permissionRole"
				        }
				      ],
				      "name": "userRole"
				    },
				    {"name": "permissionUnit", "value": "somePermissionUnit001"},
				    {"name": "permissionUnit", "value": "somePermissionUnit002"},
				    {"name": "activeStatus", "value": "active"}
				  ],
				  "name": "someId"
				}""";
		assertEquals(response.getEntity(), compactJson(expected));

	}

	private void setGetUserForTokenWithAllFieldsSet() {
		User user = new User("someId");
		user.active = true;
		user.roles.add("someRole1");
		user.roles.add("someRole2");
		user.permissionUnitIds.add("somePermissionUnit001");
		user.permissionUnitIds.add("somePermissionUnit002");
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> user);
	}

	private String compactJson(String json) {
		return json.replace("\n", "").replace("\r", "").replace(" ", "");
	}
}
