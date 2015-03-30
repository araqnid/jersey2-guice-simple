package org.araqnid.jerseysimple;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
}
