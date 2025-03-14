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
import static org.testng.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.gatekeeperserver.authentication.GatekeeperSpy;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocatorSpy;
import se.uu.ub.cora.messaging.MessageReceiver;

public class DataChangeMessageReceiverTest {

	private static final String EMPTY_MESSAGE = "";
	private DataChangeMessageReceiver receiver;
	private GatekeeperLocatorSpy locator;

	@BeforeMethod
	private void beforeMethod() {
		locator = new GatekeeperLocatorSpy();
		GatekeeperInstanceProvider.setGatekeeperLocator(locator);

		receiver = new DataChangeMessageReceiver();
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

		fail();
		// TODO: call RecordStorage.dataChanged(type, id, action);)
		var gatekeeper = (GatekeeperSpy) locator.MCR
				.assertCalledParametersReturn("locateGatekeeper");
		gatekeeper.MCR.assertParameters("dataChanged", 0, "user", "someId", "someAction");

	}

	private Map<String, String> createHeadersForType(String type) {
		Map<String, String> headers = new HashMap<>();
		headers.put("type", type);
		headers.put("id", "someId");
		headers.put("action", "someAction");
		return headers;
	}
}
