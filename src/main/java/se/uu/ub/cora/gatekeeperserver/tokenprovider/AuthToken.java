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

package se.uu.ub.cora.gatekeeperserver.tokenprovider;

public final class AuthToken {

	public final String token;
	public final int validForNoSeconds;
	public final String idInUserStorage;
	public final String loginId;
	public String firstName;
	public String lastName;

	private AuthToken(String token, int validForNoSeconds, String idInUserStorage, String loginId) {
		this.token = token;
		this.validForNoSeconds = validForNoSeconds;
		this.idInUserStorage = idInUserStorage;
		this.loginId = loginId;
	}

	public static AuthToken withTokenAndValidForNoSecondsAndIdInUserStorageAndLoginId(String token,
			int validForNoSeconds, String idInUserStorage, String loginId) {
		return new AuthToken(token, validForNoSeconds, idInUserStorage, loginId);
	}

}
