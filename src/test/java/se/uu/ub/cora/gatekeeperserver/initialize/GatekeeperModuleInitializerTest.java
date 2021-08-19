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
import static org.testng.Assert.assertTrue;

import java.util.Map;
import java.util.ServiceLoader;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.UserStorageProvider;
import se.uu.ub.cora.gatekeeperserver.log.LoggerFactorySpy;
import se.uu.ub.cora.logger.LoggerProvider;

public class GatekeeperModuleInitializerTest {
	private ServletContext source;
	private ServletContextEvent context;
	private GatekeeperModuleInitializer initializer;
	private LoggerFactorySpy loggerFactorySpy;
	private String testedClassName = "GatekeeperModuleInitializer";

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		source = new ServletContextSpy();
		source.setInitParameter("initParam1", "initValue1");
		source.setInitParameter("initParam2", "initValue2");
		context = new ServletContextEvent(source);
		initializer = new GatekeeperModuleInitializer();
	}

	@Test
	public void testNonExceptionThrowingStartup() throws Exception {
		GatekeeperModuleStarterSpy starter = startGatekeeperModuleInitializerWithStarterSpy();
		assertTrue(starter.startWasCalled);
	}

	@Test
	public void testLogMessagesOnStartup() throws Exception {
		startGatekeeperModuleInitializerWithStarterSpy();
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 0),
				"GatekeeperModuleInitializer starting...");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 1),
				"GatekeeperModuleInitializer started");
	}

	private GatekeeperModuleStarterSpy startGatekeeperModuleInitializerWithStarterSpy() {
		GatekeeperModuleStarterSpy starter = new GatekeeperModuleStarterSpy();
		initializer.setStarter(starter);
		initializer.contextInitialized(context);
		return starter;
	}

	@Test
	public void testInitParametersArePassedOnToStarter() {
		GatekeeperModuleStarterSpy starter = startGatekeeperModuleInitializerWithStarterSpy();
		Map<String, String> initInfo = starter.initInfo;
		assertEquals(initInfo.size(), 2);
		assertEquals(initInfo.get("initParam1"), "initValue1");
		assertEquals(initInfo.get("initParam2"), "initValue2");
	}

	@Test
	public void testUserPickerProviderImplementationsArePassedOnToStarter() {
		GatekeeperModuleStarterSpy starter = startGatekeeperModuleInitializerWithStarterSpy();

		Iterable<UserPickerProvider> iterable = starter.userPickerProviderImplementations;
		assertTrue(iterable instanceof ServiceLoader);
	}

	@Test
	public void testUserStorageProviderImplementationsArePassedOnToStarter() {
		GatekeeperModuleStarterSpy starter = startGatekeeperModuleInitializerWithStarterSpy();

		Iterable<UserStorageProvider> iterable = starter.userStorageProviderImplementations;
		assertTrue(iterable instanceof ServiceLoader);
	}

	@Test
	public void testInitUsesDefaultGatekeeperModuleStarter() throws Exception {
		makeSureErrorIsThrownAsNoImplementationsExistInThisModule();
		GatekeeperModuleStarterImp starter = (GatekeeperModuleStarterImp) initializer.getStarter();
		assertStarterIsGatekeeperModuleStarter(starter);
	}

	private void makeSureErrorIsThrownAsNoImplementationsExistInThisModule() {
		Exception caughtException = null;
		try {
			initializer.contextInitialized(context);
		} catch (Exception e) {
			caughtException = e;
		}
		assertTrue(caughtException instanceof GatekeeperInitializationException);
		assertEquals(caughtException.getMessage(),
				"No implementations found for UserPickerProvider");
	}

	private void assertStarterIsGatekeeperModuleStarter(GatekeeperModuleStarter starter) {
		assertTrue(starter instanceof GatekeeperModuleStarterImp);
	}

}
