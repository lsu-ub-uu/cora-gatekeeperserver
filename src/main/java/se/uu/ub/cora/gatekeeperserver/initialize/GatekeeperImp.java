/*
 * Copyright 2016, 2017, 2022 Uppsala University Library
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

package se.uu.ub.cora.gatekeeperserver.initialize;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import se.uu.ub.cora.gatekeeper.picker.UserInfo;
import se.uu.ub.cora.gatekeeper.picker.UserPicker;
import se.uu.ub.cora.gatekeeper.picker.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.Gatekeeper;
import se.uu.ub.cora.gatekeeperserver.authentication.AuthenticationException;
import se.uu.ub.cora.gatekeeperserver.tokenprovider.AuthToken;

public enum GatekeeperImp implements Gatekeeper {
	INSTANCE;

	private static final int VALID_FOR_NO_SECONDS = 600;
	private Map<String, User> pickedUsers = new HashMap<>();

	@Override
	public User getUserForToken(String authToken) {
		if (authToken == null) {
			return returnGuestUser();
		}
		return tryToGetAuthenticatedUser(authToken);
	}

	private User returnGuestUser() {
		UserPicker userPicker = UserPickerProvider.getUserPicker();
		return userPicker.pickGuest();
	}

	private User tryToGetAuthenticatedUser(String authToken) {
		throwErrorIfInvalidToken(authToken);
		return getAuthenticatedUser(authToken);
	}

	private void throwErrorIfInvalidToken(String authToken) {
		if (!pickedUsers.containsKey(authToken)) {
			throw new AuthenticationException("token not valid");
		}
	}

	private User getAuthenticatedUser(String authToken) {
		return pickedUsers.get(authToken);
	}

	@Override
	public AuthToken getAuthTokenForUserInfo(UserInfo userInfo) {
		try {
			return tryToGetAuthTokenForUserInfo(userInfo);
		} catch (Exception e) {
			throw new AuthenticationException("Could not pick user for userInfo, with error: " + e,
					e);
		}
	}

	private AuthToken tryToGetAuthTokenForUserInfo(UserInfo userInfo) {
		UserPicker userPicker = UserPickerProvider.getUserPicker();
		User pickedUser = userPicker.pickUser(userInfo);
		String generatedAuthToken = generateAuthToken();
		pickedUsers.put(generatedAuthToken, pickedUser);
		return createAuthTokenUsingPickedUserAndGeneratedAuthToken(pickedUser, generatedAuthToken);
	}

	private AuthToken createAuthTokenUsingPickedUserAndGeneratedAuthToken(User pickedUser,
			String generatedAuthToken) {
		AuthToken authToken = AuthToken.withTokenAndValidForNoSecondsAndIdInUserStorageAndLoginId(
				generatedAuthToken, VALID_FOR_NO_SECONDS, pickedUser.id, pickedUser.loginId);
		setNamesInAuthToken(pickedUser, authToken);
		return authToken;
	}

	private void setNamesInAuthToken(User pickedUser, AuthToken authToken) {
		if (pickedUser.firstName != null) {
			authToken.firstName = pickedUser.firstName;
		}
		if (pickedUser.lastName != null) {
			authToken.lastName = pickedUser.lastName;
		}
	}

	private String generateAuthToken() {
		return UUID.randomUUID().toString();
	}

	@Override
	public void removeAuthTokenForUser(String authTokenId, String loginId) {
		throwErrorIfAuthTokenDoesNotExists(authTokenId);
		removeAuthTokenIfUserIdMatches(authTokenId, loginId);
	}

	private void throwErrorIfAuthTokenDoesNotExists(String authTokenId) {
		if (!pickedUsers.containsKey(authTokenId)) {
			throw new AuthenticationException("AuthToken does not exist");
		}
	}

	private void removeAuthTokenIfUserIdMatches(String authTokenId, String loginId) {
		ensureUserIdMatchesTokensUserId(authTokenId, loginId);
		pickedUsers.remove(authTokenId);
	}

	private void ensureUserIdMatchesTokensUserId(String authTokenId, String loginId) {
		User storedUser = pickedUsers.get(authTokenId);
		if (!userInfoLoginIdEqualsStoredLoginId(loginId, storedUser)) {
			throw new AuthenticationException("idInUserStorage does not exist");
		}
	}

	private boolean userInfoLoginIdEqualsStoredLoginId(String loginId, User storedUser) {
		return storedUser.loginId.equals(loginId);
	}

}
