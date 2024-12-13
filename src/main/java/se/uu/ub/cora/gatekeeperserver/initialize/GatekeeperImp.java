/*
 * Copyright 2016, 2017, 2022, 2024 Uppsala University Library
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
import java.util.Optional;
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

	private static final long VALID_UNTIL_NO_MILLIS = 600000L;
	private static final long RENEW_UNTIL_NO_MILLIS = 86400000L;
	private Map<String, Authentication> authentications = new HashMap<>();

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
		if (!authentications.containsKey(authToken)) {
			throw new AuthenticationException("token not valid");
		}
	}

	private User getAuthenticatedUser(String authToken) {
		Authentication authentication = authentications.get(authToken);
		return authentication.user();
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
		String generatedToken = generateRandomUUID();
		String generatedTokenId = generateRandomUUID();
		Authentication createdAuthentication = new Authentication(generatedTokenId, pickedUser);
		authentications.put(generatedToken, createdAuthentication);
		return createAuthTokenUsingPickedUserAndTokenAndTokenId(pickedUser, generatedToken,
				generatedTokenId);
	}

	private AuthToken createAuthTokenUsingPickedUserAndTokenAndTokenId(User pickedUser,
			String token, String tokenId) {
		long validUntil = System.currentTimeMillis() + VALID_UNTIL_NO_MILLIS;
		long renewUntil = System.currentTimeMillis() + RENEW_UNTIL_NO_MILLIS;
		return new AuthToken(token, tokenId, validUntil, renewUntil, pickedUser.id,
				pickedUser.loginId, Optional.ofNullable(pickedUser.firstName),
				Optional.ofNullable(pickedUser.lastName));
	}

	private String generateRandomUUID() {
		return UUID.randomUUID().toString();
	}

	@Override
	public void removeAuthToken(String tokenId, String token) {
		throwErrorIfAuthTokenDoesNotExists(token);
		removeAuthTokenIfUserIdMatches(token, tokenId);
	}

	private void throwErrorIfAuthTokenDoesNotExists(String token) {
		if (!authentications.containsKey(token)) {
			throw new AuthenticationException("AuthToken does not exist");
		}
	}

	private void removeAuthTokenIfUserIdMatches(String token, String tokenId) {
		ensureUserIdMatchesTokensUserId(token, tokenId);
		authentications.remove(token);
	}

	private void ensureUserIdMatchesTokensUserId(String token, String tokenId) {
		Authentication authentication = authentications.get(token);
		if (!tokenId.equals(authentication.tokenId())) {
			throw new AuthenticationException("TokenId does not exists");
		}
	}

	record Authentication(String tokenId, User user) {
	}
}
