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
import se.uu.ub.cora.gatekeeperserver.UserPickerProviderSpy;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;

public class GatekeeperModuleStarterTest {

	private Map<String, String> initInfo;
	private List<UserPickerProvider> userPickerProviders;
	private List<UserStorageProvider> userStorageProviders;

	@BeforeMethod
	public void beforeMethod() {
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

	@Test(expectedExceptions = GatekeeperInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "More than one implementation found for UserStorageProvider")
	public void testStartModuleThrowsErrorIfMoreThanOneUserStorageImplementations()
			throws Exception {
		userStorageProviders.add(new UserStorageProviderSpy());
		startGatekeeperModuleStarter();
	}

	@Test()
	public void testStartModuleInitInfoSentToUserStorageProviderImplementation() throws Exception {
		UserStorageProviderSpy userStorageProviderSpy = (UserStorageProviderSpy) userStorageProviders
				.get(0);
		startGatekeeperModuleStarter();
		assertSame(userStorageProviderSpy.initInfo, initInfo);
	}

	@Test(expectedExceptions = GatekeeperInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "No implementations found for UserPickerProvider")
	public void testStartModuleThrowsErrorIfNoUserPickerProviderImplementations() throws Exception {
		userPickerProviders.clear();
		startGatekeeperModuleStarter();
	}

	@Test(expectedExceptions = GatekeeperInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "More than one implementation found for UserPickerProvider")
	public void testStartModuleThrowsErrorIfMoreThanOneUserPickerProviderImplementations()
			throws Exception {
		userPickerProviders.add(new UserPickerProviderSpy(null));
		startGatekeeperModuleStarter();
	}

	@Test(expectedExceptions = GatekeeperInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "InitInfo must contain guestUserId")
	public void testStartModuleThrowsErrorIfMissingGuestUserId() throws Exception {
		initInfo.clear();
		startGatekeeperModuleStarter();
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