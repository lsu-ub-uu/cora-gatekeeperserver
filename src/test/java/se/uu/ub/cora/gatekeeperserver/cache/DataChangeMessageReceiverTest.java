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
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.logger.spies.LoggerSpy;
import se.uu.ub.cora.messaging.MessageReceiver;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.spies.RecordStorageInstanceProviderSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataChangeMessageReceiverTest {

	private static final String EMPTY_MESSAGE = "";
	private static final String SOME_OTHER_MESSAGING_ID = "someOtherMessagingId";
	private DataChangeMessageReceiver receiver;
	private GatekeeperLocatorSpy locator;
	private RecordStorageInstanceProviderSpy recordStorageProvider;
	private LoggerFactorySpy loggerFactory;

	@BeforeMethod
	private void beforeMethod() {
		setUpRecordStorageProvider();
		locator = new GatekeeperLocatorSpy();
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);

		receiver = new DataChangeMessageReceiver();
	}

	@AfterMethod
	private void afterMethod() {
		RecordStorageProvider.onlyForTestSetRecordStorageInstanceProvider(null);
	}

	private void setUpRecordStorageProvider() {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
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

	// @Test(enabled = false)
	// public void testTopicClosed() {
	// // can not be tested as security manager is removed from java
	// receiver.topicClosed();
	//
	// LoggerSpy logger = getLogger();
	// logger.MCR.assertParameters("logFatalUsingMessage", 0,
	// "Shuting down Gatekeeper due to lost connection with message broker,"
	// + "continued operation would lead to system inconsistencies.");
	// }

	// private LoggerSpy getLogger() {
	// return (LoggerSpy) loggerFactory.MCR.getReturnValue("factorForClass", 0);
	// }

	private Map<String, String> createHeadersForType(String type) {
		Map<String, String> headers = new HashMap<>();
		headers.put("type", type);
		headers.put("id", "someId");
		headers.put("action", "someAction");
		return headers;
	}

	@Test
	public void testTopicClosed() {
		DataChangeMessageRecieverForTest receiverForTest = new DataChangeMessageRecieverForTest();
		// can not be tested as security manager is removed from java
		receiverForTest.topicClosed();

		LoggerSpy logger = (LoggerSpy) loggerFactory.MCR.assertCalledParametersReturn(
				"factorForClass", DataChangeMessageRecieverForTest.class);
		logger.MCR.assertParameters("logFatalUsingMessage", 0,
				"Shuting down due to lost connection with message broker,"
						+ "continued operation would lead to system inconsistencies.");
		receiverForTest.MCR.assertMethodWasCalled("shutdownSystemToPreventDataInconsistency");
	}

	@Test
	public void testReceiveMessage_errorUpdateingCache() {
		DataChangeMessageRecieverForTest receiverForTest = new DataChangeMessageRecieverForTest();
		RuntimeException returnException = new RuntimeException("someException");
		recordStorageProvider.MRV.setAlwaysThrowException("dataChanged", returnException);

		Map<String, String> headers = createHeadersForType("user", "someAction",
				SOME_OTHER_MESSAGING_ID);

		receiverForTest.receiveMessage(headers, EMPTY_MESSAGE);

		LoggerSpy logger = (LoggerSpy) loggerFactory.MCR.assertCalledParametersReturn(
				"factorForClass", DataChangeMessageRecieverForTest.class);
		logger.MCR.assertParameters("logFatalUsingMessageAndException", 0,
				"Shuting down due to error keeping data in sync,"
						+ "continued operation would lead to system inconsistencies.",
				returnException);
		receiverForTest.MCR.assertMethodWasCalled("shutdownSystemToPreventDataInconsistency");
	}

	class DataChangeMessageRecieverForTest extends DataChangeMessageReceiver {
		public MethodCallRecorder MCR = new MethodCallRecorder();
		public MethodReturnValues MRV = new MethodReturnValues();

		public DataChangeMessageRecieverForTest() {
			log = LoggerProvider.getLoggerForClass(DataChangeMessageRecieverForTest.class);
			MCR.useMRV(MRV);
		}

		@Override
		void shutdownSystemToPreventDataInconsistency() {
			MCR.addCall();
		}
	}

	private LoggerSpy getLogger() {
		return (LoggerSpy) loggerFactory.MCR.getReturnValue("factorForClass", 0);
	}

	private Map<String, String> createHeadersForType(String type, String action,
			String messagingId) {
		Map<String, String> headers = new HashMap<>();
		headers.put("type", type);
		headers.put("id", "someId");
		headers.put("action", action);
		headers.put("messagingId", messagingId);
		return headers;
	}

}
