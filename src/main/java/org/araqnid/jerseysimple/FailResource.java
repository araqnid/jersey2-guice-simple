package org.araqnid.jerseysimple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("fail")
public class FailResource {
	@GET
	public String fail() {
		throw new UnsupportedOperationException("fail");
	}
}
