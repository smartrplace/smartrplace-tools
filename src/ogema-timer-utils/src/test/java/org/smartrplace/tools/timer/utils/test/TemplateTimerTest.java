package org.smartrplace.tools.timer.utils.test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.ogema.core.administration.FrameworkClock;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.exam.latest.LatestVersionsTestBase;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.smartrplace.tools.timer.utils.model.DayTemplateProviderData;
import org.smartrplace.tools.timer.utils.templates.TemplateManagement;
import org.smartrplace.tools.timer.utils.templates.TemplateTimer;
import org.smartrplace.tools.timer.utils.templates.TemplateTimer.TemplateTimerListener;
import org.smartrplace.tools.timer.utils.templates.resource.ResourceTemplateManagement;
import org.smartrplace.tools.timer.utils.templates.simple.SimpleTemplateManagement;
import org.smartrplace.tools.timer.utils.templates.simple.SimpleTemplateTimer;

/**
 * Note: these tests are timing sensitive and may fail sporadically.
 * 
 * @author cnoelle
 *
 */
@ExamReactorStrategy(PerClass.class)
public class TemplateTimerTest extends LatestVersionsTestBase {
	
	private static final ZoneId zone = ZoneId.of("Z");
	
	public TemplateTimerTest() {
		super(true);
	}
	
	private <T> T executeAtSimulationSpeed(final float simulationFactor, final Callable<T> task) {
		final FrameworkClock clock = getApplicationManager().getAdministrationManager().getFrameworkClock();
		clock.setSimulationFactor(simulationFactor);
		try {
			return task.call();
		} catch (Exception e) {
			throw new AssertionError("Exception in task execution", e);
		} finally {
			clock.setSimulationFactor(1);
		}
	}
	
