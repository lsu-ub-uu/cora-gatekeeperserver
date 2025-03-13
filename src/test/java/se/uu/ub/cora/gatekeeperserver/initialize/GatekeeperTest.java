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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.gatekeeper.picker.UserInfo;
import se.uu.ub.cora.gatekeeper.picker.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.authentication.AuthenticationException;
import se.uu.ub.cora.gatekeeperserver.initialize.GatekeeperImp.ActiveUser;
import se.uu.ub.cora.gatekeeperserver.tokenprovider.AuthToken;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;

public class GatekeeperTest {
	private static final String TOKEN = "someToken";
	private static final String TOKEN_ID = "someTokenId";
	private static final String USER_RECORD_TYPE = "user";
	private static final long THIRTY_MINUTES = 1800000L;
	private static final long PRECISION = 5000L;
	private static final long VALID_UNTIL_NO_MILLIS = 600000L;
	private static final long RENEW_UNTIL_NO_MILLIS = 86400000L;
	private UserPickerInstanceProviderSpy userPickerInstanceProvider;
	private GatekeeperImp gatekeeper;
	private LoggerFactorySpy loggerFactory;
	private UserInfo userInfo;
	private User userElly;
	private User userRudolf;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		userPickerInstanceProvider = new UserPickerInstanceProviderSpy();
		UserPickerProvider.onlyForTestSetUserPickerInstanceProvider(userPickerInstanceProvider);
		userInfo = UserInfo.withLoginIdAndLoginDomain("someLoginId", "someLoginDomain");
		userElly = createUserForElly();
		userRudolf = createUserForRudolf();

