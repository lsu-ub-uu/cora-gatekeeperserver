module se.uu.ub.cora.gatekeeperserver {
	requires transitive jakarta.ws.rs;
	requires transitive se.uu.ub.cora.json;
	requires transitive jakarta.servlet;

	requires se.uu.ub.cora.gatekeeper;
	requires se.uu.ub.cora.logger;

	uses se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
	uses se.uu.ub.cora.gatekeeper.user.UserStorageProvider;

	exports se.uu.ub.cora.gatekeeperserver.initialize;
}