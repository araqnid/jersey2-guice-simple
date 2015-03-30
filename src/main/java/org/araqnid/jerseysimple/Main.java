package org.araqnid.jerseysimple;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(Stage.PRODUCTION, new AppConfig(System.getenv()));

		configureJerseyLogging();

		ServiceManager serviceManager = injector.getInstance(ServiceManager.class);
		serviceManager.addListener(new ServiceManager.Listener() {
			@Override
			public void healthy() {
				LOG.info("Finished starting");
			}

			@Override
			public void failure(Service service) {
				LOG.error("Service failed: {}", service, service.failureCause());
				System.exit(1);
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}, "shutdown"));
		serviceManager.startAsync().awaitHealthy();
	}

	// make Jersey log through SLF4J
	private static void configureJerseyLogging() {
		java.util.logging.LogManager.getLogManager().reset();
		SLF4JBridgeHandler.install();
		java.util.logging.Logger.getGlobal().setLevel(java.util.logging.Level.FINEST);
		java.util.logging.Logger.getLogger("org.glassfish.jersey").setLevel(java.util.logging.Level.INFO);
	}
}
