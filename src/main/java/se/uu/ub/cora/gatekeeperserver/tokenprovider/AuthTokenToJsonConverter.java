/*
 * Copyright 2016, 2017, 2024 Uppsala University Library
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

import java.util.Optional;

import se.uu.ub.cora.json.builder.JsonArrayBuilder;
import se.uu.ub.cora.json.builder.JsonObjectBuilder;
import se.uu.ub.cora.json.builder.org.OrgJsonBuilderFactoryAdapter;

public final class AuthTokenToJsonConverter {

	private static final String VALUE = "value";
	private static final String NAME = "name";
	private static final String CHILDREN = "children";
	private AuthToken authToken;
	private OrgJsonBuilderFactoryAdapter orgJsonBuilderFactoryAdapter = new OrgJsonBuilderFactoryAdapter();

	public AuthTokenToJsonConverter(AuthToken authToken) {
		this.authToken = authToken;
	}

	public String convertAuthTokenToJson() {
		JsonObjectBuilder userBuilder = createObjectBuilderWithName("authToken");
		JsonArrayBuilder userChildren = returnAndAddChildrenToBuilder(userBuilder);

		addTokenToJson(userChildren);
		addTokenIdToJson(userChildren);
		addValidUntilToJson(userChildren);
		addrenewUntilToJson(userChildren);
		addIdInUserStorageToJson(userChildren);
		addLoginIdToJson(userChildren);
		possiblyAddNameToJson(userChildren);
		return userBuilder.toJsonFormattedString();
	}

	private JsonObjectBuilder createObjectBuilderWithName(String name) {
		JsonObjectBuilder roleBuilder = orgJsonBuilderFactoryAdapter.createObjectBuilder();
		roleBuilder.addKeyString(NAME, name);
		return roleBuilder;
	}

	private void addTokenToJson(JsonArrayBuilder userChildren) {
		JsonObjectBuilder token = createObjectBuilderWithName("token");
		token.addKeyString(VALUE, authToken.token());
		userChildren.addJsonObjectBuilder(token);
	}

	private void addTokenIdToJson(JsonArrayBuilder userChildren) {
		JsonObjectBuilder token = createObjectBuilderWithName("tokenId");
		token.addKeyString(VALUE, authToken.tokenId());
		userChildren.addJsonObjectBuilder(token);
	}

	private void addValidUntilToJson(JsonArrayBuilder userChildren) {
		JsonObjectBuilder validForNoSeconds = createObjectBuilderWithName("validUntil");
		validForNoSeconds.addKeyString(VALUE, String.valueOf(authToken.validUntil()));
		userChildren.addJsonObjectBuilder(validForNoSeconds);
	}

	private void addrenewUntilToJson(JsonArrayBuilder userChildren) {
		JsonObjectBuilder validForNoSeconds = createObjectBuilderWithName("renewUntil");
		validForNoSeconds.addKeyString(VALUE, String.valueOf(authToken.renewUntil()));
		userChildren.addJsonObjectBuilder(validForNoSeconds);
	}

	private void addIdInUserStorageToJson(JsonArrayBuilder userChildren) {
		JsonObjectBuilder idInUserStorage = createObjectBuilderWithName("idInUserStorage");
		idInUserStorage.addKeyString(VALUE, String.valueOf(authToken.idInUserStorage()));
		userChildren.addJsonObjectBuilder(idInUserStorage);
	}

	private void addLoginIdToJson(JsonArrayBuilder userChildren) {
		JsonObjectBuilder loginId = createObjectBuilderWithName("loginId");
		loginId.addKeyString(VALUE, String.valueOf(authToken.loginId()));
		userChildren.addJsonObjectBuilder(loginId);
	}

	private void possiblyAddNameToJson(JsonArrayBuilder userChildren) {
		possiblyAddFirstNameToJson(userChildren);
		possiblyAddLastNameToJson(userChildren);
	}

	private void possiblyAddFirstNameToJson(JsonArrayBuilder userChildren) {
		Optional<String> optionalFirstname = authToken.firstName();
		if (optionalFirstname.isPresent()) {
			JsonObjectBuilder firstName = createObjectBuilderWithName("firstName");
			firstName.addKeyString(VALUE, String.valueOf(optionalFirstname.get()));
			userChildren.addJsonObjectBuilder(firstName);
		}
	}

	private void possiblyAddLastNameToJson(JsonArrayBuilder userChildren) {
		Optional<String> optionalLastname = authToken.lastName();
		if (optionalLastname.isPresent()) {
			JsonObjectBuilder lastName = createObjectBuilderWithName("lastName");
			lastName.addKeyString(VALUE, String.valueOf(optionalLastname.get()));
			userChildren.addJsonObjectBuilder(lastName);
		}
	}

	private JsonArrayBuilder returnAndAddChildrenToBuilder(JsonObjectBuilder userBuilder) {
		JsonArrayBuilder userChildren = orgJsonBuilderFactoryAdapter.createArrayBuilder();
		userBuilder.addKeyJsonArrayBuilder(CHILDREN, userChildren);
		return userChildren;
	}

}
