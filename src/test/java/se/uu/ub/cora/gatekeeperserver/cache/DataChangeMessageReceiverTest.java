/*
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
package se.uu.ub.cora.gatekeeperserver.cache;

import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.gatekeeperserver.authentication.GatekeeperSpy;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocatorSpy;
import se.uu.ub.cora.logger.LoggerFactory;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.messaging.MessageReceiver;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.spies.RecordStorageInstanceProviderSpy;

public class DataChangeMessageReceiverTest {

	private static final String EMPTY_MESSAGE = "";
	private DataChangeMessageReceiver receiver;
	private GatekeeperLocatorSpy locator;
	private RecordStorageInstanceProviderSpy recordStorageProvider;

	@BeforeMethod
	private void beforeMethod() {
		setUpRecordStorageProvider();
		locator = new GatekeeperLocatorSpy();
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);

		receiver = new DataChangeMessageReceiver();
	}

	@AfterMethod
	private void afterMethod() {
		LoggerProvider.setLoggerFactory(null);
		RecordStorageProvider.onlyForTestSetRecordStorageInstanceProvider(null);
	}

	private void setUpRecordStorageProvider() {
		LoggerFactory logger = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(logger);
		recordStorageProvider = new RecordStorageInstanceProviderSpy();
		RecordStorageProvider.onlyForTestSetRecordStorageInstanceProvider(recordStorageProvider);
	}

	@Test
	public void testImplementsMessageReceiver() {
		assertTrue(receiver instanceof MessageReceiver);
	}

	@Test
	public void testReceiveMessage_UnrecognizedType_doNothing() {
		Map<String, String> headers = createHeadersForType("someType");

		receiver.receiveMessage(headers, EMPTY_MESSAGE);

		locator.MCR.assertMethodNotCalled("locateGatekeeper");
	}

	@Test
	public void testReceiveMessageAndCallDataChanged_forUserType() {
		Map<String, String> headers = createHeadersForType("user");

		receiver.receiveMessage(headers, EMPTY_MESSAGE);

		recordStorageProvider.MCR.assertParameters("dataChanged", 0, "user", "someId",
				"someAction");
		var gatekeeper = (GatekeeperSpy) locator.MCR
				.assertCalledParametersReturn("locateGatekeeper");
		gatekeeper.MCR.assertParameters("dataChanged", 0, "user", "someId", "someAction");

	}

	@Test
	public void testTopicClosed() {
		receiver.topicClosed();
	}

	private Map<String, String> createHeadersForType(String type) {
		Map<String, String> headers = new HashMap<>();
		headers.put("type", type);
		headers.put("id", "someId");
		headers.put("action", "someAction");
		return headers;
	}

}
