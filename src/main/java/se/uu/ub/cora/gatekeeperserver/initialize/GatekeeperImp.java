/*
 * Copyright 2016, 2017, 2022, 2024, 2025 Uppsala University Library
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
	private Map<String, ActiveTokenForUser> activeTokens = new ConcurrentHashMap<>();

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
		if (tokenDoNotExistsOrInvalid(token)) {
			throw createExceptionTokenNotValid();
		}
	}

	private AuthenticationException createExceptionTokenNotValid() {
		return new AuthenticationException("Token not valid");
	}

	private boolean tokenDoNotExistsOrInvalid(String token) {
		return !activeTokens.containsKey(token) || !checkTokenIsValid(token);
	}

	boolean checkTokenIsValid(String token) {
		ActiveTokenForUser activeTokenForUser = activeTokens.get(token);
		return activeTokenForUserIsValid(activeTokenForUser);
	}

	private boolean activeTokenForUserIsValid(ActiveTokenForUser activeTokenForUser) {
		long currentTimestamp = System.currentTimeMillis();
		return currentTimestamp <= activeTokenForUser.validUntil();
	}

	private User getAuthenticatedUser(String token) {
		ActiveTokenForUser authentication = activeTokens.get(token);
		return authentication.user();
	}

	@Override
	public AuthToken getAuthTokenForUserInfo(UserInfo userInfo) {
		removeNoLongerValidActiveToken();
		try {
			return tryToGetAuthTokenForUserInfo(userInfo);
		} catch (Exception e) {
			throw new AuthenticationException("Could not pick user for userInfo, with error: " + e,
					e);
		}
	}

	void removeNoLongerValidActiveToken() {
		for (Entry<String, ActiveTokenForUser> entry : activeTokens.entrySet()) {
			removeActiveTokenIfNoLongerValid(entry);
		}
	}

	private void removeActiveTokenIfNoLongerValid(Entry<String, ActiveTokenForUser> entry) {
		if (!activeTokenForUserIsValid(entry.getValue())) {
			activeTokens.remove(entry.getKey());
		}
	}

	private AuthToken tryToGetAuthTokenForUserInfo(UserInfo userInfo) {
		String generatedToken = generateRandomUUID();
		String generatedTokenId = generateRandomUUID();
		ActiveTokenForUser activeToken = createActiveTokenForUser(generatedTokenId, userInfo);
		activeTokens.put(generatedToken, activeToken);
		return createAuthToken(generatedToken, activeToken);
	}

	private ActiveTokenForUser createActiveTokenForUser(String tokenId, UserInfo userInfo) {
		User pickedUser = pickUser(userInfo);
		long currentTime = System.currentTimeMillis();
		long validUntil = currentTime + VALID_UNTIL_NO_MILLIS;
		long renewUntil = currentTime + RENEW_UNTIL_NO_MILLIS;
		return new ActiveTokenForUser(tokenId, pickedUser, validUntil, renewUntil);
	}

	private User pickUser(UserInfo userInfo) {
		UserPicker userPicker = UserPickerProvider.getUserPicker();
		return userPicker.pickUser(userInfo);
	}

	private AuthToken createAuthToken(String token, ActiveTokenForUser activeTokenForUser) {
		User user = activeTokenForUser.user();
		return new AuthToken(token, activeTokenForUser.tokenId(), activeTokenForUser.validUntil(),
				activeTokenForUser.renewUntil(), user.id, user.loginId,
				Optional.ofNullable(user.firstName), Optional.ofNullable(user.lastName),
				user.permissionUnitIds);
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
		if (!activeTokens.containsKey(token)) {
			throw createExceptionTokenNotValid();
		}
	}

	private void removeAuthTokenIfUserIdMatches(String tokenId, String token) {
		ensureUserIdMatchesTokensUserId(tokenId, token);
		activeTokens.remove(token);
	}

	private void ensureUserIdMatchesTokensUserId(String tokenId, String token) {
		ActiveTokenForUser authentication = activeTokens.get(token);
		if (!tokenId.equals(authentication.tokenId())) {
			throw createExceptionTokenNotValid();
		}
	}

	void onlyForTestSetActiveTokenForUser(String token, ActiveTokenForUser activeTokenForUser) {
		activeTokens.put(token, activeTokenForUser);

	}

	@Override
	public AuthToken renewAuthToken(String tokenId, String oldToken) {
		throwErrorIfInvalidToken(oldToken);
		ensureUserIdMatchesTokensUserId(tokenId, oldToken);
		ensureRenewUntilHasNotPassed(oldToken);
		String newToken = generateRandomUUID();
		ActiveTokenForUser newAuthentication = replaceOldToNewAuthentication(oldToken, newToken);
		return createAuthToken(newToken, newAuthentication);
	}

	private ActiveTokenForUser replaceOldToNewAuthentication(String token, String newToken) {
		ActiveTokenForUser activeTokenForUser = activeTokens.get(token);
		ActiveTokenForUser newAuthentication = renewAuthentication(activeTokenForUser);
		storeNewAuthentication(newToken, newAuthentication);
		return newAuthentication;
	}

	private ActiveTokenForUser renewAuthentication(ActiveTokenForUser activeTokenForUser) {
		long currentTime = System.currentTimeMillis();
		long validUntil = currentTime + VALID_UNTIL_NO_MILLIS;
		return new ActiveTokenForUser(activeTokenForUser.tokenId(), activeTokenForUser.user(),
				validUntil, activeTokenForUser.renewUntil());
	}

	private void storeNewAuthentication(String newToken, ActiveTokenForUser newAuthentication) {
		activeTokens.put(newToken, newAuthentication);
	}

	void ensureRenewUntilHasNotPassed(String token) {
		if (!tokenCanBeRenewed(token)) {
			throw createExceptionTokenNotValid();
		}
	}

	boolean tokenCanBeRenewed(String token) {
		ActiveTokenForUser activeTokenForUser = activeTokens.get(token);
		long currentTimestamp = System.currentTimeMillis();
		return currentTimestamp <= activeTokenForUser.renewUntil();
	}

	void onlyForTestEmptyAuthentications() {
		activeTokens = new HashMap<>();
	}

	Map<String, ActiveTokenForUser> onlyForTestGetActiveTokens() {
		return activeTokens;
	}
}
