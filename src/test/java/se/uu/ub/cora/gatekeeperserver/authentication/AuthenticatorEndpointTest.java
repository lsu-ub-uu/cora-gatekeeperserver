/*
 * Copyright 2016 Uppsala University Library
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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;

public class AuthenticatorEndpointTest {
	private AuthenticatorEndpoint authenticatorEndpoint;
	private Response response;
	private GateKeeperLocatorSpy locator;
	private GatekeeperSpy gatekeeperSpy;

	@BeforeMethod
	public void setUp() {
		locator = new GateKeeperLocatorSpy();
		gatekeeperSpy = new GatekeeperSpy();
		locator.setGatekeepSpy(gatekeeperSpy);
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);
		authenticatorEndpoint = new AuthenticatorEndpoint();
	}

	@Test
	public void testDependenciesAreCalled() {
		String token = "someToken";
		response = authenticatorEndpoint.getUserForToken(token);
		assertTrue(locator.gatekeeperLocated);
		gatekeeperSpy.MCR.assertMethodWasCalled("getUserForToken");
	}

	@Test
	public void testGetUserForToken() {
		String token = "someToken";
		response = authenticatorEndpoint.getUserForToken(token);
		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = "{\"children\":[{\"children\":[{\"children\":["
				+ "{\"name\":\"role\",\"value\":\"someRole1\"}],\"name\":\"rolePlus\"}"
				+ ",{\"children\":[{\"name\":\"role\",\"value\":\"someRole2\"}]"
				+ ",\"name\":\"rolePlus\"}],\"name\":\"rolesPlus\"}],\"name\":\"someId\"}";
		assertEquals(response.getEntity(), expected);
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
	public void testNoTokenShouldBeGuest() {
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken",
				() -> createGuestUser());

		response = authenticatorEndpoint.getUserForToken(null);

		assertResponseIsCorrectGuestUser();
	}

	@Test
	public void testGetUserWithoutTokenShouldBeGuest() {
		gatekeeperSpy.MRV.setDefaultReturnValuesSupplier("getUserForToken",
				() -> createGuestUser());
		response = authenticatorEndpoint.getUserWithoutToken();
		assertResponseIsCorrectGuestUser();
	}

	private User createGuestUser() {
		User user = new User("12345");
		user.roles.add("someRole112345");
		user.roles.add("someRole212345");
		return user;
	}

	private void assertResponseIsCorrectGuestUser() {
		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		String expected = "{\"children\":[{\"children\":["
				+ "{\"children\":[{\"name\":\"role\",\"value\":\"someRole112345\"}]"
				+ ",\"name\":\"rolePlus\"},{\"children\":["
				+ "{\"name\":\"role\",\"value\":\"someRole212345\"}]"
				+ ",\"name\":\"rolePlus\"}],\"name\":\"rolesPlus\"}]" + ",\"name\":\"12345\"}";
		assertEquals(response.getEntity(), expected);
	}

}
