/*
 * Copyright 2015, 2025 Uppsala University Library
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

package se.uu.ub.cora.gatekeeperserver.dependency;

import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.gatekeeperserver.authentication.GatekeeperSpy;

public class GatekeeperInstanceProviderTest {

	private GatekeeperLocatorSpy locator;

	@BeforeMethod
	private void beforeMethod() {
		locator = new GatekeeperLocatorSpy();
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);
	}

	@AfterMethod
	private void afterMethod() {
		GatekeeperInstanceProvider.setGatekeeperLocator(null);
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		Constructor<GatekeeperInstanceProvider> constructor = GatekeeperInstanceProvider.class
				.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
	}

	@Test(expectedExceptions = InvocationTargetException.class)
	public void testPrivateConstructorInvoke() throws Exception {
		Constructor<GatekeeperInstanceProvider> constructor = GatekeeperInstanceProvider.class
				.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void makeSureLocatorIsCalledForGatekeeper() {
		GatekeeperInstanceProvider.getGatekeeper();

		locator.MCR.assertMethodWasCalled("locateGatekeeper");
	}

	@Test
	public void testDataChange() {
		GatekeeperInstanceProvider.dataChanged("someType", "someId", "someAction");

		var gatekeeper = (GatekeeperSpy) locator.MCR.getReturnValue("locateGatekeeper", 0);
		gatekeeper.MCR.assertParameters("dataChanged", 0, "someType", "someId", "someAction");
	}
}
