/*
 * Copyright 2016, 2024 Uppsala University Library
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

package se.uu.ub.cora.gatekeeperserver.tokenprovider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import se.uu.ub.cora.gatekeeperserver.authentication.AuthenticationException;
import se.uu.ub.cora.gatekeeperserver.authentication.GatekeeperSpy;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocatorSpy;

public class TokenProviderEndpointTest {
	private static final String TOKEN = "someToken";
	private static final String TOKEN_ID = "someTokenId";
	private Response response;
	private GatekeeperLocatorSpy locator;
	private TokenProviderEndpoint tokenProviderEndpoint;
	private String jsonUserInfo;
	private GatekeeperSpy gatekeeperSpy;

	@BeforeMethod
	public void setUp() {

		locator = new GatekeeperLocatorSpy();
		gatekeeperSpy = new GatekeeperSpy();
		locator.MRV.setDefaultReturnValuesSupplier("locateGatekeeper", () -> gatekeeperSpy);
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);
		tokenProviderEndpoint = new TokenProviderEndpoint();
		jsonUserInfo = jsonUserInfo();
	}

	private String jsonUserInfo() {
		String userInfo = """
				{"children":[{"name":"loginId","value":""},
				{"name":"domainFromLogin","value":""},
				{"name":"idInUserStorage","value":"131313"}],
				"name":"userInfo"}""";
		return userInfo.replace("\n", "");
	}

	@Test
	public void testDependenciesAreCalled() {
		response = tokenProviderEndpoint.getAuthTokenForUserInfo(jsonUserInfo);
		locator.MCR.assertMethodWasCalled("locateGatekeeper");
		gatekeeperSpy.MCR.assertMethodWasCalled("getAuthTokenForUserInfo");
	}

	@Test
	public void testGetToken_Annotations() throws NoSuchMethodException {
		AnnotationTestHelper annotationHelper = AnnotationTestHelper
				.createAnnotationTestHelperForClassMethodNameAndNumOfParameters(
						TokenProviderEndpoint.class, "getAuthTokenForUserInfo", 1);

		annotationHelper.assertHttpMethodAnnotation("POST");
		annotationHelper.assertConsumesAnnotation("application/vnd.uub.userInfo+json");
		annotationHelper.assertProducesAnnotation("application/vnd.uub.authToken+json");
	}

	@Test
	public void testGetToken() {
		response = tokenProviderEndpoint.getAuthTokenForUserInfo(jsonUserInfo);

		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		assertEquals(response.getEntity(), expectedAuthToken());
	}

	private String expectedAuthToken() {
		String authToken = """
				{
				  "children": [
				    {
				      "name": "token",
				      "value": "someAuthToken"
				    },
				    {
				      "name": "tokenId",
				      "value": "someTokenId"
				    },
				    {
				      "name": "validUntil",
				      "value": "100"
				    },
				    {
				      "name": "renewUntil",
				      "value": "200"
				    },
				    {
				      "name": "idInUserStorage",
				      "value": "someIdFromStorage"
				    },
				    {
				      "name": "loginId",
				      "value": "someloginId"
				    }
				  ],
				  "name": "authToken"
				}""";
		return authToken.replace("\n", "").replace(" ", "");
	}

	private void assertResponseStatusIs(Status responseStatus) {
		assertEquals(response.getStatusInfo(), responseStatus);
	}

	private void assertEntityExists() {
		assertNotNull(response.getEntity(), "An entity in json format should be returned");
	}

	@Test
	public void testNonUserInfoWithProblem() {
		gatekeeperSpy.MRV.setAlwaysThrowException("getAuthTokenForUserInfo",
				new AuthenticationException("problem getting authToken for userInfo"));

		response = tokenProviderEndpoint.getAuthTokenForUserInfo(jsonUserInfo);

		assertResponseStatusIs(Response.Status.UNAUTHORIZED);
	}

	@Test
	public void testRenewAuthToken_Annotations() throws NoSuchMethodException {
		AnnotationTestHelper annotationHelper = AnnotationTestHelper
				.createAnnotationTestHelperForClassMethodNameAndNumOfParameters(
						TokenProviderEndpoint.class, "renewAuthToken", 2);

		annotationHelper.assertHttpMethodAndPathAnnotation("POST", "{tokenId}");
		annotationHelper.assertPathParamAnnotationByNameAndPosition("tokenId", 0);
		annotationHelper.assertConsumesAnnotation("text/plain");
		annotationHelper.assertProducesAnnotation("application/vnd.uub.authToken+json");
	}

	@Test
	public void testRenewAuthCallsCorrectMethod() {
		tokenProviderEndpoint.renewAuthToken(TOKEN_ID, TOKEN);

		gatekeeperSpy.MCR.assertCalledParametersReturn("renewAuthToken", TOKEN_ID, TOKEN);
	}

	@Test
	public void testRenewAuthUnAuthorized() {
		gatekeeperSpy.MRV.setAlwaysThrowException("renewAuthToken",
				new AuthenticationException("error from spy"));

		response = tokenProviderEndpoint.renewAuthToken(TOKEN_ID, TOKEN);

		assertResponseStatusIs(Response.Status.UNAUTHORIZED);
	}

	@Test
	public void testRenewAuthTokenOK() {

		response = tokenProviderEndpoint.renewAuthToken(TOKEN_ID, TOKEN);

		assertResponseStatusIs(Response.Status.OK);
		assertEntityExists();
		assertEquals(response.getEntity(), expectedAuthToken());

	}

	@Test
	public void testRemoveAuthToken_Annotations() throws NoSuchMethodException {
		AnnotationTestHelper annotationHelper = AnnotationTestHelper
				.createAnnotationTestHelperForClassMethodNameAndNumOfParameters(
						TokenProviderEndpoint.class, "removeAuthToken", 2);

		annotationHelper.assertHttpMethodAndPathAnnotation("DELETE", "{tokenId}");
		annotationHelper.assertPathParamAnnotationByNameAndPosition("tokenId", 0);
		annotationHelper.assertConsumesAnnotation("text/plain");
	}

	@Test
	public void testRemoveAuthTokenForUser() {
		response = tokenProviderEndpoint.removeAuthToken(TOKEN_ID, TOKEN);

		gatekeeperSpy.MCR.assertParameters("removeAuthToken", 0, TOKEN_ID, TOKEN);
		assertResponseStatusIs(Response.Status.OK);
	}

	@Test
	public void testRemoveAuthTokenForUserWithProblem() {
		gatekeeperSpy.MRV.setAlwaysThrowException("removeAuthToken",
				new AuthenticationException("authToken does not exist"));
		response = tokenProviderEndpoint.removeAuthToken(TOKEN_ID, "someNonExistingToken");

		assertResponseStatusIs(Response.Status.NOT_FOUND);
	}
}
