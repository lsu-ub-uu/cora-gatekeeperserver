module se.uu.ub.cora.gatekeeperserver {
	requires transitive jakarta.ws.rs;
	requires transitive se.uu.ub.cora.json;
	requires transitive jakarta.servlet;

	requires transitive se.uu.ub.cora.gatekeeper;
	requires se.uu.ub.cora.logger;
	requires se.uu.ub.cora.initialize;
	requires se.uu.ub.cora.messaging;
	requires se.uu.ub.cora.storage;

	uses se.uu.ub.cora.gatekeeper.picker.UserPickerInstanceProvider;
	uses se.uu.ub.cora.gatekeeper.storage.UserStorageProvider;

	exports se.uu.ub.cora.gatekeeperserver.initialize;
	exports se.uu.ub.cora.gatekeeperserver.tokenprovider;
}