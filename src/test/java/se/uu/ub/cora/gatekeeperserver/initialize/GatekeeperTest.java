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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.gatekeeper.picker.UserInfo;
import se.uu.ub.cora.gatekeeper.picker.UserPicker;
import se.uu.ub.cora.gatekeeper.picker.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.authentication.AuthenticationException;
import se.uu.ub.cora.gatekeeperserver.tokenprovider.AuthToken;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;

public class GatekeeperTest {
	private UserPickerInstanceProviderSpy userPickerInstanceProvider;
	private GatekeeperImp gatekeeper;
	private LoggerFactorySpy loggerFactory;
	private UserInfo userInfo;

	@BeforeMethod
	public void setUp() {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		userPickerInstanceProvider = new UserPickerInstanceProviderSpy();
		UserPickerProvider.onlyForTestSetUserPickerInstanceProvider(userPickerInstanceProvider);
		userInfo = UserInfo.withLoginIdAndLoginDomain("someLoginId", "someLoginDomain");
		gatekeeper = GatekeeperImp.INSTANCE;
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
		return (UserPickerSpy) userPickerInstanceProvider.MCR.getReturnValue("getUserPicker", 0);
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "token not valid")
	public void testNonAuthenticatedUser() {
		gatekeeper.getUserForToken("dummyNonAuthenticatedToken");
	}

	@Test
	public void testGetAuthTokenForUserInfo() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);

		User pickedUser = assertAndReturnPickUserWasUsed(userInfo);

		assertNotNull(authToken.token);
		assertEquals(authToken.validForNoSeconds, 600);
		assertSame(authToken.idInUserStorage, pickedUser.id);
		assertSame(authToken.loginId, pickedUser.loginId);
		assertSame(authToken.firstName, pickedUser.firstName);
		assertSame(authToken.lastName, pickedUser.lastName);
	}

	private User assertAndReturnPickUserWasUsed(UserInfo userInfo) {
		UserPickerSpy userPicker = assertAndReturnUserPickerProviderWasUsedToGetUserPicker();
		userPicker.MCR.assertMethodWasCalled("pickUser");
		userPicker.MCR.assertParameters("pickUser", 0, userInfo);
		User pickedUser = (User) userPicker.MCR.getReturnValue("pickUser", 0);
		return pickedUser;
	}

	@Test
	public void testGetAuthTokenForUserInfo_noNames() {
		User user = new User("user");
		user.loginId = "loginId";
		setUserToReturnFromUserProviderSpy(user);

		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		User pickedUser = assertAndReturnPickUserWasUsed(userInfo);
		assertNotNull(authToken.token);
		assertEquals(authToken.validForNoSeconds, 600);
		assertSame(authToken.idInUserStorage, pickedUser.id);
		assertSame(authToken.loginId, pickedUser.loginId);
		assertNull(authToken.firstName);
		assertNull(authToken.lastName);
	}

	private void setUserToReturnFromUserProviderSpy(User user) {
		UserPickerSpy userPicker = new UserPickerSpy();
		userPicker.MRV.setDefaultReturnValuesSupplier("pickUser", (Supplier<User>) () -> user);
		userPickerInstanceProvider.MRV.setDefaultReturnValuesSupplier("getUserPicker",
				(Supplier<UserPicker>) () -> userPicker);
	}

	@Test
	public void testGetAuthTokenWithProblem() {
		RuntimeException errorToThrow = new RuntimeException();
		userPickerInstanceProvider.MRV.setAlwaysThrowException("getUserPicker", errorToThrow);
		UserInfo userInfo = UserInfo.withLoginIdAndLoginDomain("someLoginIdWithProblem",
				"someLoginDomain");
		try {
			gatekeeper.getAuthTokenForUserInfo(userInfo);
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
				(Supplier<UserPicker>) () -> userPicker);
		UserInfo userInfo = UserInfo.withLoginIdAndLoginDomain("someLoginIdWithProblem",
				"someLoginDomain");
		try {
			gatekeeper.getAuthTokenForUserInfo(userInfo);
			assertTrue(false);
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
		User logedInUser = gatekeeper.getUserForToken(authToken.token);
		assertSame(logedInUser, pickedUser);
	}

	@Test
	public void testMultipleLoginsReturnsDiferentTokens() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		AuthToken authToken2 = gatekeeper.getAuthTokenForUserInfo(userInfo);

		assertNotEquals(authToken.token, authToken2.token);
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "AuthToken does not exist")
	public void testRemoveAuthTokenForUserTokenDoesNotExist() {
		gatekeeper.removeAuthTokenForUser("someNonExistingToken", "someLoginId");
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "idInUserStorage does not exist")
	public void testRemoveAuthTokenForUserFailsIfWrongUserId() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		gatekeeper.getUserForToken(authToken.token);
		gatekeeper.removeAuthTokenForUser(authToken.token, "notCorrectUserId");
	}

	@Test
	public void testRemoveAuthTokenForUser_removesAccess() {
		AuthToken authToken = gatekeeper.getAuthTokenForUserInfo(userInfo);
		User logedInUser = gatekeeper.getUserForToken(authToken.token);

		gatekeeper.removeAuthTokenForUser(authToken.token, logedInUser.loginId);

		try {
			gatekeeper.getUserForToken(authToken.token);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof AuthenticationException);
			assertEquals(e.getMessage(), "token not valid");
		}
	}
}
