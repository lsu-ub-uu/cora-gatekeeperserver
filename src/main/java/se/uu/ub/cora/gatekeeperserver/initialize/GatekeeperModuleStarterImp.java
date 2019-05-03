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

import java.util.Map;

import se.uu.ub.cora.gatekeeper.user.PreferenceLevel;
import se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.UserStorage;
import se.uu.ub.cora.gatekeeper.user.UserStorageProvider;
import se.uu.ub.cora.gatekeeperserver.GatekeeperImp;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocator;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocatorImp;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;

public class GatekeeperModuleStarterImp implements GatekeeperModuleStarter {
	private static final String IMPLEMENTATION = " implementation";
	private static final String FOUND = "Found ";
	private Map<String, String> initInfo;
	private Iterable<UserPickerProvider> userPickerProviders;
	private Iterable<UserStorageProvider> userStorageProviders;
	private Logger log = LoggerProvider.getLoggerForClass(GatekeeperModuleStarterImp.class);

	@Override
	public void startUsingInitInfoAndUserPickerProvidersAndUserStorageProviders(
			Map<String, String> initInfo,
			Iterable<UserPickerProvider> userPickerProviderImplementations,
			Iterable<UserStorageProvider> userStorageProviderImplementations) {
		this.initInfo = initInfo;
		this.userPickerProviders = userPickerProviderImplementations;
		this.userStorageProviders = userStorageProviderImplementations;
		start();
	}

	public void start() {
		UserPickerProvider userPickerProvider = getImplementationBasedOnPreferenceLevelThrowErrorIfNone(
				userPickerProviders, "UserPickerProvider");

		UserStorageProvider userStorageProvider = getImplementationBasedOnPreferenceLevelThrowErrorIfNone(
				userStorageProviders, "UserStorageProvider");

		userStorageProvider.startUsingInitInfo(initInfo);
		UserStorage userStorage = userStorageProvider.getUserStorage();

		String guestUserId = tryToGetInitParameter("guestUserId");
		userPickerProvider.startUsingUserStorageAndGuestUserId(userStorage, guestUserId);

		GatekeeperLocator locator = new GatekeeperLocatorImp();
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);
		GatekeeperImp.INSTANCE.setUserPickerProvider(userPickerProvider);
	}

	private String tryToGetInitParameter(String parameterName) {
		throwErrorIfKeyIsMissingFromInitInfo(parameterName);
		String parameter = initInfo.get(parameterName);
		log.logInfoUsingMessage(FOUND + parameter + " as " + parameterName);
		return parameter;
	}

	private void throwErrorIfKeyIsMissingFromInitInfo(String key) {
		if (!initInfo.containsKey(key)) {
			String errorMessage = "InitInfo must contain " + key;
			log.logFatalUsingMessage(errorMessage);
			throw new GatekeeperInitializationException(errorMessage);
		}
	}

	private <T extends PreferenceLevel> T getImplementationBasedOnPreferenceLevelThrowErrorIfNone(
			Iterable<T> implementations, String interfaceClassName) {
		T implementation = findAndLogPreferedImplementation(implementations, interfaceClassName);
		throwErrorIfNoImplementationFound(interfaceClassName, implementation);
		log.logInfoUsingMessage("Using " + implementation.getClass().getName() + " as "
				+ interfaceClassName + " implementation.");
		return implementation;
	}

	private <T extends PreferenceLevel> void throwErrorIfNoImplementationFound(
			String interfaceClassName, T implementation) {
		if (null == implementation) {
			throwAndLogError(interfaceClassName);
		}
	}

	private <T extends PreferenceLevel> T findAndLogPreferedImplementation(
			Iterable<T> implementations, String interfaceClassName) {
		T implementation = null;
		int preferenceLevel = -99999;
		for (T currentImplementation : implementations) {
			if (preferenceLevel < currentImplementation.getPreferenceLevel()) {
				preferenceLevel = currentImplementation.getPreferenceLevel();
				implementation = currentImplementation;
			}
			log.logInfoUsingMessage(FOUND + currentImplementation.getClass().getName() + " as "
					+ interfaceClassName + " implementation with preference level "
					+ currentImplementation.getPreferenceLevel() + ".");
		}
		return implementation;
	}

	private void throwAndLogError(String interfaceClassName) {
		String errorMessage = "No implementations found for " + interfaceClassName;
		log.logFatalUsingMessage(errorMessage);
		throw new GatekeeperInitializationException(errorMessage);
	}
}
