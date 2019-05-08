package se.uu.ub.cora.gatekeeperserver.initialize;

import java.util.Map;

import se.uu.ub.cora.gatekeeper.user.UserStorage;
import se.uu.ub.cora.gatekeeper.user.UserStorageProvider;

public class UserStorageProviderSpy2 implements UserStorageProvider {

	Map<String, String> initInfo;
	private UserStorageSpy userStorageSpy = new UserStorageSpy();

	@Override
	public UserStorage getUserStorage() {
		return userStorageSpy;
	}

	@Override
	public void startUsingInitInfo(Map<String, String> initInfo) {
		this.initInfo = initInfo;
	}

	@Override
	public int getOrderToSelectImplementionsBy() {
		return 2;
	}

}
