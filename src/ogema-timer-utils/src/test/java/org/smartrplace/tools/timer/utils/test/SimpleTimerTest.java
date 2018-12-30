/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.tools.timer.utils.test;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.ogema.core.administration.FrameworkClock;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.exam.latest.LatestVersionsTestBase;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.smartrplace.tools.timer.utils.PeriodTimerSimple;

/*
 * Note: these tests are timing sensitive... therefore it cannot be guaranteed
 * that they always succeed.
 */
@ExamReactorStrategy(PerClass.class)
public class SimpleTimerTest extends LatestVersionsTestBase {
	
	public SimpleTimerTest() {
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

	private void testTimer(final TemporalAmount period, final int expectedExecutions, final long timeout, final TimeUnit unit) {
		final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), expectedExecutions);
		final PeriodTimerSimple timer = PeriodTimerSimple.builder(getApplicationManager(), period)
			.addListener(listener)
			.build();
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
		testTimer(Duration.ofSeconds(2), 2, 8, TimeUnit.SECONDS);
	}
	
	@Test
	public void periodicTimerWorksPeriodBased() {
		final float simFactor = 24 * 60 * 1000 / 3; // simulation factor that makes a day pass in 3 seconds
		executeAtSimulationSpeed(simFactor, () -> testTimer(Period.ofDays(1), 2, 15, TimeUnit.SECONDS));
	}
	
	@Test
	public void endTimeWorks() {
		final TestListener listener = new TestListener(getApplicationManager().getFrameworkTime(), 4);
		final long now = System.currentTimeMillis();
		final Instant end = Instant.ofEpochMilli(now + 3500); 
		final PeriodTimerSimple timer = PeriodTimerSimple.builder(getApplicationManager(), Duration.ofSeconds(1))
				.setEndTime(end)
				.addListener(listener)
				.build();
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
	
	private static class TestListener implements TimerListener {
		
		private final long startTime;
		private final CountDownLatch latch;
		
		TestListener(long start, int expectedCallbacks) {
			this.startTime = start;
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