	private void templateWorksSingleDay(final Map<LocalTime, Integer> map, final DayOfWeek day, final boolean persistent) {
		final TemplateManagement<Integer> templateMgmt;
		if (persistent) {
			final DayTemplateProviderData data = getApplicationManager().getResourceManagement().createResource(newResourceName(), DayTemplateProviderData.class);
			templateMgmt = new ResourceTemplateManagement<IntegerResource, Integer>(data, IntegerResource.class);
		} else {
			 templateMgmt = new SimpleTemplateManagement<Integer>();
		}
		if (day == null)
			templateMgmt.addDefaultValues(map);
		else
			templateMgmt.addValues(day, map);
		// start at 1970-01-01
		getApplicationManager().getAdministrationManager().getFrameworkClock().setSimulationTime(-1);
		final TestListener listener = new TestListener(map.size(), 3600 * 1000); // start recording callbacks after one hour
		final SimpleTemplateTimer<Integer> timer = new SimpleTemplateTimer<Integer>(getApplicationManager(), templateMgmt.getPovider(), null, zone);
		try {
			timer.addTemplateTimerListener(listener); // FIXME add constrcutor to add template listener immediately
			final float factor = 24 * 60 * 60 / 10; // factor to let one day pass in 10 seconds
			final boolean done = executeAtSimulationSpeed(factor, () -> listener.await(20, TimeUnit.SECONDS)); // expected: 10s
			timer.stop();
			Assert.assertTrue("Timer listener not called", done);
			Assert.assertEquals("Unexpected number of listener values", map.size(), listener.values.size());
			final ZonedDateTime day0 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), zone);
			final Iterator<Map.Entry<ZonedDateTime, Integer>> it = listener.values.entrySet().iterator();
			map.entrySet()
				.forEach(entry -> {
					Assert.assertTrue("Callback entry missing", it.hasNext());
					final Map.Entry<ZonedDateTime, Integer> actual = it.next();
					final ZonedDateTime expected = day0.with(entry.getKey());
					final Duration diff = Duration.between(expected, actual.getKey());
					Assert.assertTrue("Distance between expected callback time and actual time too large: " + expected + ": " + actual, 
							diff.compareTo(Duration.ofHours(1)) < 0);
					Assert.assertEquals("Unexpected callback value", entry.getValue(), actual.getValue());
				});
		} finally {
			timer.destroy();
		}
	}

	private void templatingWorksDefaultSingleDay(boolean persistent) {
		final AtomicInteger cnt = new AtomicInteger(0);
		final Map<LocalTime, Integer> map = new TreeMap<>(Stream.<LocalTime> builder()
				.add(LocalTime.of(3, 17))
				.add(LocalTime.of(7, 23))
				.add(LocalTime.of(14, 23))
				.build()
				.collect(Collectors.toMap(Function.identity(), (d) -> cnt.getAndIncrement())));
		templateWorksSingleDay(map, null, persistent);
	}
	
	@Test
	public void templatingWorksDefaultSingleDayPersistent() {
		templatingWorksDefaultSingleDay(true);
	}
	
	@Test
	public void templatingWorksDefaultSingleDayNonPersistent() {
		templatingWorksDefaultSingleDay(false);
	}
	
	private void templatingWorksSpecialDay(boolean persistent) {
		final AtomicInteger cnt = new AtomicInteger(0);
		final Map<LocalTime, Integer> map = new TreeMap<>(Stream.<LocalTime> builder()
				.add(LocalTime.of(3, 17))
				.add(LocalTime.of(7, 23))
				.add(LocalTime.of(14, 23))
				.build()
				.collect(Collectors.toMap(Function.identity(), (d) -> cnt.getAndIncrement())));
		templateWorksSingleDay(map, DayOfWeek.THURSDAY, persistent);
	}
	
	@Test
	public void templatingWorksSpecialDayPersistent() {
		templatingWorksSpecialDay(true);
	}
	
	@Test
	public void templatingWorksSpecialDayNonPersistent() {
		templatingWorksSpecialDay(false);
	}
	
	@Test
	public void templatingWorksMultipleDaysPersistent() {
		templatingWorksMultipleDays(true);
	}

	@Test
	public void templatingWorksMultipleDaysNonPersistent() {
		templatingWorksMultipleDays(false);
	}
	
	private void templatingWorksMultipleDays(final boolean persistent) { 
		final TemplateManagement<Integer> templateMgmt;
		if (persistent) {
			final DayTemplateProviderData data = getApplicationManager().getResourceManagement().createResource(newResourceName(), DayTemplateProviderData.class);
			templateMgmt = new ResourceTemplateManagement<IntegerResource, Integer>(data, IntegerResource.class);
		} else {
			 templateMgmt = new SimpleTemplateManagement<Integer>();
		}
		final AtomicInteger cnt = new AtomicInteger(0);
		final Map<LocalTime, Integer> defaultDayValues = new TreeMap<>(Stream.<LocalTime> builder()
				.add(LocalTime.of(3, 17))
				.add(LocalTime.of(7, 23))
				.add(LocalTime.of(14, 23))
				.build()
				.collect(Collectors.toMap(Function.identity(), (d) -> cnt.getAndIncrement())));
		final Map<LocalTime, Integer> thursdayValues = new TreeMap<>(Stream.<LocalTime> builder()
				.add(LocalTime.of(2, 42))
				.add(LocalTime.of(12, 01))
				.add(LocalTime.of(20, 59))
				.build()
				.collect(Collectors.toMap(Function.identity(), (d) -> cnt.getAndIncrement())));
		final Map<LocalTime, Integer> saturdayValues = new TreeMap<>(Stream.<LocalTime> builder()
				.add(LocalTime.of(4, 30))
				.add(LocalTime.of(10, 45))
				.add(LocalTime.of(19, 15))
				.build()
				.collect(Collectors.toMap(Function.identity(), (d) -> cnt.getAndIncrement())));
		templateMgmt.addValues(DayOfWeek.THURSDAY, thursdayValues);
		templateMgmt.addValues(DayOfWeek.SATURDAY, saturdayValues);
		templateMgmt.addDefaultValues(defaultDayValues);
		// start at 1970-01-01
		final int sz = defaultDayValues.size() + thursdayValues.size() + saturdayValues.size();
		getApplicationManager().getAdministrationManager().getFrameworkClock().setSimulationTime(-1);
		final TestListener listener = new TestListener(sz, 3600 * 1000);  // start recording callbacks after one hour
		final SimpleTemplateTimer<Integer> timer = new SimpleTemplateTimer<Integer>(getApplicationManager(), templateMgmt.getPovider(), null, zone);
		try {
			timer.addTemplateTimerListener(listener); // FIXME add constrcutor to add template listener immediately
			final float factor = 24 * 60 * 60 / 10; // factor to let one day pass in 5 seconds
			final boolean done = executeAtSimulationSpeed(factor, () -> listener.await(60, TimeUnit.SECONDS)); // expected: 30s
			Assert.assertTrue("Timer listener not called as often as expected: " + listener.getCount() + " (" + sz + ")", done);
			Assert.assertEquals("Unexpected number of listener values", sz, listener.values.size());
			final ZonedDateTime day0 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), zone);
			final Iterator<Map.Entry<ZonedDateTime, Integer>> it = listener.values.entrySet().iterator();
			thursdayValues.entrySet().forEach(entry -> {
				Assert.assertTrue("Callback entry missing", it.hasNext());
				final Map.Entry<ZonedDateTime, Integer> actual = it.next();
				final ZonedDateTime expected = day0.with(entry.getKey());
				final Duration diff = Duration.between(expected, actual.getKey());
				Assert.assertTrue("Distance between expected callback time and actual time too large: " + expected + ": " + actual, 
						diff.compareTo(Duration.ofHours(1)) < 0);
				Assert.assertEquals("Unexpected callback value", entry.getValue(), actual.getValue());
			});
			final ZonedDateTime day1 = day0.plus(1, ChronoUnit.DAYS);
			defaultDayValues.entrySet()
				.forEach(entry -> {
					Assert.assertTrue("Callback entry missing", it.hasNext());
					final Map.Entry<ZonedDateTime, Integer> actual = it.next();
					final ZonedDateTime expected = day1.with(entry.getKey());
					final Duration diff = Duration.between(expected, actual.getKey());
					Assert.assertTrue("Distance between expected callback time and actual time too large: " + expected + ": " + actual, 
							diff.compareTo(Duration.ofHours(1)) < 0);
					Assert.assertEquals("Unexpected callback value", entry.getValue(), actual.getValue());
				});
			final ZonedDateTime day2 = day1.plus(1, ChronoUnit.DAYS);
			saturdayValues.entrySet()
				.forEach(entry -> {
					Assert.assertTrue("Callback entry missing", it.hasNext());
					final Map.Entry<ZonedDateTime, Integer> actual = it.next();
					final ZonedDateTime expected = day2.with(entry.getKey());
					final Duration diff = Duration.between(expected, actual.getKey());
					Assert.assertTrue("Distance between expected callback time and actual time too large: " + expected + ": " + actual, 
							diff.compareTo(Duration.ofHours(1)) < 0);
					Assert.assertEquals("Unexpected callback value", entry.getValue(), actual.getValue());
				});
		} finally {
			timer.destroy();
		}
	}
	
	private static class TestListener implements TemplateTimerListener<Integer> {
		
		private final CountDownLatch latch;
		final NavigableMap<ZonedDateTime, Integer> values = new ConcurrentSkipListMap<>();
		private final long startTime;
		
		TestListener(int expectedCallbacks, long startTime) {
			this.latch = new CountDownLatch(expectedCallbacks);
			this.startTime = startTime;
		}

		@Override
		public void timerElapsed(TemplateTimer<Integer> timer, Integer value) {
			if (timer.getExecutionTime() < startTime)
				return;
			values.put(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timer.getExecutionTime()), zone), value);
//			System.out.println("  &&//\\&& timer listener called, values " + values);
			latch.countDown();
		}
		
		boolean await(long timeout, TimeUnit unit) {
			try {
				return latch.await(timeout, unit);
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
		}
		
		long getCount() {
			return latch.getCount();
		}
		
	}
	
}
