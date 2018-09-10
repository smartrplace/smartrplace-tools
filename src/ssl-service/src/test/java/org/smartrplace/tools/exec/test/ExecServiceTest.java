package org.smartrplace.tools.exec.test;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.smartrplace.tools.ssl.SslConstants;

@ExamReactorStrategy(PerClass.class)
@RunWith(PaxExam.class)
public class ExecServiceTest {

	private static final String slf4jVersion = "1.7.25";
	
	private static final Dictionary<String, Object> getProperties(long delay, long period) {
		final Dictionary<String, Object> dict = new Hashtable<>(4);
		dict.put(SslConstants.TASK_DELAY, period);
		dict.put(SslConstants.TASK_PERIOD, period);
		return dict;
	}
	
	@Inject
	private BundleContext ctx;

	@Configuration
	public Option[] configuration() throws IOException {
		return new Option[] { CoreOptions.composite(bundles()) };
	}

	public Option[] bundles() throws IOException {
		return new Option[] {
				// CoreOptions.cleanCaches(),
				CoreOptions.frameworkProperty("org.osgi.framework.system.packages.extra")
						.value("sun.reflect,sun.misc,javax.annotation"),
				CoreOptions.vmOption("-ea"), CoreOptions.junitBundles(),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.1.0"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.9.4"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configurator", "1.0.0"),
//				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.5.0"),
//				CoreOptions.mavenBundle("org.osgi", "org.osgi.service.useradmin", "1.1.0"),
//				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin", "1.0.3"),
//				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin.filestore", "1.0.2"),
				//
				CoreOptions.mavenBundle("org.slf4j", "slf4j-api", slf4jVersion),
				CoreOptions.mavenBundle("org.slf4j", "osgi-over-slf4j", slf4jVersion),
				CoreOptions.mavenBundle("org.slf4j", "slf4j-simple", slf4jVersion).noStart(),
				CoreOptions.mavenBundle("org.smartrplace.tools", "executor-service", "0.0.1-SNAPSHOT").start(),
				CoreOptions.systemProperty("configurator.initial").value(configProperty().toString().replace('=', ':')) };
	}

	// here we use the default values everywhere
	protected Map<String, Object> configProperty() {
		final Map<String, Map<String, Object>> properties0 = Arrays
				.stream(new String[] { SslConstants.HOUSEKEEPING_EXEC_PID })
				.collect(Collectors.toMap(str -> "\n\"" + str + "\"", str -> Collections.<String, Object> emptyMap()));
		final Map<String, Object> properties = new HashMap<>(properties0);
		properties.put("\n\":configurator:version\"", "\"1\"");
		properties.put("\n\":configurator:symbolic-name\"", "\"initConfig\"");
		return properties;
	}
	
	@Test
	public void startupTest() {
		final Bundle bundle = Arrays.stream(ctx.getBundles())
			.filter(b -> "org.smartrplace.tools.executor-service".equals(b.getSymbolicName()))
			.findAny().orElseThrow(() -> new AssertionError("Executor service bundle not found"));
		final int state = bundle.getState();
		Assert.assertTrue("Bundle not active: " + state, state == Bundle.ACTIVE || state == Bundle.RESOLVED);
	}
	
