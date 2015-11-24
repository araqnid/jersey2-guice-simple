package org.araqnid.jerseysimple;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.araqnid.jerseysimple.AppConfig.ServerFlavour;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.base.Throwables;

@RunWith(Parameterized.class)
public class ServerIntegrationTest {
	@Parameters(name = "{0}")
	public static AppConfig.ServerFlavour[] combinations() {
		return AppConfig.ServerFlavour.values();
	}

	@Parameter
	public AppConfig.ServerFlavour serverFlavour;

	@Test
	public void root() throws Exception {
		try (ServerRunner server = makeServer()) {
			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				try (CloseableHttpResponse resp = httpClient.execute(new HttpGet(server.uri("/")))) {
					assertThat(resp.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
				}
			}
		}
	}

	@Test
	public void server_version() throws Exception {
		try (ServerRunner server = makeServer()) {
			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				try (CloseableHttpResponse resp = httpClient.execute(new HttpGet(server.uri("/info/version")))) {
					assertThat(resp.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
					assertThat(resp.getEntity().getContentType().getValue(), equalTo("text/plain"));
				}
			}
		}
	}

	@Test
	public void sleep() throws Exception {
		assumeThat("suspending requests not supported with JDK server", serverFlavour,
				is(not(equalTo(ServerFlavour.JDK))));
		try (ServerRunner server = makeServer()) {
			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				try (CloseableHttpResponse resp = httpClient.execute(new HttpGet(server.uri("/sleep")))) {
					assertThat(resp.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
					assertThat(resp.getEntity().getContentType().getValue(), equalTo("text/plain"));
				}
			}
		}
	}

	@Test
	public void json_info() throws Exception {
		try (ServerRunner server = makeServer()) {
			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				try (CloseableHttpResponse resp = httpClient.execute(new HttpGet(server.uri("/info/_all")))) {
					assertThat(resp.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
					assertThat(resp.getEntity().getContentType().getValue(), equalTo("application/json"));
					ObjectMapper mapper = new ObjectMapper();
					Map<String, String> info = mapper.reader(new TypeReference<Map<String, String>>() {
					}).readValue(resp.getEntity().getContent());
					assertThat(info, IsMapContaining.hasEntry("version", "0.0.0"));
				}
			}
		}
	}

	private ServerRunner makeServer() {
		return new ServerRunner(ImmutableMap.of("FLAVOUR", serverFlavour.toString()));
	}

	private static final class ServerRunner implements Closeable {
		private final Injector injector;

		public ServerRunner(Map<String, String> environment) {
			this.injector = Guice.createInjector(new AppConfig(environment));
			getInstance(ServiceManager.class).startAsync().awaitHealthy();
		}

		@Override
		public void close() {
			getInstance(ServiceManager.class).stopAsync().awaitStopped();
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
