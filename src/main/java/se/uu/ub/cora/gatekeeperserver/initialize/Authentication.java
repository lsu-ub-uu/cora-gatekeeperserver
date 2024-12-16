package se.uu.ub.cora.gatekeeperserver.initialize;

import se.uu.ub.cora.gatekeeper.user.User;

public record Authentication(String tokenId, User user, long validUntil, long renewUntil) {

}
