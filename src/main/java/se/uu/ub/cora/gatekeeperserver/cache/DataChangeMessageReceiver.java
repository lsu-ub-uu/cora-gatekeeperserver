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

import java.util.Map;

import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperInstanceProvider;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.messaging.MessageReceiver;
import se.uu.ub.cora.storage.RecordStorageProvider;

public class DataChangeMessageReceiver implements MessageReceiver {
	protected Logger log = LoggerProvider.getLoggerForClass(DataChangeMessageReceiver.class);

	@Override
	public void receiveMessage(Map<String, String> headers, String message) {
		try {
			handleUserChanges(headers);
		} catch (Exception e) {
			log.logFatalUsingMessageAndException("Shuting down due to error keeping data in sync,"
					+ "continued operation would lead to system inconsistencies.", e);
			shutdownSystemToPreventDataInconsistency();
		}
	}

	private void handleUserChanges(Map<String, String> headers) {
		String type = headers.get("type");
		if ("user".equals(type)) {
			handleDataChangedForGatekeeper(type, headers.get("id"), headers.get("action"));
		}
	}

	private void handleDataChangedForGatekeeper(String type, String id, String action) {
		RecordStorageProvider.dataChanged(type, id, action);
		GatekeeperInstanceProvider.dataChanged(type, id, action);
	}

	@Override
	public void topicClosed() {
		log.logFatalUsingMessage("Shuting down due to lost connection with message broker,"
				+ "continued operation would lead to system inconsistencies.");
		shutdownSystemToPreventDataInconsistency();
	}

	void shutdownSystemToPreventDataInconsistency() {
		System.exit(-1);
	}
}
