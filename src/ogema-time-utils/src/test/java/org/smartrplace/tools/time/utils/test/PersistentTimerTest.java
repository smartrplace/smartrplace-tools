package org.smartrplace.tools.time.utils.test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.ogema.core.administration.FrameworkClock;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.exam.latest.LatestVersionsTestBase;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.smartrplace.tools.time.utils.PeriodTimerPersistent;
import org.smartrplace.tools.time.utils.model.TimerData;

/*
 * Note: these tests are timing sensitive... therefore it cannot be guaranteed
 * that they always succeed.
 */
@ExamReactorStrategy(PerClass.class)
public class PersistentTimerTest extends LatestVersionsTestBase {
	
	public PersistentTimerTest() {
		super(true);
	}
	 
	private void executeAtSimulationSpeed(final float simulationFactor, final Runnable task) {
		final FrameworkClock clock = getApplicationManager().getAdministrationManager().getFrameworkClock();
		clock.setSimulationFactor(simulationFactor);
		try {
			task.run();
		} finally {
			clock.setSimulationFactor(1);
		}
	}

	private void testTimer(final ChronoUnit chronoUnit, final int factor, final int expectedExecutions, 
			final long timeout, final TimeUnit unit, final boolean useInitConstructor) {
		final TimerData config = getApplicationManager().getResourceManagement().createResource(newResourceName(), TimerData.class);
		if (!useInitConstructor) {
			config.period().chronoUnit().<StringResource> create().setValue(chronoUnit.name());
			config.period().factor().<IntegerResource> create().setValue(factor);
			config.activate(true);
		}
		final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), expectedExecutions);
		final PeriodTimerPersistent timer = !useInitConstructor ? new PeriodTimerPersistent(getApplicationManager(), config, listener)
				: new PeriodTimerPersistent(getApplicationManager(), config, chronoUnit, factor, null, listener);
		try {
			Assert.assertTrue("Timer listener not called", listener.await(timeout, unit));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted");
		} finally {
			timer.destroy();
		}
	}
	
	@Test
	public void periodicTimerWorksDurationBased() {
		testTimer(ChronoUnit.SECONDS, 2, 2, 8, TimeUnit.SECONDS, false);
	}
	
	@Test
	public void periodicTimerWorksDurationBasedWithInitConstructor() {
		testTimer(ChronoUnit.SECONDS, 2, 2, 8, TimeUnit.SECONDS, true);
	}
	
	@Test
	public void periodicTimerWorksPeriodBased() {
		final float simFactor = 24 * 60 * 1000 / 3; // simulation factor that makes a day pass in 3 seconds
		executeAtSimulationSpeed(simFactor, () -> testTimer(ChronoUnit.DAYS, 1, 2, 15, TimeUnit.SECONDS, false));
	}
	
	@Test
	public void endTimeWorks() {
		final TimerData config = getApplicationManager().getResourceManagement().createResource(newResourceName(), TimerData.class);
		config.period().chronoUnit().<StringResource> create().setValue(ChronoUnit.SECONDS.name());
		config.period().factor().<IntegerResource> create().setValue(1);
		final long now = System.currentTimeMillis();
		config.endTime().<TimeResource> create().setValue(now + 3500);
		config.activate(true);
		final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), 4);
		final PeriodTimerPersistent timer = new PeriodTimerPersistent(getApplicationManager(), config, listener);
		try {
			Assert.assertFalse("Timer listener called too often", listener.await(6, TimeUnit.SECONDS));
			Assert.assertTrue("Listener not called at all", listener.getCount() > 0);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted");
		} finally {
			timer.destroy();
		}
	}

	@Test
	public void configChangesWork0() {
		final TimerData config = getApplicationManager().getResourceManagement().createResource(newResourceName(), TimerData.class);
		// we do not activate the factor subresource
		config.period().chronoUnit().<StringResource> create().setValue(ChronoUnit.SECONDS.name());
		config.activate(true);
		final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), 1);
		final PeriodTimerPersistent timer = new PeriodTimerPersistent(getApplicationManager(), config, listener);
		try {
			Assert.assertFalse("Timer executed despite missing config",listener.await(2, TimeUnit.SECONDS));
			config.period().factor().<IntegerResource> create().setValue(1);
			config.period().factor().activate(false);
			Assert.assertTrue("Timer listener not called", listener.await(3, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted");
		} finally {
			timer.destroy();
		}
	}
	
	@Test
	public void stopResumeWorks() throws InterruptedException, ExecutionException, TimeoutException {
		final TimerData config = getApplicationManager().getResourceManagement().createResource(newResourceName(), TimerData.class);
		config.period().chronoUnit().<StringResource> create().setValue(ChronoUnit.SECONDS.name());
		config.period().factor().<IntegerResource> create().setValue(1);
		config.activate(true);
		final PeriodTimerPersistent timer = new PeriodTimerPersistent(getApplicationManager(), config);
		try {
			getApplicationManager().submitEvent(() -> {
				timer.stop();
				return (Void) null;
			}).get(5, TimeUnit.SECONDS);
			final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), 1);
			timer.addListener(listener);
			Assert.assertFalse("Timer executed although it has been stopped", listener.await(2, TimeUnit.SECONDS));
			timer.resume();
			Assert.assertTrue("Timer listener not called", listener.await(3, TimeUnit.SECONDS));
		} finally {
			timer.destroy();
		}
	}
	
	@Test
	public void stopResumeWorksViaActivation() throws InterruptedException {
		final TimerData config = getApplicationManager().getResourceManagement().createResource(newResourceName(), TimerData.class);
		config.period().chronoUnit().<StringResource> create().setValue(ChronoUnit.SECONDS.name());
		config.period().factor().<IntegerResource> create().setValue(1);
//		config.activate(true); // we leave it deliberately inactive
		final PeriodTimerPersistent timer = new PeriodTimerPersistent(getApplicationManager(), config);
		try {
			final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), 1);
			timer.addListener(listener);
			Assert.assertFalse("Timer executed although its config resource is inactive", listener.await(2, TimeUnit.SECONDS));
			config.activate(true);
			Assert.assertTrue("Timer listener not called", listener.await(5, TimeUnit.SECONDS));
		} finally {
			timer.destroy();
		}
	}
	
	@Test
	public void stopWorksViaDeactivation() throws InterruptedException {
		final TimerData config = getApplicationManager().getResourceManagement().createResource(newResourceName(), TimerData.class);
		config.period().chronoUnit().<StringResource> create().setValue(ChronoUnit.SECONDS.name());
		config.period().factor().<IntegerResource> create().setValue(1);
		config.activate(true);
		final PeriodTimerPersistent timer = new PeriodTimerPersistent(getApplicationManager(), config);
		try {
			final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), 1);
			timer.addListener(listener);
			Assert.assertTrue("Timer listener not called", listener.await(5, TimeUnit.SECONDS));
			config.deactivate(false);
			Thread.sleep(50); // there might be pending timer callbacks...
			listener.reset(1);
			Assert.assertFalse("Timer listener called although its config resource has been deactivated", listener.await(2, TimeUnit.SECONDS));
		} finally {
			timer.destroy();
		}
	}
	
	
	private static class TestListener implements TimerListener {
		
		private final long startTime;
		private volatile CountDownLatch latch;
		
		TestListener(long start, int expectedCallbacks) {
			this.startTime = start;
			this.latch = new CountDownLatch(expectedCallbacks);
		}
		
		public void reset(int expectedCallbacks) {
			this.latch = new CountDownLatch(expectedCallbacks);
		}
		
		@Override
		public void timerElapsed(Timer timer) {
			System.out.println(" ~ Timer elapsed after " + ((timer.getExecutionTime() - startTime)/1000) + "s.");
			latch.countDown();
		}
		
		boolean await(long time, TimeUnit unit) throws InterruptedException {
			return latch.await(time, unit);
		}
		
		long getCount() {
			return latch.getCount();
		}
		
	}
	

}
