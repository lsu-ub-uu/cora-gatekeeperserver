package se.uu.ub.cora.gatekeeperserver.initialize;

import java.util.Map;

import se.uu.ub.cora.gatekeeper.user.UserPickerProvider;
import se.uu.ub.cora.gatekeeper.user.UserStorageProvider;

public interface GatekeeperModuleStarter {

	void startUsingInitInfoAndUserPickerProvidersAndUserStorageProviders(
			Map<String, String> initInfo,
			Iterable<UserPickerProvider> userPickerProviderImplementations,
			Iterable<UserStorageProvider> userStorageProviderImplementations);

}