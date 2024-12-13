/*
 * Copyright 2016, 2017, 2024 Uppsala University Library
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

import java.util.Optional;

import org.testng.annotations.Test;

public class AuthTokenToJsonConverterTest {
	@Test
	public void testAuthTokenToJsonConverter() {
		AuthToken authToken = new AuthToken("someToken", "someTokenId", 100L, 200L,
				"someIdFromStorage", "loginId", Optional.empty(), Optional.empty());
		AuthTokenToJsonConverter converter = new AuthTokenToJsonConverter(authToken);
		String json = converter.convertAuthTokenToJson();
		String expected = """
				{
				  "children": [
				    {
				      "name": "token",
				      "value": "someToken"
				    },
				    {
				      "name": "tokenId",
				      "value": "someTokenId"
				    },
				    {
				      "name": "validUntil",
				      "value": "100"
				    },
				    {
				      "name": "renewableUntil",
				      "value": "200"
				    },
				    {
				      "name": "idInUserStorage",
				      "value": "someIdFromStorage"
				    },
				    {
				      "name": "loginId",
				      "value": "loginId"
				    }
				  ],
				  "name": "authToken"
				}""";
		assertEquals(json, compactString(expected));
	}

	private String compactString(String string) {
		return string.trim().replace("\n", "").replace("\s", "");
	}

	@Test
	public void testAuthTokenToJsonConverterWithName() {
		AuthToken authToken = new AuthToken("someToken", "someTokenId", 100L, 200L,
				"someIdFromStorage", "loginId", Optional.of("someFirstName"),
				Optional.of("someLastName"));
		AuthTokenToJsonConverter converter = new AuthTokenToJsonConverter(authToken);
		String json = converter.convertAuthTokenToJson();
		String expected = """
				{
				  "children": [
				    {
				      "name": "token",
				      "value": "someToken"
				    },
				    {
				      "name": "tokenId",
				      "value": "someTokenId"
				    },
				    {
				      "name": "validUntil",
				      "value": "100"
				    },
				    {
				      "name": "renewableUntil",
				      "value": "200"
				    },
				    {
				      "name": "idInUserStorage",
				      "value": "someIdFromStorage"
				    },
				    {
				      "name": "loginId",
				      "value": "loginId"
				    },
				    {
				      "name": "firstName",
				      "value": "someFirstName"
				    },
				    {
				      "name": "lastName",
				      "value": "someLastName"
				    }
				  ],
				  "name": "authToken"
				}""";
		assertEquals(json, compactString(expected));
	}
}
