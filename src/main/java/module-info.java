module se.uu.ub.cora.gatekeeperserver {
	requires transitive java.ws.rs;
	requires transitive json;
	requires transitive javax.servlet.api;
	requires transitive bookkeeper;
	requires transitive java.activation;

	requires se.uu.ub.cora.gatekeeper;

	uses se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
	uses se.uu.ub.cora.gatekeeper.user.UserStorage;
}