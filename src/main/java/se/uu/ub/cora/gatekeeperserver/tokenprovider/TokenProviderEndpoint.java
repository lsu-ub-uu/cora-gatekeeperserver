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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import se.uu.ub.cora.gatekeeper.picker.UserInfo;
import se.uu.ub.cora.gatekeeperserver.Gatekeeper;
import se.uu.ub.cora.gatekeeperserver.authentication.AuthenticationException;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;

@Path("authToken")
public final class TokenProviderEndpoint {

	@POST
	public Response getAuthTokenForUserInfo(String jsonUserInfo) {
		try {
			return tryToGetAuthTokenForUserInfo(jsonUserInfo);
		} catch (AuthenticationException e) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
	}

	private Response tryToGetAuthTokenForUserInfo(String jsonUserInfo) {
		Gatekeeper gatekeeper = GatekeeperInstanceProvider.getGatekeeper();
		UserInfo userInfo = convertJsonToUserInfo(jsonUserInfo);
		AuthToken authTokenForUserInfo = gatekeeper.getAuthTokenForUserInfo(userInfo);
		String json = convertAuthTokenToJson(authTokenForUserInfo);
		return Response.status(Response.Status.OK).entity(json).build();
	}

	private UserInfo convertJsonToUserInfo(String jsonUserInfo) {
		JsonToUserInfoConverter converter = new JsonToUserInfoConverter(jsonUserInfo);
		return converter.parseUserInfoFromJson();
	}

	private String convertAuthTokenToJson(AuthToken authTokenForUserInfo) {
		AuthTokenToJsonConverter converter = new AuthTokenToJsonConverter(authTokenForUserInfo);
		return converter.convertAuthTokenToJson();
	}

	@DELETE
	@Path("{tokenId}")
	public Response removeAuthToken(@PathParam("tokenId") String tokenId, String authToken) {
		try {
			return tryToRemoveAuthToken(tokenId, authToken);
		} catch (AuthenticationException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	private Response tryToRemoveAuthToken(String tokenId, String authToken) {
		Gatekeeper gatekeeper = GatekeeperInstanceProvider.getGatekeeper();
		gatekeeper.removeAuthToken(tokenId, authToken);
		return Response.status(Response.Status.OK).build();
	}
}
