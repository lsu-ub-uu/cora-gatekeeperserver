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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Optional;

import org.testng.annotations.Test;

public class AuthTokenTest {
	@Test
	public void authTokenWithoutNames() {
		String token = "someToken";
		String tokenId = "someTokenId";
		int validForNoSeconds = 600;
		String idInUserStorage = "141414";
		String loginId = "loginId";

		AuthToken authToken = new AuthToken(token, tokenId, validForNoSeconds, idInUserStorage,
				loginId, Optional.empty(), Optional.empty());

		assertEquals(authToken.token(), token);
		assertEquals(authToken.tokenId(), tokenId);
		assertEquals(authToken.validForNoSeconds(), validForNoSeconds);
		assertEquals(authToken.idInUserStorage(), idInUserStorage);
		assertEquals(authToken.loginId(), loginId);
		assertTrue(authToken.firstName().isEmpty());
		assertTrue(authToken.lastName().isEmpty());
	}

	@Test
	public void authTokenWithFirstAndLastName() {
		String token = "someToken";
		String tokenId = "someTokenId";
		int validForNoSeconds = 600;
		String idInUserStorage = "141414";
		String loginId = "loginId";
		String firstname = "someFirstName";
		String lastname = "someLastName";

		AuthToken authToken = new AuthToken(token, tokenId, validForNoSeconds, idInUserStorage,
				loginId, Optional.of(firstname), Optional.of(lastname));

		assertEquals(authToken.token(), token);
		assertEquals(authToken.tokenId(), tokenId);
		assertEquals(authToken.validForNoSeconds(), validForNoSeconds);
		assertEquals(authToken.idInUserStorage(), idInUserStorage);
		assertEquals(authToken.loginId(), loginId);
		assertEquals(authToken.firstName().get(), firstname);
		assertEquals(authToken.lastName().get(), lastname);
	}
}