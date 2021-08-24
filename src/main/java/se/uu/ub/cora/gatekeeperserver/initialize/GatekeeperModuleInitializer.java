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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ServiceLoader;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.UserStorageProvider;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;

@jakarta.servlet.annotation.WebListener
public class GatekeeperModuleInitializer implements ServletContextListener {
	private ServletContext servletContext;
	private HashMap<String, String> initInfo = new HashMap<>();
	private GatekeeperModuleStarter starter = new GatekeeperModuleStarterImp();
	private Iterable<UserPickerProvider> userPickerProviderImplementations;
	private ServiceLoader<UserStorageProvider> userStorageProviderImplementations;
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
		collectUserPickerProviderImplementations();
		collectUserStorageImplementations();
		startGatekeeperStarter();
		log.logInfoUsingMessage(simpleName + " started");
	}

	private void collectInitInformation() {
		Enumeration<String> initParameterNames = servletContext.getInitParameterNames();
		while (initParameterNames.hasMoreElements()) {
			String key = initParameterNames.nextElement();
			initInfo.put(key, servletContext.getInitParameter(key));
		}
	}

	private void collectUserPickerProviderImplementations() {
		userPickerProviderImplementations = ServiceLoader.load(UserPickerProvider.class);
	}

	private void collectUserStorageImplementations() {
		userStorageProviderImplementations = ServiceLoader.load(UserStorageProvider.class);
	}

	private void startGatekeeperStarter() {
		starter.startUsingInitInfoAndUserPickerProvidersAndUserStorageProviders(initInfo,
				userPickerProviderImplementations, userStorageProviderImplementations);
	}

	GatekeeperModuleStarter getStarter() {
		// needed for test
		return starter;
	}

	void setStarter(GatekeeperModuleStarter starter) {
		this.starter = starter;

	}

}
