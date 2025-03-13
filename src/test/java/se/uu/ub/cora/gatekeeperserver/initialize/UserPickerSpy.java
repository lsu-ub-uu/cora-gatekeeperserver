/*
 * Copyright 2022, 2025 Uppsala University Library
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

import se.uu.ub.cora.gatekeeper.picker.UserInfo;
import se.uu.ub.cora.gatekeeper.picker.UserPicker;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class UserPickerSpy implements UserPicker {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public UserPickerSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("pickGuest", () -> createGenericUser("Guest"));
		MRV.setDefaultReturnValuesSupplier("pickUser", () -> createGenericUser("SomeUser"));
	}

	private User createGenericUser(String name) {
		User aUser = new User("some" + name + "UserId");
		aUser.firstName = "some" + name + "FirstName";
		aUser.lastName = "some" + name + "LastName";
		aUser.loginId = "some" + name + "LoginId";
		return aUser;
	}

	@Override
	public User pickGuest() {
		return (User) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public User pickUser(UserInfo userInfo) {
		return (User) MCR.addCallAndReturnFromMRV("userInfo", userInfo);
	}

}
