module se.uu.ub.cora.gatekeeperserver {
	requires transitive java.ws.rs;
	requires transitive javax.servlet.api;
	requires transitive java.activation;
	requires transitive se.uu.ub.cora.json;
	requires transitive se.uu.ub.cora.bookkeeper;

	requires se.uu.ub.cora.gatekeeper;

	uses se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
	uses se.uu.ub.cora.gatekeeper.user.UserStorageProvider;
}