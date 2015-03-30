package org.araqnid.jerseysimple;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

@Path("sleep")
public class SleepResource {
	private static final Logger LOG = LoggerFactory.getLogger(SleepResource.class);
	private final ScheduledExecutorService executor;

	@Inject
	public SleepResource(@ThreadPool ScheduledExecutorService executor) {
		this.executor = executor;
	}

	@GET
	public void sleep(@Suspended final AsyncResponse asyncResponse) {
		LOG.info("entering sleep()");
		executor.schedule(() -> {
			LOG.info("scheduled action");
			asyncResponse.resume("completed on " + Thread.currentThread().getName());
		}, 1, TimeUnit.SECONDS);
		LOG.info("exiting sleep()");
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
	@Documented
	public @interface ThreadPool {
	}

	public static class ThreadPoolCleanupService extends AbstractIdleService {
		private final ScheduledExecutorService executor;

		@Inject
		public ThreadPoolCleanupService(@ThreadPool ScheduledExecutorService executor) {
			this.executor = executor;
		}

		@Override
		protected void startUp() throws Exception {
		}

		@Override
		protected void shutDown() throws Exception {
			shutdownAndAwaitTermination(executor, 5, TimeUnit.SECONDS);
		}
	}
}
