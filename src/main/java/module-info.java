module se.uu.ub.cora.gatekeeperserver {
	requires transitive java.ws.rs;
	requires transitive java.activation;
	requires transitive se.uu.ub.cora.json;
	// requires transitive se.uu.ub.cora.bookkeeper;
	requires transitive javax.servlet.api;

	requires se.uu.ub.cora.gatekeeper;
	requires se.uu.ub.cora.logger;
	requires se.uu.ub.cora.data;

	uses se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
	uses se.uu.ub.cora.gatekeeper.user.UserStorageProvider;
}