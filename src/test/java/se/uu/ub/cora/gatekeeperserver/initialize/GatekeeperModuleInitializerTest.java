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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import se.uu.ub.cora.gatekeeper.picker.UserPickerProvider;
import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.logger.spies.LoggerSpy;

public class GatekeeperModuleInitializerTest {
	private LoggerFactorySpy loggerFactorySpy;
	private ServletContext source;
	private ServletContextEvent context;
	private GatekeeperModuleInitializer gatekeeperInitializer;
	private UserPickerInstanceProviderSpy userPickerInstanceProviderSpy;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		userPickerInstanceProviderSpy = new UserPickerInstanceProviderSpy();
		UserPickerProvider.onlyForTestSetUserPickerInstanceProvider(userPickerInstanceProviderSpy);
		source = new ServletContextSpy();
		context = new ServletContextEvent(source);
		setNeededInitParameters();
		gatekeeperInitializer = new GatekeeperModuleInitializer();
	}

	private void setNeededInitParameters() {
		source.setInitParameter("initParam1", "initValue1");
		source.setInitParameter("initParam2", "initValue2");

	}

	@Test
	public void testLogMessagesOnStartup() throws Exception {
		gatekeeperInitializer.contextInitialized(context);

		loggerFactorySpy.MCR.assertParameters("factorForClass", 0,
				GatekeeperModuleInitializer.class);
		LoggerSpy loggerSpy = (LoggerSpy) loggerFactorySpy.MCR.getReturnValue("factorForClass", 0);
		loggerSpy.MCR.assertParameters("logInfoUsingMessage", 0,
				"GatekeeperModuleInitializer starting...");
		loggerSpy.MCR.assertParameters("logInfoUsingMessage", 1,
				"GatekeeperModuleInitializer started");
	}

	@Test
	public void testMakeCallToKnownNeededProvidersToMakeSureTheyStartCorrectlyAtSystemStartup()
			throws Exception {
		gatekeeperInitializer.contextInitialized(context);

		userPickerInstanceProviderSpy.MCR.assertMethodWasCalled("getUserPicker");
	}

	@Test
	public void testInitParametersArePassedOnToStarter() {
		gatekeeperInitializer.contextInitialized(context);

		assertEquals(SettingsProvider.getSetting("initParam1"), "initValue1");
		assertEquals(SettingsProvider.getSetting("initParam2"), "initValue2");
	}
}
