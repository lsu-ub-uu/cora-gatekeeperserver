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
	 * Retrieves an authentication token for the given user information.
	 *
	 * @param userInfo
	 *            the user information used to generate the authentication token
	 * @return an {@link AuthToken} associated with the provided {@link UserInfo}
	 */
	AuthToken getAuthTokenForUserInfo(UserInfo userInfo);

	/**
	 * Returns the user associated with the given authentication token.
	 * 
	 * This method checks the validity of the authentication token, ensuring that it exists and its
	 * `validUntil` time has not passed, otherwise an {@link AuthenticationException} is thrown.
	 *
	 * @param token
	 *            the authentication token to look up
	 * @return the {@link User} associated with the provided authentication token
	 * @throws AuthenticationException
	 *             if the authentication token is not valid
	 */
	User getUserForToken(String token);

	/**
	 * Renews the authentication token if it is valid and eligible for renewal.
	 *
	 * This method checks the validity of the authentication token, ensuring that its `validUntil`
	 * time has not passed, and that the `renewUntil` timestamp allows renewal. If the token is
	 * non-existent, invalid, or past the renewal period, an {@link AuthenticationException} is
	 * thrown.
	 *
	 * @param tokenId
	 *            the ID of the token to renew
	 * @param token
	 *            the authentication token to renew
	 * @return a new {@link AuthToken} if renewal is successful
	 * @throws AuthenticationException
	 *             if the authentication token is invalid or cannot be renewed
	 */
	AuthToken renewAuthToken(String tokenId, String token);

	/**
	 * Removes the authentication token from the gatekeeper system.
	 * 
	 * If trying to remove a token that does not exist or do not belong to tokenId then an
	 * {@link AuthenticationException} is thrown.
	 *
	 * @param tokenId
	 *            the ID of the token to remove
	 * @param token
	 *            the authentication token to remove
	 */
	void removeAuthToken(String tokenId, String token);

	/**
	 * dataChanged method is intended to inform gatekeeper about data that is changed in storage.
	 * This is to make it possible to implement a cached data from storage and update relevant
	 * records when data is changed. This change can be done by processes running in the same system
	 * or by processes running on other servers.
	 * 
	 * @param type
	 *            A String with the records type
	 * @param id
	 *            A String with the records id
	 * @param action
	 *            A String with the action of how the data was changed ("create", "update" or
	 *            "delete").
	 */
	void dataChanged(String type, String id, String action);
}
