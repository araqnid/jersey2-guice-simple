package org.araqnid.jerseysimple;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.google.common.collect.ImmutableMap;

@Path("info")
public class InfoResources {
	private final String version;
	private final String serverInfo;

	@Inject
	public InfoResources(@AppVersion String version, @ServerInfo String serverInfo) {
		this.version = version;
		this.serverInfo = serverInfo;
	}

	@GET
	@Path("version")
	@Produces("text/plain")
	public String version() {
		return version;
	}

	@GET
	@Path("server")
	@Produces("text/plain")
	public String serverInfo() {
		return serverInfo;
	}

	@GET
	@Path("_all")
	@Produces("application/json")
	public Map<String, String> combinedInfo() {
		return ImmutableMap.of("version", version, "server", serverInfo);
	}
}
