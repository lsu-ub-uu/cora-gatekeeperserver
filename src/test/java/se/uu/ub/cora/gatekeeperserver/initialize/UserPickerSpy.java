/*
 * Copyright 2022 Uppsala University Library
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

import java.util.function.Supplier;

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
		User guest = new User("guest");
		MRV.setDefaultReturnValuesSupplier("pickGuest", (Supplier<User>) () -> guest);
		User user = new User("user");
		user.loginId = "loginId";
		user.firstName = "firstName";
		user.lastName = "lastName";
		MRV.setDefaultReturnValuesSupplier("pickUser", (Supplier<User>) () -> user);
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
