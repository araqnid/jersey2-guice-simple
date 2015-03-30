package org.araqnid.jerseysimple;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.jersey.jdkhttp.JdkHttpHandlerContainer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;

public class JdkHttpServerService extends AbstractIdleService {
	private static final Logger LOG = LoggerFactory.getLogger(JdkHttpServerService.class);
	private final URI uri;
	private final HttpServer server;

	@Inject
	public JdkHttpServerService(Injector injector, @Port int port, @Resources Set<Class<?>> resourceClasses) {
		uri = UriBuilder.fromUri("http://localhost/").port(port).build();
		ResourceConfig resourceConfig = new ResourceConfig(ImmutableSet.<Class<?>> builder().addAll(resourceClasses)
				.add(EventListener.class).build());
		ServiceLocator serviceLocator = ServiceLocatorFactory.getInstance().create("http-" + port);
		GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
		serviceLocator.getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);
		server = createHttpServer(uri, resourceConfig, serviceLocator, false);
	}

	@Override
	protected void startUp() throws Exception {
		server.start();
		LOG.info("Started server at {}", uri);
	}

	@Override
	protected void shutDown() throws Exception {
		LOG.info("Stopping server at {}", uri);
		server.stop(0);
	}

	public static class EventListener implements ApplicationEventListener, RequestEventListener {

		@Override
		public void onEvent(ApplicationEvent event) {
		}

		@Override
		public RequestEventListener onRequest(RequestEvent requestEvent) {
			return this;
		}

		@SuppressWarnings("incomplete-switch")
		@Override
		public void onEvent(RequestEvent event) {
			switch (event.getType()) {
			case ON_EXCEPTION:
				LOG.error("exception handling request", event.getException());
			}
		}
	}

	// there is no method provided in JdkHttpServerFactory taking both parentLocator and start as parameters :(
	private static HttpServer createHttpServer(URI uri,
			ResourceConfig resourceConfig,
			ServiceLocator parentLocator,
			boolean start) {
		try {
			Constructor<JdkHttpHandlerContainer> constructor = JdkHttpHandlerContainer.class.getDeclaredConstructor(
					Application.class, ServiceLocator.class);
			constructor.setAccessible(true);
			JdkHttpHandlerContainer handler = constructor.newInstance(resourceConfig, parentLocator);

			Method factoryMethod = JdkHttpServerFactory.class.getDeclaredMethod("createHttpServer", URI.class,
					JdkHttpHandlerContainer.class, HttpsConfigurator.class, Boolean.TYPE);
			factoryMethod.setAccessible(true);
			return (HttpServer) factoryMethod.invoke(null, uri, handler, null, start);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException("Unable to naughtily create server with reflection", e);
		}
	}
}
