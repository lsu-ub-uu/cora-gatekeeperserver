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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
	private Map<String, ActiveUser> activeUsers = new ConcurrentHashMap<>();

	// TODO: create getGuestUser method, instead of using getUseForToken(null)
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
		return activeUsers.get(authentication.loginId()).user;
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

	synchronized void removeNoLongerValidActiveToken() {
		for (Entry<String, ActiveTokenForUser> activeTokenEntry : activeTokens.entrySet()) {
			removeActiveTokenIfNoLongerValid(activeTokenEntry);
		}
	}

	private void removeActiveTokenIfNoLongerValid(
			Entry<String, ActiveTokenForUser> activeTokenEntry) {
		if (!activeTokenForUserIsValid(activeTokenEntry.getValue())) {
			ActiveTokenForUser activeToken = activeTokens.get(activeTokenEntry.getKey());
			activeTokens.remove(activeTokenEntry.getKey());
			ActiveUser activeUser = activeUsers.get(activeToken.loginId());
			removeActiveUser(activeToken.loginId(), activeUser);
		}
	}

	private AuthToken tryToGetAuthTokenForUserInfo(UserInfo userInfo) {
		String generatedToken = generateRandomUUID();
		String generatedTokenId = generateRandomUUID();
		User pickedUser = pickUser(userInfo);
		ActiveTokenForUser activeToken = createActiveTokenForUser(generatedTokenId,
				pickedUser.loginId);
		activeTokens.put(generatedToken, activeToken);

		addActiveUser(pickedUser);
		return generateAuthToken(generatedToken, activeToken);
	}

	private void addActiveUser(User user) {
		activeUsers.compute(user.loginId, (_, existingUser) -> {
			if (existingUser != null) {
				existingUser.incrementAnGet();
				return existingUser;
			}
			return new ActiveUser(user);
		});
	}

	private ActiveTokenForUser createActiveTokenForUser(String tokenId, String userId) {
		long currentTime = System.currentTimeMillis();
		long validUntil = currentTime + VALID_UNTIL_NO_MILLIS;
		long renewUntil = currentTime + RENEW_UNTIL_NO_MILLIS;
		return new ActiveTokenForUser(tokenId, userId, validUntil, renewUntil);
	}

	private User pickUser(UserInfo userInfo) {
		UserPicker userPicker = UserPickerProvider.getUserPicker();
		return userPicker.pickUser(userInfo);
	}

	private AuthToken generateAuthToken(String token, ActiveTokenForUser activeTokenForUser) {
		String loginId = activeTokenForUser.loginId();
		User user = activeUsers.get(loginId).user;
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
		removeActiveTokenAndUser(token);
	}

	private void removeActiveTokenAndUser(String token) {
		ActiveTokenForUser activeToken = activeTokens.get(token);
		activeTokens.remove(token);
		ActiveUser activeUser = activeUsers.get(activeToken.loginId());
		removeActiveUser(activeToken.loginId(), activeUser);
	}

	private void removeActiveUser(String loginIdFromToken, ActiveUser activeUser) {

		if (activeUser.counter.get() > 1) {
			activeUser.decrementAndGet();
		} else {
			activeUsers.remove(loginIdFromToken);
		}
	}

	private void ensureUserIdMatchesTokensUserId(String tokenId, String token) {
		ActiveTokenForUser authentication = activeTokens.get(token);
		if (!tokenId.equals(authentication.tokenId())) {
			throw createExceptionTokenNotValid();
		}
	}

	void onlyForTestSetActiveTokenAndActiveUsers(String token, ActiveTokenForUser activeToken,
			User activeUser) {
		activeTokens.put(token, activeToken);
		addActiveUser(activeUser);
	}

	@Override
	public AuthToken renewAuthToken(String tokenId, String oldToken) {
		throwErrorIfInvalidToken(oldToken);
		ensureUserIdMatchesTokensUserId(tokenId, oldToken);
		ensureRenewUntilHasNotPassed(oldToken);
		String newToken = generateRandomUUID();
		ActiveTokenForUser newAuthentication = replaceOldToNewAuthentication(oldToken, newToken);
		activeUsers.get(newAuthentication.loginId()).incrementAnGet();
		return generateAuthToken(newToken, newAuthentication);
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
		return new ActiveTokenForUser(activeTokenForUser.tokenId(), activeTokenForUser.loginId(),
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

	@Override
	public void dataChanged(String type, String id, String action) {
		// TODO: Vi behöver hantera fallet om loginId ändras i användare. Då behöver man byta
		// loginId i alla activeTokens och i key för activeUsers (eller kan vi lösa problemet på ett
		// annat sett)
		if ("user".equals(type) && "update".equals(action)) {
			updateRelatedUsersDataFromStorage(id);
		}

		if ("user".equals(type) && "delete".equals(action)) {
			deleteRelatedUsersFromCache(id);
		}
	}

	private void deleteRelatedUsersFromCache(String id) {
		Iterator<ActiveUser> iterator = activeUsers.values().iterator();
		while (iterator.hasNext()) {
			possiblyDeleteUserFromCache(id, iterator.next(), iterator);
		}
	}

	private void possiblyDeleteUserFromCache(String id, ActiveUser activeUser,
			Iterator<ActiveUser> iterator) {
		String activeUserId = activeUser.user.id;
		if (activeUserId.equals(id)) {
			deleteUserFromCache(activeUser, iterator);
		}
	}

	private void deleteUserFromCache(ActiveUser activeUser, Iterator<ActiveUser> iterator) {
		String activeUserLoginId = activeUser.user.loginId;
		activeTokens.values()
				.removeIf(activeToken -> activeToken.loginId().equals(activeUserLoginId));
		iterator.remove();
	}

	private void updateRelatedUsersDataFromStorage(String id) {
		Optional<ActiveUser> foundActiveUser = findActiveUserWithId(id);
		if (foundActiveUser.isPresent()) {
			handleUpdateForActiveUser(id, foundActiveUser.get());
		}
	}

	private Optional<ActiveUser> findActiveUserWithId(String id) {
		for (ActiveUser activeUser : activeUsers.values()) {
			String activeUserId1 = activeUser.user.id;
			if (activeUserId1.equals(id)) {
				return Optional.of(activeUser);
			}
		}
		return Optional.empty();
	}

	private void handleUpdateForActiveUser(String id, ActiveUser activeUser) {
		User pickedUser = pickUserUsingId(id);
		if (activeUserIdDifferentThanUserPickedFromStorage(id, pickedUser.id)) {
			deleteRelatedUsersFromCache(id);
		} else {
			activeUser.user = pickedUser;
		}
	}

	private boolean activeUserIdDifferentThanUserPickedFromStorage(String id,
			String idFromPickedUser) {
		return !id.equals(idFromPickedUser);
	}

	private User pickUserUsingId(String id) {
		UserPicker userPicker = UserPickerProvider.getUserPicker();
		UserInfo userInfo = UserInfo.withIdInUserStorage(id);
		return userPicker.pickUser(userInfo);
	}

	void onlyForTestEmptyAuthentications() {
		activeTokens = new HashMap<>();
		activeUsers = new HashMap<>();
	}

	Map<String, ActiveTokenForUser> onlyForTestGetActiveTokens() {
		return activeTokens;
	}

	Map<String, ActiveUser> onlyForTestGetActiveUsers() {
		return activeUsers;
	}

	class ActiveUser {
		User user;
		private AtomicInteger counter;

		public ActiveUser(User user) {
			this.user = user;
			this.counter = new AtomicInteger(1);
		}

		public int incrementAnGet() {
			return counter.incrementAndGet();
		}

		public int decrementAndGet() {
			return counter.decrementAndGet();
		}

		public int getNumberActiveTokens() {
			return counter.get();
		}
	}
}