		gatekeeper = GatekeeperImp.INSTANCE;
	}

	@AfterMethod
	public void afterMethod() {
		gatekeeper.onlyForTestEmptyAuthentications();
	}

	private User createUserForElly() {
		return createGenericUser("Elly");
	}

	private User createUserForRudolf() {
		return createGenericUser("Rudolf");
	}

	private User createGenericUser(String name) {
		User aUser = new User("some" + name + "UserId");
		aUser.firstName = "some" + name + "FirstName";
		aUser.lastName = "some" + name + "LastName";
		aUser.loginId = "some" + name + "LoginId";
		return aUser;
	}

	@Test
	public void testEnum() {
		// small hack to get 100% coverage on enum
		GatekeeperImp.valueOf(GatekeeperImp.INSTANCE.toString());
	}

	@Test
	public void testNoTokenAlsoKnownAsGuest() {
		User logedInUser = gatekeeper.getUserForToken(null);

		assertGuestWasPicked(logedInUser);
	}

	private void assertGuestWasPicked(User logedInUser) {
		UserPickerSpy userPicker = assertAndReturnUserPickerProviderWasUsedToGetUserPicker();
		userPicker.MCR.assertMethodWasCalled("pickGuest");
		userPicker.MCR.assertReturn("pickGuest", 0, logedInUser);
	}

	private UserPickerSpy assertAndReturnUserPickerProviderWasUsedToGetUserPicker() {
		userPickerInstanceProvider.MCR.assertMethodWasCalled("getUserPicker");
		return getUserPickerSpy();
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testNonAuthenticatedUser() {
		gatekeeper.getUserForToken("dummyNonAuthenticatedToken");
	}

	@Test
	public void testGetAuthTokenForUserInfo() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);

		User pickedUser = assertAndReturnPickUserWasUsed(userInfo);

		assertTokenHasUUIDFormat(authToken.token());
		assertTokenHasUUIDFormat(authToken.tokenId());
		assertTimestamp(authToken.validUntil(), VALID_UNTIL_NO_MILLIS);
		assertTimestamp(authToken.renewUntil(), RENEW_UNTIL_NO_MILLIS);
		assertSame(authToken.idInUserStorage(), pickedUser.id);
		assertSame(authToken.loginId(), pickedUser.loginId);
		assertSame(authToken.firstName().get(), pickedUser.firstName);
		assertSame(authToken.lastName().get(), pickedUser.lastName);
		assertSame(authToken.permissionUnits(), pickedUser.permissionUnitIds);
	}

	private void assertTimestamp(long timestampToValidate, long extraMillis) {
		long expectedTimestamp = extraMillis + System.currentTimeMillis();
		long expectedTimestampMinusPresicion = expectedTimestamp - PRECISION;
		boolean evaluation = (expectedTimestampMinusPresicion <= timestampToValidate)
				&& (timestampToValidate <= expectedTimestamp);
		assertTrue(evaluation);
	}

	private User assertAndReturnPickUserWasUsed(UserInfo userInfo) {
		UserPickerSpy userPicker = assertAndReturnUserPickerProviderWasUsedToGetUserPicker();
		userPicker.MCR.assertMethodWasCalled("pickUser");
		userPicker.MCR.assertParameters("pickUser", 0, userInfo);
		return (User) userPicker.MCR.getReturnValue("pickUser", 0);
	}

	@Test
	public void testGetAuthTokenForUserInfo_noNames() {
		User user = new User(USER_RECORD_TYPE);
		user.loginId = "loginId";
		setUserToReturnFromUserProviderSpy(user);

		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);

		User pickedUser = assertAndReturnPickUserWasUsed(userInfo);
		assertTokenHasUUIDFormat(authToken.token());
		assertTokenHasUUIDFormat(authToken.tokenId());
		assertTimestamp(authToken.validUntil(), VALID_UNTIL_NO_MILLIS);
		assertTimestamp(authToken.renewUntil(), RENEW_UNTIL_NO_MILLIS);
		assertSame(authToken.idInUserStorage(), pickedUser.id);
		assertSame(authToken.loginId(), pickedUser.loginId);
		assertTrue(authToken.firstName().isEmpty());
		assertTrue(authToken.lastName().isEmpty());
		assertSame(authToken.permissionUnits(), pickedUser.permissionUnitIds);
	}

	private void assertTokenHasUUIDFormat(String token) {
		String regex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(token);
		assertTrue(matcher.matches());
	}

	private void setUserToReturnFromUserProviderSpy(User user) {
		UserPickerSpy userPicker = new UserPickerSpy();
		userPicker.MRV.setDefaultReturnValuesSupplier("pickUser", () -> user);
		userPickerInstanceProvider.MRV.setDefaultReturnValuesSupplier("getUserPicker",
				() -> userPicker);
	}

	@Test
	public void testGetAuthTokenWithProblem() {
		RuntimeException errorToThrow = new RuntimeException();
		userPickerInstanceProvider.MRV.setAlwaysThrowException("getUserPicker", errorToThrow);
		UserInfo specificUserInfo = UserInfo.withLoginIdAndLoginDomain("someLoginIdWithProblem",
				"someLoginDomain");
		try {
			gatekeeper.getAuthTokenForUserInfo(specificUserInfo);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof AuthenticationException);
			assertEquals(e.getMessage(),
					"Could not pick user for userInfo, with error: java.lang.RuntimeException");
			assertSame(e.getCause(), errorToThrow);
		}
	}

	@Test
	public void testGetAuthTokenWithProblem_pickUserFails() {
		RuntimeException errorToThrow = new RuntimeException("errorFromPickUser");
		UserPickerSpy userPicker = new UserPickerSpy();
		userPicker.MRV.setAlwaysThrowException("pickUser", errorToThrow);
		userPickerInstanceProvider.MRV.setDefaultReturnValuesSupplier("getUserPicker",
				() -> userPicker);
		UserInfo.withLoginIdAndLoginDomain("someLoginIdWithProblem", "someLoginDomain");
		try {
			gatekeeper.getAuthTokenForUserInfo(userInfo);
			fail("it should throw AuthenticationException");
		} catch (Exception e) {
			assertTrue(e instanceof AuthenticationException);
			assertEquals(e.getMessage(), "Could not pick user for userInfo, with error: "
					+ "java.lang.RuntimeException: errorFromPickUser");
			assertSame(e.getCause(), errorToThrow);
		}
	}

	@Test
	public void testGetAuthTokenForUserInfo_canReturnCorrectUserForReturnedToken() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);

		User pickedUser = assertAndReturnPickUserWasUsed(userInfo);
		User logedInUser = gatekeeper.getUserForToken(authToken.token());
		assertSame(logedInUser, pickedUser);
	}

	@Test
	public void testOldTokensRemovedWhenCreatingANew() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilInThePast(
				userElly.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(TOKEN, activeTokenForUser, userElly);
		assertEquals(gatekeeper.onlyForTestGetActiveTokens().size(), 1);
		assertTrue(gatekeeper.onlyForTestGetActiveTokens().containsKey(TOKEN));

		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);

		assertEquals(gatekeeper.onlyForTestGetActiveTokens().size(), 1);
		assertTrue(gatekeeper.onlyForTestGetActiveTokens().containsKey(authToken.token()));
		assertFalse(gatekeeper.onlyForTestGetActiveTokens().containsKey(TOKEN));

	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testgetUserForTokenNotValid() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilInThePast(
				userElly.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(TOKEN, activeTokenForUser, userElly);

		gatekeeper.getUserForToken(TOKEN);
	}

	@Test
	public void testMultipleLoginsReturnsDiferentTokens() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		AuthToken authToken2 = gatekeeper.getAuthTokenForUserInfo(userInfo);

		assertNotEquals(authToken.token(), authToken2.token());
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testRemoveAuthTokenForUserTokenDoesNotExist() {
		gatekeeper.removeAuthToken("someLoginId", "someNonExistingToken");
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testRemoveAuthTokenForUserFailsIfWrongUserId() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		gatekeeper.getUserForToken(authToken.token());
		gatekeeper.removeAuthToken("anotherTokenId", authToken.token());
	}

	@Test
	public void testRemoveAuthTokenForUser_removesAccess() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);

		gatekeeper.removeAuthToken(authToken.tokenId(), authToken.token());
		try {
			gatekeeper.getUserForToken(authToken.token());
			fail("should throw exception");
		} catch (AuthenticationException e) {
			assertTrue(e instanceof AuthenticationException);
			assertEquals(e.getMessage(), "Token not valid");
			assertTrue(gatekeeper.onlyForTestGetActiveTokens().isEmpty());
			assertTrue(gatekeeper.onlyForTestGetActiveUsers().isEmpty());
		}
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testRenewAuthTokenTokenIdAndTokenDoNotExist() {
		gatekeeper.renewAuthToken(TOKEN_ID, TOKEN);
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testRenewAuthTokenTokenIdDoesNotExists() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		gatekeeper.renewAuthToken("anotherTokenId", authToken.token());
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testRenewAuthTokenTokenDoesNotExists() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		gatekeeper.renewAuthToken(authToken.tokenId(), "anotherToken");
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testRenewUntilAuthTokenAfterValidUntil() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilInThePast(
				userElly.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(TOKEN, activeTokenForUser, userElly);

		gatekeeper.renewAuthToken(activeTokenForUser.tokenId(), TOKEN);
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Token not valid")
	public void testRenewUntilAuthTokenAfterRenewUntil() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserRenewUntilInThePast(
				userElly.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(TOKEN, activeTokenForUser, userElly);

		gatekeeper.renewAuthToken(activeTokenForUser.tokenId(), TOKEN);
	}

	@Test
	public void testRenewAuthTokenSetCorrectData() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilAndRenewUntilInTheFuture(
				userElly.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(TOKEN, activeTokenForUser, userElly);

		AuthToken newAuthToken = gatekeeper.renewAuthToken(activeTokenForUser.tokenId(), TOKEN);

		assertEquals(newAuthToken.tokenId(), activeTokenForUser.tokenId());
		assertNotEquals(newAuthToken.token(), TOKEN);
		assertTokenHasUUIDFormat(newAuthToken.token());
		assertNotEquals(newAuthToken.validUntil(), activeTokenForUser.validUntil());
		assertTimestamp(newAuthToken.validUntil(), VALID_UNTIL_NO_MILLIS);
		assertEquals(newAuthToken.renewUntil(), activeTokenForUser.renewUntil());
		assertEquals(newAuthToken.idInUserStorage(), userElly.id);
		assertEquals(newAuthToken.loginId(), userElly.loginId);
		assertEquals(newAuthToken.firstName().get(), userElly.firstName);
		assertEquals(newAuthToken.lastName().get(), userElly.lastName);
	}

	@Test
	public void testRenewAuthTokenOldTokenShouldStillWork() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilAndRenewUntilInTheFuture(
				userElly.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(TOKEN, activeTokenForUser, userElly);

		AuthToken renewedAuthToken = gatekeeper.renewAuthToken(activeTokenForUser.tokenId(), TOKEN);

		String oldToken = TOKEN;
		User userForToken = gatekeeper.getUserForToken(oldToken);
		User userForToken2 = gatekeeper.getUserForToken(renewedAuthToken.token());

		assertEquals(userForToken.loginId, userForToken2.loginId);
	}

	@Test
	public void testRenewTokenIsAbleToAuthenticateWithNewAuthToken() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilAndRenewUntilInTheFuture(
				userElly.loginId);
		String oldToken = TOKEN;
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(oldToken, activeTokenForUser, userElly);

		AuthToken newAuthToken = gatekeeper.renewAuthToken(activeTokenForUser.tokenId(), oldToken);

		gatekeeper.getUserForToken(newAuthToken.token());
	}

	@Test
	public void testDataChanged_create() {
		gatekeeper.dataChanged(USER_RECORD_TYPE, "someUserId", "create");

		userPickerInstanceProvider.MCR.assertMethodNotCalled("getUserPicker");
	}

	private UserPickerSpy getUserPickerSpy() {
		return (UserPickerSpy) userPickerInstanceProvider.MCR.getReturnValue("getUserPicker", 0);
	}

	@Test
	public void testDataChanged_update_notFound() {
		setupAnActiveUser(TOKEN, userElly);

		gatekeeper.dataChanged(USER_RECORD_TYPE, "someUserId", "update");

		userPickerInstanceProvider.MCR.assertMethodNotCalled("getUserPicker");
	}

	@Test
	public void testDataChanged_update_wrongRecordType() {
		setupAnActiveUser(TOKEN, userElly);

		gatekeeper.dataChanged("someRecordType", userElly.id, "update");

		userPickerInstanceProvider.MCR.assertMethodNotCalled("getUserPicker");

	}

	@Test
	public void testDataChanged_update_found() {
		setupAnActiveUser(TOKEN, userElly);

		gatekeeper.dataChanged(USER_RECORD_TYPE, userElly.id, "update");

		userPickerInstanceProvider.MCR.assertMethodWasCalled("getUserPicker");
		UserPickerSpy userPickerSpy = getUserPickerSpy();
		userPickerSpy.MCR.assertMethodWasCalled("pickUser");

		var userInfoToGetUpdatedUser = (UserInfo) userPickerSpy.MCR
				.getParameterForMethodAndCallNumberAndParameter("pickUser", 0, "userInfo");
		assertEquals(userInfoToGetUpdatedUser.idInUserStorage, userElly.id);

		var updatedUserFromStorage = userPickerSpy.MCR.getReturnValue("pickUser", 0);

		Map<String, ActiveUser> activeUsers = gatekeeper.onlyForTestGetActiveUsers();
		User userInCache = activeUsers.get(userElly.loginId).user;
		assertSame(userInCache, updatedUserFromStorage);

	}

	@Test
	public void testDataChanged_delete_notFound() {
		setupAnActiveUser(TOKEN, userElly);

		gatekeeper.dataChanged(USER_RECORD_TYPE, "someUserId", "delete");

		Map<String, ActiveUser> activeUsers = gatekeeper.onlyForTestGetActiveUsers();
		assertTrue(activeUsers.containsKey(userElly.loginId));
	}

	@Test
	public void testDataChanged_delete_found() {
		setupAnActiveUser(TOKEN, userElly);

		try {
			gatekeeper.dataChanged(USER_RECORD_TYPE, userElly.id, "delete");
			gatekeeper.getUserForToken(TOKEN);
			fail();
		} catch (AuthenticationException e) {
			assertActiveUserAndTokenHaveBeenRemovedFromGatekeeper(e);
		}
	}

	@Test
	public void testMultipleLogInAndDataChanges() {
		setupAnActiveUser("tokenElly_1", userElly);
		setupAnActiveUser("tokenElly_2", userElly);

		setupAnActiveUser("tokenRudolf_1", userRudolf);
		setupAnActiveUser("tokenRudolf_2", userRudolf);

		assertSame(gatekeeper.getUserForToken("tokenRudolf_1"), userRudolf);
		assertSame(gatekeeper.getUserForToken("tokenRudolf_2"), userRudolf);
		assertSame(gatekeeper.getUserForToken("tokenElly_1"), userElly);
		assertSame(gatekeeper.getUserForToken("tokenElly_2"), userElly);
		assertEquals(gatekeeper.onlyForTestGetActiveTokens().size(), 4);
		assertEquals(gatekeeper.onlyForTestGetActiveUsers().size(), 2);

		gatekeeper.dataChanged(USER_RECORD_TYPE, userRudolf.id, "update");

		assertNotSame(gatekeeper.getUserForToken("tokenRudolf_1"), userRudolf);
		assertNotSame(gatekeeper.getUserForToken("tokenRudolf_2"), userRudolf);
		assertEquals(gatekeeper.getUserForToken("tokenRudolf_1").id, "someSomeUserUserId");
		assertEquals(gatekeeper.getUserForToken("tokenRudolf_2").id, "someSomeUserUserId");
		assertEquals(gatekeeper.onlyForTestGetActiveTokens().size(), 4);
		assertEquals(gatekeeper.onlyForTestGetActiveUsers().size(), 2);

		gatekeeper.removeAuthToken(TOKEN_ID, "tokenRudolf_2");

		assertEquals(gatekeeper.onlyForTestGetActiveTokens().size(), 3);
		assertEquals(gatekeeper.onlyForTestGetActiveUsers().size(), 2);

		gatekeeper.removeAuthToken(TOKEN_ID, "tokenRudolf_1");

		assertEquals(gatekeeper.onlyForTestGetActiveTokens().size(), 2);
		printActiveUsers();
		assertEquals(gatekeeper.onlyForTestGetActiveUsers().size(), 1);

		gatekeeper.dataChanged(USER_RECORD_TYPE, userElly.id, "delete");
		assertEquals(gatekeeper.onlyForTestGetActiveTokens().size(), 0);
		assertEquals(gatekeeper.onlyForTestGetActiveUsers().size(), 0);
	}

	private void printActiveUsers() {
		for (ActiveUser activeUser : gatekeeper.onlyForTestGetActiveUsers().values()) {
			System.out.println(activeUser.user.loginId + ", " + activeUser.counter);
		}
	}

	private void assertActiveUserAndTokenHaveBeenRemovedFromGatekeeper(AuthenticationException e) {
		assertEquals(e.getMessage(), "Token not valid");
		assertTrue(gatekeeper.onlyForTestGetActiveTokens().isEmpty());
		assertTrue(gatekeeper.onlyForTestGetActiveUsers().isEmpty());
	}

	private void setupAnActiveUser(String token, User user) {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilAndRenewUntilInTheFuture(
				user.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(token, activeTokenForUser, user);
	}

	private ActiveTokenForUser createActiveTokenForUserValidUntilInThePast(String loginId) {
		long currentTimestamp = System.currentTimeMillis();
		long validUntil = currentTimestamp - THIRTY_MINUTES;
		long renewUntil = currentTimestamp + THIRTY_MINUTES;
		return new ActiveTokenForUser(TOKEN_ID, loginId, validUntil, renewUntil);
	}

	private ActiveTokenForUser createActiveTokenForUserValidUntilAndRenewUntilInTheFuture(
			String loginId) {
		long currentTimestamp = System.currentTimeMillis();
		long validUntil = currentTimestamp + THIRTY_MINUTES;
		long renewUntil = currentTimestamp + THIRTY_MINUTES;
		return new ActiveTokenForUser(TOKEN_ID, loginId, validUntil, renewUntil);
	}

	private ActiveTokenForUser createActiveTokenForUserRenewUntilInThePast(String loginId) {
		long currentTimestamp = System.currentTimeMillis();
		long validUntil = currentTimestamp + THIRTY_MINUTES;
		long renewUntil = currentTimestamp - THIRTY_MINUTES;
		return new ActiveTokenForUser(TOKEN_ID, loginId, validUntil, renewUntil);
	}

	@Test
	public void testOnlyForTestSetAuthToken() {
		setupAnActiveUser(TOKEN, userElly);

		assertEquals(gatekeeper.getUserForToken(TOKEN), userElly);
	}

	@Test
	public void testOnlyForTestGetActiveUsers() {
		ActiveTokenForUser activeTokenForUser = createActiveTokenForUserValidUntilAndRenewUntilInTheFuture(
				userElly.loginId);
		gatekeeper.onlyForTestSetActiveTokenAndActiveUsers(TOKEN, activeTokenForUser, userElly);

		Map<String, ActiveUser> activeUsers = gatekeeper.onlyForTestGetActiveUsers();

		assertEquals(activeUsers.size(), 1);
	}

}
