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

package se.uu.ub.cora.gatekeeperserver;

import se.uu.ub.cora.gatekeeper.picker.UserInfo;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.gatekeeperserver.authentication.AuthenticationException;
import se.uu.ub.cora.gatekeeperserver.tokenprovider.AuthToken;

public interface Gatekeeper {

	/**
	 * getUserForToken returns the User related to the given authToken. If the authToken is not
	 * valid an {@link AuthenticationException} is thrown.
	 * 
	 * @param authToken
	 * @return If the auhToken is valid then it returns the {@link User} related to the authToken
	 */
	User getUserForToken(String authToken);

	/**
	 * 
	 * @param userInfo
	 * @return
	 */
	AuthToken getAuthTokenForUserInfo(UserInfo userInfo);

	/**
	 * removeAuthToken removes authtoken from gatekeeper
	 * 
	 * @param tokenId
	 * @param authToken
	 */
	void removeAuthToken(String tokenId, String authToken);

	AuthToken renewAuthToken(String tokenId, String token);
}
