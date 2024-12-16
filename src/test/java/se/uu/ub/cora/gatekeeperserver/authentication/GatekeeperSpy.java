/*
 * Copyright 2016, 2024 Uppsala University Library
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

import java.util.Optional;

import se.uu.ub.cora.gatekeeper.picker.UserInfo;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.Gatekeeper;
import se.uu.ub.cora.gatekeeperserver.tokenprovider.AuthToken;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class GatekeeperSpy implements Gatekeeper {

	public boolean getUserForTokenWasCalled = false;
	public boolean getAuthTokenForUserInfoWasCalled = false;

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public GatekeeperSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> createUser());
		MRV.setDefaultReturnValuesSupplier("getAuthTokenForUserInfo", () -> createAuthToken());
	}

	@Override
	public User getUserForToken(String authToken) {
		return (User) MCR.addCallAndReturnFromMRV("authToken", authToken);
	}

	private User createUser() {
		User user = new User("someId");
		user.roles.add("someRole1");
		user.roles.add("someRole2");
		return user;
	}

	@Override
	public AuthToken getAuthTokenForUserInfo(UserInfo userInfo) {
		return (AuthToken) MCR.addCallAndReturnFromMRV("userInfo", userInfo);
	}

	private AuthToken createAuthToken() {
		return new AuthToken("someAuthToken", "someTokenId", 100L, 200L, "someIdFromStorage",
				"someloginId", Optional.empty(), Optional.empty());
	}

	@Override
	public void removeAuthToken(String tokenId, String authToken) {
		if ("someNonExistingAuthToken".equals(authToken)) {
			throw new AuthenticationException("authToken does not exist");
		}
	}

	@Override
	public AuthToken renewAuthToken(String tokenId, String token) {
		// TODO Auto-generated method stub
		return null;
	}
}
