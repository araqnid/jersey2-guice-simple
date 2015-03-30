package org.araqnid.jerseysimple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class RootResource {
	@GET
	@Produces("text/html")
	public String root() {
		return "<h1>Root</h1>";
	}
}
