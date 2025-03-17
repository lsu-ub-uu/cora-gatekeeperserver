/*
 * Copyright 2019 Olov McKie
 * Copyright 2025 Uppsala University Library
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

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import se.uu.ub.cora.gatekeeper.picker.UserPickerProvider;
import se.uu.ub.cora.gatekeeperserver.cache.DataChangeMessageReceiver;
import se.uu.ub.cora.gatekeeperserver.cache.spies.MessageListenerSpy;
import se.uu.ub.cora.gatekeeperserver.cache.spies.MessagingFactorySpy;
import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.logger.spies.LoggerSpy;
import se.uu.ub.cora.messaging.AmqpMessageListenerRoutingInfo;
import se.uu.ub.cora.messaging.MessagingProvider;

public class GatekeeperModuleInitializerTest {
	private LoggerFactorySpy loggerFactorySpy;
	private ServletContext source;
	private ServletContextEvent context;
	private GatekeeperModuleInitializer gatekeeperInitializer;
	private UserPickerInstanceProviderSpy userPickerInstanceProviderSpy;

	private Map<String, String> settings;
	private MessagingFactorySpy messagingFactory;

	@BeforeTest
	private void beforeTest() {
		settings = new HashMap<>();
		settings.put("hostname", "someHostname");
		settings.put("port", "6666");
		settings.put("virtualHost", "someVirtualHost");
		settings.put("exchange", "someExchange");
	}

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		messagingFactory = new MessagingFactorySpy();
		MessagingProvider.setMessagingFactory(messagingFactory);

		userPickerInstanceProviderSpy = new UserPickerInstanceProviderSpy();
		UserPickerProvider.onlyForTestSetUserPickerInstanceProvider(userPickerInstanceProviderSpy);
		source = new ServletContextSpy();
		context = new ServletContextEvent(source);
		setNeededInitParameters();
		// SettingsProvider.setSettings(settings);
		gatekeeperInitializer = new GatekeeperModuleInitializer();
	}

	@AfterMethod
	private void afterMethod() {
		SettingsProvider.setSettings(null);
		LoggerProvider.setLoggerFactory(null);
		MessagingProvider.setMessagingFactory(null);
	}

	private void setNeededInitParameters() {
		source.setInitParameter("initParam1", "initValue1");
		source.setInitParameter("initParam2", "initValue2");
		source.setInitParameter("hostname", "someHostname");
		source.setInitParameter("port", "6666");
		source.setInitParameter("virtualHost", "someVirtualHost");
		source.setInitParameter("exchange", "someExchange");
	}

	@Test
	public void testLogMessagesOnStartup() {
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
	public void testMakeCallToKnownNeededProvidersToMakeSureTheyStartCorrectlyAtSystemStartup() {
		gatekeeperInitializer.contextInitialized(context);

		userPickerInstanceProviderSpy.MCR.assertMethodWasCalled("getUserPicker");
	}

	@Test
	public void testInitParametersArePassedOnToStarter() {
		gatekeeperInitializer.contextInitialized(context);

		assertEquals(SettingsProvider.getSetting("initParam1"), "initValue1");
		assertEquals(SettingsProvider.getSetting("initParam2"), "initValue2");
	}

	@Test
	public void testStartListening() {
		gatekeeperInitializer.contextInitialized(context);

		messagingFactory.MCR.assertMethodWasCalled("factorTopicMessageListener");
		assertRoutingInfo();
		var listener = assertListenerAndReturn();
		assertMessageReceiver(listener);
	}

	private void assertRoutingInfo() {
		var routingInfo = (AmqpMessageListenerRoutingInfo) messagingFactory.MCR
				.getParameterForMethodAndCallNumberAndParameter("factorTopicMessageListener", 0,
						"messagingRoutingInfo");
		assertEquals(routingInfo.hostname, settings.get("hostname"));
		assertEquals(routingInfo.port, Integer.parseInt(settings.get("port")));
		assertEquals(routingInfo.virtualHost, settings.get("virtualHost"));
		assertEquals(routingInfo.exchange, settings.get("exchange"));
		assertEquals(routingInfo.routingKey, "user");
	}

	private MessageListenerSpy assertListenerAndReturn() {
		var listener = (MessageListenerSpy) messagingFactory.MCR
				.getReturnValue("factorTopicMessageListener", 0);
		listener.MCR.assertMethodWasCalled("listen");
		return listener;
	}

	private void assertMessageReceiver(MessageListenerSpy listener) {
		var messageReceiver = listener.MCR.getParameterForMethodAndCallNumberAndParameter("listen",
				0, "messageReceiver");
		assertTrue(messageReceiver instanceof DataChangeMessageReceiver);
	}
}
