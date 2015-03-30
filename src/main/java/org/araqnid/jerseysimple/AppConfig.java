package org.araqnid.jerseysimple;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

import org.eclipse.jetty.util.Jetty;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class AppConfig extends AbstractModule {
	private final Map<String, String> environment;

	public AppConfig(Map<String, String> environment) {
		this.environment = ImmutableMap.copyOf(environment);
	}

	@Override
	protected void configure() {
		bindConstant().annotatedWith(Port.class).to(port());
		bindConstant().annotatedWith(AppVersion.class).to("0.0.0");

		services().addBinding().to(SleepResource.ThreadPoolCleanupService.class);

		switch (serverFlavour().orElse(ServerFlavour.JDK)) {
		case JETTY:
			install(new JettyModule());
			break;
		case JDK:
			install(new JdkHttpServerModule());
			break;
		}

		bindResourceClasses(SleepResource.class, FailResource.class, InfoResources.class, RootResource.class);
	}

	@Provides
	@Singleton
	public ServiceManager serviceManager(Set<Service> services) {
		return new ServiceManager(services);
	}

	@Provides
	@SleepResource.ThreadPool
	@Singleton
	public ScheduledExecutorService sleepResourceThreadPool() {
		return Executors
				.newScheduledThreadPool(4, new ThreadFactoryBuilder().setNameFormat("SleepResource-%d").build());
	}

	private Optional<String> getenv(String name) {
		return Optional.ofNullable(environment.get(name));
	}

	private Optional<ServerFlavour> serverFlavour() {
		return getenv("FLAVOUR").map(str -> ServerFlavour.valueOf(str.toUpperCase()));
	}

	private int port() {
		return getenv("PORT").map(str -> Integer.valueOf(str)).orElse(65300);
	}

	private Multibinder<Service> services() {
		return Multibinder.newSetBinder(binder(), Service.class);
	}

	private void bindResourceClasses(Class<?>... resourceClasses) {
		bind(new TypeLiteral<Set<Class<?>>>() {
		}).annotatedWith(Resources.class).toInstance(ImmutableSet.copyOf(resourceClasses));
		for (Class<?> resourceClass : resourceClasses) {
			bind(resourceClass);
		}
	}

	public enum ServerFlavour {
		JETTY, JDK;
	}

	public static class JettyModule extends AbstractModule {
		@Override
		protected void configure() {
			services().addBinding().to(JettyService.class);
			bindConstant().annotatedWith(ServerInfo.class).to("Jetty/" + Jetty.VERSION);
		}

		private Multibinder<Service> services() {
			return Multibinder.newSetBinder(binder(), Service.class);
		}
	}

	public static class JdkHttpServerModule extends AbstractModule {
		@Override
		protected void configure() {
			services().addBinding().to(JdkHttpServerService.class);
			bindConstant().annotatedWith(ServerInfo.class).to("jdk-http/" + System.getProperty("java.version"));
		}

		private Multibinder<Service> services() {
			return Multibinder.newSetBinder(binder(), Service.class);
		}
	}
}