	private void taskExecutionWorks(final int nrExecutions, final long period) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(nrExecutions);
		final Runnable r = () -> latch.countDown();
		final ServiceRegistration<?> reg = ctx.registerService(Runnable.class, r, getProperties(period, period));
		try {
			Assert.assertTrue("Task has not been executed", latch.await(period * nrExecutions + 2000, TimeUnit.MILLISECONDS));
		} finally {
			reg.unregister();
		}
	}
	
	@Test
	public void execWorks1() throws InterruptedException {
		taskExecutionWorks(1, 100);
	}
	
	@Test
	public void execWorks2() throws InterruptedException {
		taskExecutionWorks(4, 100);
	}
	
	@Test
	public void repeatedSubmissionWorks() throws InterruptedException {
		final CountDownLatch initialLatch = new CountDownLatch(2);
		final AtomicReference<CountDownLatch> ref = new AtomicReference<CountDownLatch>(initialLatch);
		final Runnable r = () -> ref.get().countDown();
		final Dictionary<String, Object> dict = getProperties(100, 100);
		final ServiceRegistration<?> reg = ctx.registerService(Runnable.class, r, dict);
		try {
			Assert.assertTrue("Task has not been executed", initialLatch.await(2000, TimeUnit.MILLISECONDS));
		} finally {
			reg.unregister();
		}
		Thread.sleep(200);
		final CountDownLatch newLatch = new CountDownLatch(2);
		ref.set(newLatch);
		final ServiceRegistration<?> reg2 = ctx.registerService(Runnable.class, r, dict);
		try {
			Assert.assertTrue("Task has not been executed", newLatch.await(2000, TimeUnit.MILLISECONDS));
		} finally {
			reg2.unregister();
		}
	}
	
	@Test
	public void taskIsNotExecutedAfterUnregistering() throws InterruptedException {
		final CountDownLatch initialLatch = new CountDownLatch(2);
		final AtomicReference<CountDownLatch> ref = new AtomicReference<CountDownLatch>(initialLatch);
		final Runnable r = () -> ref.get().countDown();
		final Dictionary<String, Object> dict = getProperties(100, 100);
		final ServiceRegistration<?> reg = ctx.registerService(Runnable.class, r, dict);
		try {
			Assert.assertTrue("Task has not been executed", initialLatch.await(2000, TimeUnit.MILLISECONDS));
		} finally {
			reg.unregister();
		}
		Thread.sleep(200); // grace period(?)
		final CountDownLatch newLatch = new CountDownLatch(1);
		ref.set(newLatch);
		Assert.assertFalse("Task has been executed although it is no longer registered as a service", newLatch.await(2000, TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void parallelExecWorks() throws InterruptedException {
		final AtomicReference<Runnable> ref = new AtomicReference<>(null);
		final CountDownLatch latch = new CountDownLatch(10);
		final AtomicInteger errorCount = new AtomicInteger(0);
		final Runnable r1 = new ParallelTask(ref, latch, errorCount);
		final Runnable r2 = new ParallelTask(ref, latch, errorCount);
		final Dictionary<String, Object> dict = getProperties(100, 100);
		// we register two tasks with the same period and delay in parallel, and expect them to run in alternating order
		final ServiceRegistration<?> reg1 = ctx.registerService(Runnable.class, r1, dict);
		final ServiceRegistration<?> reg2 = ctx.registerService(Runnable.class, r2, dict);
		try {
			Assert.assertTrue("Task execution not finished", latch.await(5000, TimeUnit.MILLISECONDS));
		} finally {
			reg1.unregister();
			reg2.unregister();
		}
		Assert.assertEquals("Alternating task execution failed " + errorCount.get() + " times.", 0, errorCount.get());
	}
	
	@Test
	public void unitWorks() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final Runnable r = () -> latch.countDown();
		final Dictionary<String, Object> dict = getProperties(1, 1);
		dict.put(SslConstants.TASK_PROPERTIES_TIME_UNIT, ChronoUnit.SECONDS.name());
		final long start = System.nanoTime() / 1000000;
		final ServiceRegistration<?> reg = ctx.registerService(Runnable.class, r, dict);
		try {
			Assert.assertTrue("Task has not been executed", latch.await(3, TimeUnit.SECONDS));
			final long diff = System.nanoTime() / 1000000 - start;
			Assert.assertTrue("Task unit not working? Expected 1 second to elapse, was " + diff + " ms.", diff > 500);
		} finally {
			reg.unregister();
		}
	}

	private static class ParallelTask implements Runnable {
		
		private final AtomicReference<Runnable> ref;
		private final CountDownLatch latch;
		private final AtomicInteger errorCount;
		
		ParallelTask(AtomicReference<Runnable> ref, CountDownLatch latch, AtomicInteger errorCount) {
			this.ref = ref;
			this.latch = latch;
			this.errorCount = errorCount;
		}

		@Override
		public void run() {
			final Runnable last = ref.getAndSet(this);
			if (this == last) {
				System.out.println("Task executed twice in a row!");
				errorCount.getAndIncrement();
			}
			latch.countDown();
		}
		
	}
	

}
