package org.araqnid.jerseysimple;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.base.Throwables;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(Theories.class)
public class ServerIntegrationTest {
	@DataPoints
	public static List<String> paths = ImmutableList.of("/", "/info/version", "/info/server");

	@DataPoints
	public static Set<AppConfig.ServerFlavour> serverFlavours = EnumSet.allOf(AppConfig.ServerFlavour.class);

	@Theory
	public void get_content(String path, AppConfig.ServerFlavour serverFlavour) throws Exception {
		try (ServerRunner server = new ServerRunner(ImmutableMap.of("FLAVOUR", serverFlavour.toString()))) {
			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				try (CloseableHttpResponse resp = httpClient.execute(new HttpGet(server.uri(path)))) {
					assertThat(resp.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
				}
			}
		}
	}

	public static class ServerRunner implements Closeable {
		private final Injector injector;

		public ServerRunner(Map<String, String> environment) {
			this.injector = Guice.createInjector(new AppConfig(environment));
			getInstance(ServiceManager.class).startAsync().awaitHealthy();
		}

		@Override
		public void close() {
			getInstance(ServiceManager.class).stopAsync().awaitStopped();
		}

		public URI uri() {
			try {
				return new URIBuilder().setScheme("http").setHost("localhost").setPort(port()).build();
			} catch (URISyntaxException e) {
				throw Throwables.propagate(e);
			}
		}

		public URI uri(String path) {
			Preconditions.checkArgument(path.startsWith("/"));
			try {
				return new URIBuilder().setScheme("http").setHost("localhost").setPort(port()).setPath(path).build();
			} catch (URISyntaxException e) {
				throw Throwables.propagate(e);
			}
		}

		public int port() {
			return getInstance(Integer.class, Port.class);
		}

		public <T> T getInstance(Class<T> clazz) {
			return injector.getInstance(clazz);
		}

		public <T> T getInstance(Class<T> clazz, Class<? extends Annotation> annotation) {
			return injector.getInstance(Key.get(clazz, annotation));
		}
	}
}
