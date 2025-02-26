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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.Gatekeeper;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.json.builder.JsonArrayBuilder;
import se.uu.ub.cora.json.builder.JsonObjectBuilder;
import se.uu.ub.cora.json.builder.org.OrgJsonBuilderFactoryAdapter;

@Path("user")
public class AuthenticatorEndpoint {

	private static final String NAME = "name";
	private static final String CHILDREN = "children";
	private Gatekeeper gatekeeper;
	private OrgJsonBuilderFactoryAdapter orgJsonBuilderFactoryAdapter;

	public AuthenticatorEndpoint() {
		gatekeeper = GatekeeperInstanceProvider.getGatekeeper();
		orgJsonBuilderFactoryAdapter = new OrgJsonBuilderFactoryAdapter();
	}

	// TODO: create getGuestUser method in gatekeeper, instead of using getUseForToken(null)
	@GET
	public Response getUserWithoutToken() {
		return tryToGetUserForToken(null);
	}

	@GET
	@Path("{token}")
	public Response getUserForToken(@PathParam("token") String token) {
		try {
			return tryToGetUserForToken(token);
		} catch (AuthenticationException e) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
	}

	private Response tryToGetUserForToken(String token) {
		User user = gatekeeper.getUserForToken(token);
		String json = convertUserToCompactJson(user);
		return Response.status(Response.Status.OK).entity(json).build();
	}

	private String convertUserToCompactJson(User user) {
		JsonObjectBuilder userBuilder = convertUserToJson(user);
		return userBuilder.toJsonFormattedString();
	}

	private JsonObjectBuilder convertUserToJson(User user) {
		JsonObjectBuilder userBuilder = createObjectBuilderWithName(user.id);
		JsonArrayBuilder userChildren = createArrayBuildeWithObjectBuilder(userBuilder);
		addUserRoles(user, userChildren);
		addPermissionUnits(user, userChildren);
		setActiveUser(user, userChildren);
		return userBuilder;
	}

	private void setActiveUser(User user, JsonArrayBuilder userChildren) {
		String status = user.active ? "active" : "inactive";
		createAtomicElement("activeStatus", status, userChildren);
	}

	private JsonObjectBuilder createObjectBuilderWithName(String name) {
		JsonObjectBuilder roleBuilder = orgJsonBuilderFactoryAdapter.createObjectBuilder();
		roleBuilder.addKeyString(NAME, name);
		return roleBuilder;
	}

	private JsonArrayBuilder createArrayBuildeWithObjectBuilder(JsonObjectBuilder userBuilder) {
		JsonArrayBuilder userChildren = orgJsonBuilderFactoryAdapter.createArrayBuilder();
		userBuilder.addKeyJsonArrayBuilder(CHILDREN, userChildren);
		return userChildren;
	}

	private void addUserRoles(User user, JsonArrayBuilder userChildren) {
		JsonArrayBuilder userRoleChildren = createUserRole(userChildren);
		createAndAddUserRolesToPermissionRoles(user, userRoleChildren);
	}

	private JsonArrayBuilder createUserRole(JsonArrayBuilder userChildren) {
		JsonObjectBuilder userRoleBuilder = createObjectBuilderWithName("userRole");
		userChildren.addJsonObjectBuilder(userRoleBuilder);
		return createArrayBuildeWithObjectBuilder(userRoleBuilder);
	}

	private void createAndAddUserRolesToPermissionRoles(User user,
			JsonArrayBuilder permissionRoles) {
		for (String roleId : user.roles) {
			JsonObjectBuilder permissionRoleBuilder = createPermissionRoleBuilder(permissionRoles);
			JsonArrayBuilder permissionRoleChildren = createArrayBuildeWithObjectBuilder(
					permissionRoleBuilder);
			createAtomicElement("id", roleId, permissionRoleChildren);
		}
	}

	private JsonObjectBuilder createPermissionRoleBuilder(JsonArrayBuilder rolesPlusChildren) {
		JsonObjectBuilder permissionRoleBuilder = createObjectBuilderWithName("permissionRole");
		rolesPlusChildren.addJsonObjectBuilder(permissionRoleBuilder);
		return permissionRoleBuilder;
	}

	private void createAtomicElement(String atomicName, String atomicValue,
			JsonArrayBuilder permissionRoleChildren) {
		JsonObjectBuilder roleBuilder = createObjectBuilderWithName(atomicName);
		permissionRoleChildren.addJsonObjectBuilder(roleBuilder);
		roleBuilder.addKeyString("value", atomicValue);
	}

	private void addPermissionUnits(User user, JsonArrayBuilder userChildren) {
		user.permissionUnitIds.forEach(permissionUnitId -> createAtomicElement("permissionUnit",
				permissionUnitId, userChildren));
	}
}
