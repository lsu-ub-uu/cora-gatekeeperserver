module se.uu.ub.cora.gatekeeperserver {
	requires transitive java.ws.rs;
	requires transitive jakarta.activation;
	requires transitive se.uu.ub.cora.json;
	requires transitive javax.servlet.api;

	requires se.uu.ub.cora.gatekeeper;
	requires se.uu.ub.cora.logger;

	uses se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
	uses se.uu.ub.cora.gatekeeper.user.UserStorageProvider;

	exports se.uu.ub.cora.gatekeeperserver.initialize;
}