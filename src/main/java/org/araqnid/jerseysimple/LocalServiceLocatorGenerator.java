package org.araqnid.jerseysimple;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

public class LocalServiceLocatorGenerator extends ServiceLocatorGeneratorImpl {

	@Override
	public ServiceLocator create(String name, ServiceLocator parent) {
		ServiceLocator serviceLocator = super.create(name, parent);
		GuiceIntoHK2Bridge bridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
		if (bridge != null) {
			GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
		}
		return serviceLocator;
	}
}
