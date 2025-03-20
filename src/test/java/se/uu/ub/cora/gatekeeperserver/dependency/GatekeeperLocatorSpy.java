/*
 * Copyright 2016 Olov McKie
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

package se.uu.ub.cora.gatekeeperserver.dependency;

import se.uu.ub.cora.gatekeeperserver.Gatekeeper;
import se.uu.ub.cora.gatekeeperserver.authentication.GatekeeperSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class GatekeeperLocatorSpy implements GatekeeperLocator {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public GatekeeperLocatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("locateGatekeeper", GatekeeperSpy::new);
	}

	@Override
	public Gatekeeper locateGatekeeper() {
		return (Gatekeeper) MCR.addCallAndReturnFromMRV();
	}
}
