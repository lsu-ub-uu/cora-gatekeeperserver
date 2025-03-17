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

import java.util.Enumeration;
import java.util.HashMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import se.uu.ub.cora.gatekeeper.picker.UserPickerProvider;
import se.uu.ub.cora.gatekeeperserver.cache.DataChangeMessageReceiver;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocator;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocatorImp;
import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.messaging.AmqpMessageListenerRoutingInfo;
import se.uu.ub.cora.messaging.MessageRoutingInfo;
import se.uu.ub.cora.messaging.MessagingProvider;

@WebListener
public class GatekeeperModuleInitializer implements ServletContextListener {
	private ServletContext servletContext;
	private Logger log = LoggerProvider.getLoggerForClass(GatekeeperModuleInitializer.class);

	@Override
	public void contextInitialized(ServletContextEvent contextEvent) {
		servletContext = contextEvent.getServletContext();
		initializeGatekeeper();
	}

	private void initializeGatekeeper() {
		String simpleName = GatekeeperModuleInitializer.class.getSimpleName();
		log.logInfoUsingMessage(simpleName + " starting...");
		collectInitInformation();
		startListenForDataChangesForUser();
		startLocator();
		makeCallToKnownNeededProvidersToMakeSureTheyStartCorrectlyAtSystemStartup();
		log.logInfoUsingMessage(simpleName + " started");
	}

	private void makeCallToKnownNeededProvidersToMakeSureTheyStartCorrectlyAtSystemStartup() {
		UserPickerProvider.getUserPicker();
	}

	private void collectInitInformation() {
		HashMap<String, String> initInfo = new HashMap<>();
		Enumeration<String> initParameterNames = servletContext.getInitParameterNames();
		while (initParameterNames.hasMoreElements()) {
			String key = initParameterNames.nextElement();
			initInfo.put(key, servletContext.getInitParameter(key));
		}
		SettingsProvider.setSettings(initInfo);
	}

	private void startLocator() {
		GatekeeperLocator locator = new GatekeeperLocatorImp();
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);
	}

	private void startListenForDataChangesForUser() {
		MessageRoutingInfo routingInfo = createRoutingInfo();
		var listener = MessagingProvider.getTopicMessageListener(routingInfo);

		listener.listen(new DataChangeMessageReceiver());
	}

	private MessageRoutingInfo createRoutingInfo() {
		String hostname = SettingsProvider.getSetting("hostname");
		int port = Integer.parseInt(SettingsProvider.getSetting("port"));
		String virtualHost = SettingsProvider.getSetting("virtualHost");
		String exchange = SettingsProvider.getSetting("exchange");
		String routingKey = "user";

		return new AmqpMessageListenerRoutingInfo(hostname, port, virtualHost, exchange,
				routingKey);
	}
}
