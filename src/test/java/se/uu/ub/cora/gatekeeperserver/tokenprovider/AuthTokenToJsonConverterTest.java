/*
 * Copyright 2016, 2017, 2024, 2025 Uppsala University Library
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

public class AuthTokenToJsonConverterTest {
	@Test
	public void testAuthTokenToJsonConverterWithoutNamesNoPermissionUnits() {
		AuthToken authToken = new AuthToken("someToken", "someTokenId", 100L, 200L,
				"someIdFromStorage", "loginId", Optional.empty(), Optional.empty(),
				Collections.emptySet());
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
				      "name": "renewUntil",
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
	public void testAuthTokenToJsonConverterWithNameAndOnePermissionUnits() {
		AuthToken authToken = new AuthToken("someToken", "someTokenId", 100L, 200L,
				"someIdFromStorage", "loginId", Optional.of("someFirstName"),
				Optional.of("someLastName"), Set.of("001"));
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
				      "name": "renewUntil",
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
				    },
				    {
				      "repeatId": "1",
				      "children": [
				        {
				          "name": "linkedRecordType",
				          "value": "permissionUnit"
				        },
				        {
				          "name": "linkedRecordId",
				          "value": "001"
				        }
				      ],
				      "name": "permissionUnit"
				    }
				  ],
				  "name": "authToken"
				}""";
		assertEquals(json, compactString(expected));
	}

	@Test
	public void testAuthTokenToJsonConverterWithNameAndTwoPermissionUnits() {
		Set<String> permissionUnits = new LinkedHashSet<>();
		permissionUnits.add("001");
		permissionUnits.add("002");

		AuthToken authToken = new AuthToken("someToken", "someTokenId", 100L, 200L,
				"someIdFromStorage", "loginId", Optional.of("someFirstName"),
				Optional.of("someLastName"), permissionUnits);
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
				      "name": "renewUntil",
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
				    },
				    {
				      "repeatId": "1",
				      "children": [
				        {
				          "name": "linkedRecordType",
				          "value": "permissionUnit"
				        },
				        {
				          "name": "linkedRecordId",
				          "value": "001"
				        }
				      ],
				      "name": "permissionUnit"
				    },
				    {
				      "repeatId": "2",
				      "children": [
				        {
				          "name": "linkedRecordType",
				          "value": "permissionUnit"
				        },
				        {
				          "name": "linkedRecordId",
				          "value": "002"
				        }
				      ],
				      "name": "permissionUnit"
				    }
				  ],
				  "name": "authToken"
				}""";
		assertEquals(json, compactString(expected));
	}
}
