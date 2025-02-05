/*
 * Copyright 2016 Uppsala University Library
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

package se.uu.ub.cora.gatekeeperserver.authentication;

import se.uu.ub.cora.gatekeeperserver.Gatekeeper;
import se.uu.ub.cora.gatekeeperserver.dependency.GatekeeperLocator;

public class GateKeeperLocatorSpy implements GatekeeperLocator {

	public GatekeeperSpy gatekeeperSpy;
	public boolean gatekeeperLocated = false;
	private GatekeeperSpy spy;

	@Override
	public Gatekeeper locateGatekeeper() {
		gatekeeperLocated = true;
		// gatekeeperSpy = new GatekeeperSpy();
		return spy;
	}

	public void setGatekeepSpy(GatekeeperSpy spy) {
		this.spy = spy;

	}

}
