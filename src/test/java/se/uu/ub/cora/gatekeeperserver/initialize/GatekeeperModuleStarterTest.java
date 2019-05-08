/*
 * Copyright 2019 Olov McKie
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.UserStorage;
import se.uu.ub.cora.gatekeeper.user.UserStorageProvider;
import se.uu.ub.cora.gatekeeperserver.GatekeeperImp;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.gatekeeperserver.log.LoggerFactorySpy;
import se.uu.ub.cora.logger.LoggerProvider;

public class GatekeeperModuleStarterTest {

	private Map<String, String> initInfo;
	private List<UserPickerProvider> userPickerProviders;
	private List<UserStorageProvider> userStorageProviders;
	private LoggerFactorySpy loggerFactorySpy;
	private String testedClassName = "GatekeeperModuleStarterImp";

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		initInfo = new HashMap<>();
		initInfo.put("guestUserId", "someGuestUserId");
		userPickerProviders = new ArrayList<>();
		userPickerProviders.add(new UserPickerProviderSpy(null));
		userStorageProviders = new ArrayList<>();
		userStorageProviders.add(new UserStorageProviderSpy());

	}

	private void startGatekeeperModuleStarter() {
		GatekeeperModuleStarter starter = new GatekeeperModuleStarterImp();
		starter.startUsingInitInfoAndUserPickerProvidersAndUserStorageProviders(initInfo,
				userPickerProviders, userStorageProviders);
	}

	@Test(expectedExceptions = GatekeeperInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "No implementations found for UserStorageProvider")
	public void testStartModuleThrowsErrorIfNoUserStorageImplementations() throws Exception {
		userStorageProviders.clear();
		startGatekeeperModuleStarter();
	}

	@Test
	public void testStartModuleLogsErrorIfNoUserStorageProviderImplementations() throws Exception {
		userStorageProviders.clear();
		startGatekeeperMakeSureAnExceptionIsThrown();
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"No implementations found for UserStorageProvider");
	}

	@Test
	public void testStartModuleLogsErrorIfMoreThanOneUserStorageProviderImplementations()
			throws Exception {
		userStorageProviders.add(new UserStorageProviderSpy2());
		userStorageProviders.add(new UserStorageProviderSpy());
		startGatekeeperModuleStarter();
		String testedClassName = "GatekeeperModuleStarterImp";
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 2),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserStorageProviderSpy as "
						+ "UserStorageProvider implementation with select order 0.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 3),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserStorageProviderSpy2 as "
						+ "UserStorageProvider implementation with select order 2.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 4),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserStorageProviderSpy as "
						+ "UserStorageProvider implementation with select order 0.");

		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 5),
				"Using se.uu.ub.cora.gatekeeperserver.initialize.UserStorageProviderSpy2 as "
						+ "UserStorageProvider implementation.");
	}

	@Test
	public void testStartModuleInitInfoSentToUserStorageProviderImplementation() throws Exception {
		UserStorageProviderSpy userStorageProviderSpy = (UserStorageProviderSpy) userStorageProviders
				.get(0);
		startGatekeeperModuleStarter();
		assertSame(userStorageProviderSpy.initInfo, initInfo);
	}

	@Test
	public void testStartModuleLogsImplementationDetails() throws Exception {
		startGatekeeperModuleStarter();
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 0),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserPickerProviderSpy as "
						+ "UserPickerProvider implementation with select order 0.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 1),
				"Using se.uu.ub.cora.gatekeeperserver.initialize.UserPickerProviderSpy as "
						+ "UserPickerProvider implementation.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 2),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserStorageProviderSpy as "
						+ "UserStorageProvider implementation with select order 0.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 3),
				"Using se.uu.ub.cora.gatekeeperserver.initialize.UserStorageProviderSpy as "
						+ "UserStorageProvider implementation.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 4),
				"Found someGuestUserId as guestUserId");
	}

	@Test(expectedExceptions = GatekeeperInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "No implementations found for UserPickerProvider")
	public void testStartModuleThrowsErrorIfNoUserPickerProviderImplementations() throws Exception {
		userPickerProviders.clear();
		startGatekeeperModuleStarter();
	}

	@Test
	public void testStartModuleLogsErrorIfNoUserPickerProviderImplementations() throws Exception {
		userPickerProviders.clear();
		startGatekeeperMakeSureAnExceptionIsThrown();
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"No implementations found for UserPickerProvider");
	}

	private void startGatekeeperMakeSureAnExceptionIsThrown() {
		Exception caughtException = null;
		try {
			startGatekeeperModuleStarter();
		} catch (Exception e) {
			caughtException = e;
		}
		assertNotNull(caughtException);
	}

	@Test
	public void testStartModuleLogsErrorIfMoreThanOneUserPickerProviderImplementations()
			throws Exception {
		userPickerProviders.add(new UserPickerProviderSpy2(null));
		userPickerProviders.add(new UserPickerProviderSpy(null));
		startGatekeeperModuleStarter();
		String testedClassName = "GatekeeperModuleStarterImp";
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 0),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserPickerProviderSpy as "
						+ "UserPickerProvider implementation with select order 0.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 1),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserPickerProviderSpy2 as "
						+ "UserPickerProvider implementation with select order 2.");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 2),
				"Found se.uu.ub.cora.gatekeeperserver.initialize.UserPickerProviderSpy as "
						+ "UserPickerProvider implementation with select order 0.");

		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 3),
				"Using se.uu.ub.cora.gatekeeperserver.initialize.UserPickerProviderSpy2 as "
						+ "UserPickerProvider implementation.");

	}

	@Test(expectedExceptions = GatekeeperInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "InitInfo must contain guestUserId")
	public void testStartModuleThrowsErrorIfMissingGuestUserId() throws Exception {
		initInfo.clear();
		startGatekeeperModuleStarter();
	}

	@Test
	public void testStartModuleLogsErrorIfMissingGuestUserId() throws Exception {
		initInfo.clear();
		startGatekeeperMakeSureAnExceptionIsThrown();
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"InitInfo must contain guestUserId");
	}

	@Test()
	public void testStartModuleGuestUserIdSentToUserPickerImplementation() throws Exception {
		UserPickerProviderSpy userPickerProviderSpy = (UserPickerProviderSpy) userPickerProviders
				.get(0);
		startGatekeeperModuleStarter();
		assertEquals(userPickerProviderSpy.guestUserId(), "someGuestUserId");
	}

	@Test()
	public void testStartModuleUserStorageSentToUserPickerImplementation() throws Exception {
		UserPickerProviderSpy userPickerProviderSpy = (UserPickerProviderSpy) userPickerProviders
				.get(0);
		UserStorageProviderSpy userStorageProviderSpy = (UserStorageProviderSpy) userStorageProviders
				.get(0);
		UserStorage userStorage = userStorageProviderSpy.getUserStorage();
		startGatekeeperModuleStarter();
		assertEquals(userPickerProviderSpy.getUserStorage(), userStorage);
	}

	@Test
	public void testGatekeeperInstanceProviderSetUpWithLocator() throws Exception {
		UserPickerProviderSpy userPickerProviderSpy = (UserPickerProviderSpy) userPickerProviders
				.get(0);
		startGatekeeperModuleStarter();
		assertTrue(GatekeeperImp.INSTANCE.getUserPickerProvider() instanceof UserPickerProviderSpy);
		assertSame(GatekeeperImp.INSTANCE.getUserPickerProvider(), userPickerProviderSpy);
		assertNotNull(GatekeeperInstanceProvider.getGatekeeper());
	}
}
