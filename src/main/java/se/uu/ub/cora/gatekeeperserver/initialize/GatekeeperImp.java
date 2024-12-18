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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
	private Map<String, Authentication> authentications = new ConcurrentHashMap<>();

	@Override
	public User getUserForToken(String token) {
		if (token == null) {
			return returnGuestUser();
		}
		return tryToGetAuthenticatedUser(token);
	}

	private User returnGuestUser() {
		UserPicker userPicker = UserPickerProvider.getUserPicker();
		return userPicker.pickGuest();
	}

	private User tryToGetAuthenticatedUser(String token) {
		throwErrorIfInvalidToken(token);
		return getAuthenticatedUser(token);
	}

	private void throwErrorIfInvalidToken(String token) {
		if (authenticationDoNotExistsOrInvalid(token)) {
			throw createExceptionTokenNotValid();
		}
	}

	private AuthenticationException createExceptionTokenNotValid() {
		return new AuthenticationException("Token not valid");
	}

	private boolean authenticationDoNotExistsOrInvalid(String token) {
		return !authentications.containsKey(token) || !authenticationValid(token);
	}

	boolean authenticationValid(String token) {
		Authentication authentication = authentications.get(token);
		return authenticationIsValid(authentication);
	}

	private boolean authenticationIsValid(Authentication authentication) {
		long currentTimestamp = System.currentTimeMillis();
		return currentTimestamp <= authentication.validUntil();
	}

	private User getAuthenticatedUser(String token) {
		Authentication authentication = authentications.get(token);
		return authentication.user();
	}

	@Override
	public AuthToken getAuthTokenForUserInfo(UserInfo userInfo) {
		removeNoLongerValidAuthentications();
		try {
			return tryToGetAuthTokenForUserInfo(userInfo);
		} catch (Exception e) {
			throw new AuthenticationException("Could not pick user for userInfo, with error: " + e,
					e);
		}
	}

	void removeNoLongerValidAuthentications() {
		for (Entry<String, Authentication> entry : authentications.entrySet()) {
			removeAuthenticationIfNoLongerValid(entry);
		}
	}

	private void removeAuthenticationIfNoLongerValid(Entry<String, Authentication> entry) {
		if (!authenticationIsValid(entry.getValue())) {
			authentications.remove(entry.getKey());
		}
	}

	private AuthToken tryToGetAuthTokenForUserInfo(UserInfo userInfo) {
		String generatedToken = generateRandomUUID();
		String generatedTokenId = generateRandomUUID();
		Authentication createdAuthentication = createNewAuthentication(generatedTokenId, userInfo);
		authentications.put(generatedToken, createdAuthentication);
		return createAuthToken(generatedToken, createdAuthentication);
	}

	private Authentication createNewAuthentication(String tokenId, UserInfo userInfo) {
		User pickedUser = pickUser(userInfo);
		long currentTime = System.currentTimeMillis();
		long validUntil = currentTime + VALID_UNTIL_NO_MILLIS;
		long renewUntil = currentTime + RENEW_UNTIL_NO_MILLIS;
		return new Authentication(tokenId, pickedUser, validUntil, renewUntil);
	}

	private User pickUser(UserInfo userInfo) {
		UserPicker userPicker = UserPickerProvider.getUserPicker();
		return userPicker.pickUser(userInfo);
	}

	private AuthToken createAuthToken(String token, Authentication authentication) {
		User user = authentication.user();
		return new AuthToken(token, authentication.tokenId(), authentication.validUntil(),
				authentication.renewUntil(), user.id, user.loginId,
				Optional.ofNullable(user.firstName), Optional.ofNullable(user.lastName));
	}

	private String generateRandomUUID() {
		return UUID.randomUUID().toString();
	}

	@Override
	public void removeAuthToken(String tokenId, String token) {
		throwErrorIfAuthTokenDoesNotExists(token);
		removeAuthTokenIfUserIdMatches(tokenId, token);
	}

	private void throwErrorIfAuthTokenDoesNotExists(String token) {
		if (!authentications.containsKey(token)) {
			throw createExceptionTokenNotValid();
		}
	}

	private void removeAuthTokenIfUserIdMatches(String tokenId, String token) {
		ensureUserIdMatchesTokensUserId(tokenId, token);
		authentications.remove(token);
	}

	private void ensureUserIdMatchesTokensUserId(String tokenId, String token) {
		Authentication authentication = authentications.get(token);
		if (!tokenId.equals(authentication.tokenId())) {
			throw createExceptionTokenNotValid();
		}
	}

	void onlyForTestSetAuthentication(String token, Authentication authentication) {
		authentications.put(token, authentication);

	}

	@Override
	public AuthToken renewAuthToken(String tokenId, String oldToken) {
		throwErrorIfInvalidToken(oldToken);
		ensureUserIdMatchesTokensUserId(tokenId, oldToken);
		ensureRenewUntilHasNotPassed(oldToken);
		String newToken = generateRandomUUID();
		Authentication newAuthentication = replaceOldToNewAuthentication(oldToken, newToken);
		return createAuthToken(newToken, newAuthentication);
	}

	private Authentication replaceOldToNewAuthentication(String token, String newToken) {
		Authentication authentication = authentications.get(token);
		Authentication newAuthentication = renewAuthentication(authentication);
		synchronizeAuthentications(token, newToken, newAuthentication);
		return newAuthentication;
	}

	private void synchronizeAuthentications(String token, String newToken,
			Authentication newAuthentication) {
		authentications.remove(token);
		authentications.put(newToken, newAuthentication);
	}

	private Authentication renewAuthentication(Authentication authentication) {
		long currentTime = System.currentTimeMillis();
		long validUntil = currentTime + VALID_UNTIL_NO_MILLIS;
		return new Authentication(authentication.tokenId(), authentication.user(), validUntil,
				authentication.renewUntil());
	}

	void ensureRenewUntilHasNotPassed(String token) {
		if (!authenticationCanBeRenewed(token)) {
			throw createExceptionTokenNotValid();
		}
	}

	boolean authenticationCanBeRenewed(String token) {
		Authentication authentication = authentications.get(token);
		long currentTimestamp = System.currentTimeMillis();
		return currentTimestamp <= authentication.renewUntil();
	}

	void onlyForTestEmptyAuthentications() {
		authentications = new HashMap<>();
	}

	Map<String, Authentication> onlyForTestGetAuthentications() {
		return authentications;
	}
}
