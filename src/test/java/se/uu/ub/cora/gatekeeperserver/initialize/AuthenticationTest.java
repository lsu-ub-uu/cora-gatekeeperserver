package se.uu.ub.cora.gatekeeperserver.initialize;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import se.uu.ub.cora.gatekeeper.user.User;

public class AuthenticationTest {

	@Test
	public void createAuthentication() throws Exception {
		User someUser = new User("someId");
		long validUntil = 100L;
		long renewUntil = 200L;
		Authentication authentication = new Authentication("someTokenId", someUser, validUntil,
				renewUntil);

		assertEquals(authentication.tokenId(), "someTokenId");
		assertEquals(authentication.user(), someUser);
		assertEquals(authentication.validUntil(), validUntil);
		assertEquals(authentication.renewUntil(), renewUntil);
	}
}
