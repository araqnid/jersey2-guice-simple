package org.araqnid.jerseysimple;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;

public class JettyServletService extends AbstractIdleService {
	private static final AtomicInteger SERVER_INDEX = new AtomicInteger();
	private final Server server;

	@Inject
	public JettyServletService(Injector injector, @Port int port, @Resources Set<Class<?>> resourceClasses) {
		URI baseUri = UriBuilder.fromUri("http://localhost/").port(port).build();
		ResourceConfig config = new ResourceConfig(resourceClasses);
		String serverName = "servlet-" + port + "-" + SERVER_INDEX.getAndIncrement();
		ServiceLocator serviceLocator = ServiceLocatorFactory.getInstance().create(serverName);
		GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
		serviceLocator.getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);

		ServletContextHandler context = new ServletContextHandler();
		context.getServletContext().setAttribute(ServletProperties.SERVICE_LOCATOR, serviceLocator);

		ServletHolder servletHolder = new ServletHolder(new ServletContainer(config));
		context.addServlet(servletHolder, "/*");

		server = JettyHttpContainerFactory.createServer(baseUri, false);
		server.setHandler(context);
		server.getBean(QueuedThreadPool.class).setName("Jetty-" + serverName);
	}

	@Override
	protected void startUp() throws Exception {
		server.start();
	}

	@Override
	protected void shutDown() throws Exception {
		server.stop();
	}
}
