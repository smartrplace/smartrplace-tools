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
package org.smartrplace.tools.exec.impl;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.exec.ExecutorConstants;

/**
 * A whiteboard service that collects all {@link Runnable} services with properties
 * {@link ExecutorConstants#TASK_DELAY} and {@link ExecutorConstants#TASK_PERIOD}
 * and executes them periodically. The service uses a single-threaded executor; it is 
 * intended for short-lived and seldom-running tasks only. 
 * 
 * @author cnoelle
 *
 */
@Component(
		service=HousekeepingExecService.class, // for the Gogo shell only
		immediate=true,
		configurationPid=ExecutorConstants.HOUSEKEEPING_EXEC_PID,
		configurationPolicy=ConfigurationPolicy.OPTIONAL,
		property={
				"osgi.command.scope=housekeeping",
				"osgi.command.function=getExecTime",
				"osgi.command.function=getExecTimeFraction",
				"osgi.command.function=getIdleTime",
				"osgi.command.function=getTasks",
				"osgi.command.function=isTaskAlive",
				"osgi.command.function=isTaskRunning",
				"osgi.command.function=restartTask",
				"osgi.command.function=runTask",
				"osgi.command.function=stopTask"
		}
)
@Designate(ocd=HousekeepingServiceConfig.class)
public class HousekeepingExecService {

	private final Logger logger = LoggerFactory.getLogger(HousekeepingExecService.class);
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor((task) -> new Thread(task, "housekeeping-thread"));
	private final CompletableFuture<HousekeepingServiceConfig> config = new CompletableFuture<HousekeepingServiceConfig>();
	private final ConcurrentMap<Runnable, CompletableFuture<?>> submissionFutures = new ConcurrentHashMap<>();
	private final ConcurrentMap<TaskWrapper, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
	private volatile long startTimeMillis;

	@Reference(
			target="(&(" + ExecutorConstants.TASK_DELAY + "=*)(" + ExecutorConstants.TASK_PERIOD + "=*))",
			service=Runnable.class,
			cardinality=ReferenceCardinality.MULTIPLE,
			policy=ReferencePolicy.DYNAMIC,
			policyOption=ReferencePolicyOption.GREEDY,
			bind="addTask",
			unbind="removeTask"
	)
	protected void addTask(ComponentServiceObjects<Runnable> taskService) {
		final ServiceReference<?> ref = taskService.getServiceReference();
		final Object delay = ref.getProperty(ExecutorConstants.TASK_DELAY);
		final Object period = ref.getProperty(ExecutorConstants.TASK_PERIOD);
		if (!(delay instanceof Long) || !(period instanceof Long)) {
			logger.error("Task service with invalid properties {}: delay: {}, period: {}", taskService, delay, period);
			return;
		}
		final Object unitObj = ref.getProperty(ExecutorConstants.TASK_PROPERTIES_TIME_UNIT);
		final ChronoUnit unit;
		if (unitObj == null)
			unit = ChronoUnit.MILLIS;
		else {
			try {
				unit = ChronoUnit.valueOf(((String) unitObj).toUpperCase());
			} catch (IllegalArgumentException | ClassCastException e) {
				logger.error("Task with invalid unit property {}: {}", taskService, unitObj);
				return;
			}
		}
		final long delay0 = ((Long) delay).longValue();
		final long period0 = ((Long) period).longValue();
		final long delay1 = unit.getDuration().multipliedBy(delay0).toMillis();
		final long period1 = unit.getDuration().multipliedBy(period0).toMillis();
		final Runnable task = taskService.getService();
		synchronized (this) {
			submissionFutures.put(task, config.thenAcceptAsync(cfg -> {
				synchronized(HousekeepingExecService.this) {
					if (submissionFutures.remove(task) == null) // task has been removed
						return;
					if (period1 <= 0 || period1 <= cfg.minPeriodMs()) {
						logger.warn("Task service period too small: {}. Minimum period set: {}", period1, cfg.minPeriodMs());
						taskService.ungetService(task);
						return;
					}
					final TaskWrapper wrapper = new TaskWrapper(task); 
					submit(wrapper, delay1, period1);
				}
				logger.info("New housekeeping task {} with period {}, initial delay {}", task, period1, delay1);
			}));
		}
	}
	
	private final void submit(final TaskWrapper task, final long delay, final long period) {
		futures.put(task, exec.scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS));
	}
	
	protected void removeTask(ComponentServiceObjects<Runnable> taskService) {
		final Runnable task = taskService.getService();
		final ScheduledFuture<?> future;
		synchronized (this) {
			final CompletableFuture<?> submissionFuture = submissionFutures.remove(task);
			if (submissionFuture != null)
				submissionFuture.cancel(true);
			future = futures.remove(new TaskWrapper(task));
		}
		if (future != null) {
			future.cancel(true);
			logger.info("Removing housekeeping task {}", task);
		}
		taskService.ungetService(task);
		taskService.ungetService(task); // does this work?
	}
	
	@Activate
	protected void activate(HousekeepingServiceConfig config) {
		this.startTimeMillis = System.currentTimeMillis();
		this.config.complete(config);
		logger.debug("Housekeeping executor started with configuration: min period: {} ms, wait time: {} ms", config.minPeriodMs(), config.waitTimeOnShutdownMs());
	}
	
	@Deactivate
	protected void deactivate() {
		exec.shutdown();
		HousekeepingServiceConfig config = null;
		try {
			config = this.config.getNow(null);
		} catch (Exception ignore) {}
		final long waitTime = config == null ? 0 : config.waitTimeOnShutdownMs();
		if (!exec.isTerminated() && waitTime > 0) {
			try {
				exec.awaitTermination(waitTime, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				try {
					Thread.currentThread().interrupt();
				} catch (SecurityException ok) {}
			}
		}
		if (!exec.isTerminated()) {
			final int nr = exec.shutdownNow().size();
			logger.warn("Housekeeping exec service completed with unfinished tasks: {}",nr);
		}
	}
	
	@Descriptor("Get all active tasks. Returns a map task -> hash code.")
	public Map<Runnable, Integer> getTasks() {
		return futures.keySet().stream()
			.map(TaskWrapper::getTask)
			.collect(Collectors.toMap(Function.identity(), System::identityHashCode));
	}
	
	@Descriptor("Stop all tasks with the provided identity hash code. Returns the cancelled tasks.")
	public Collection<Runnable> stopTask(@Descriptor("The hash code of the task to be canceled") int identityHashCode) {
		return futures.entrySet().stream()
			.filter(entry -> System.identityHashCode(entry.getKey().getTask()) == identityHashCode)
			.filter(entry -> entry.getValue().cancel(true))
			.map(Map.Entry::getKey)
			.map(TaskWrapper::getTask)
			.collect(Collectors.toList());
	}
	
	@Descriptor("Check if a task with the provided identity hash code is still active.")
	public Map<Runnable, Boolean> isTaskAlive (
			@Descriptor("The hash code of the task to be checked. If absent, all tasks will be checked.")
			@Parameter(names= {"-h", "--hashcode"}, absentValue="-1")
			int identityHashCode) {
		Stream<Map.Entry<TaskWrapper, ScheduledFuture<?>>> stream = futures.entrySet().stream();
		if (identityHashCode != -1)
			stream = stream
				.filter(entry -> System.identityHashCode(entry.getKey()) == identityHashCode);
		return stream
				.collect(Collectors.toMap(entry -> entry.getKey().getTask(), entry -> !entry.getValue().isCancelled()));
	}
	
	@Descriptor("Check if a task with the provided identity hash code is still running.")
	public void runTask (
			@Descriptor("The hash code of the task to be run. If absent, the first task found will be run.")
			@Parameter(names= {"-h", "--hashcode"}, absentValue="-1")
			int identityHashCode) {
		Stream<TaskWrapper> stream = futures.keySet().stream()
				.filter(task -> !task.isRunning());
		if (identityHashCode != -1)
			stream = stream
				.filter(task -> System.identityHashCode(task) == identityHashCode);
		stream
			.findAny()
			.ifPresent(exec::submit);
	}
	
	@Descriptor("Check if a task with the provided identity hash code is currently running.")
	public Map<Runnable, Boolean> isTaskRunning (
			@Descriptor("The hash code of the task to be checked. If absent, all tasks will be checked.")
			@Parameter(names= {"-h", "--hashcode"}, absentValue="-1")
			int identityHashCode) {
		Stream<TaskWrapper> stream = futures.keySet().stream();
		if (identityHashCode != -1)
			stream = stream
				.filter(task -> System.identityHashCode(task) == identityHashCode);
		return stream
				.collect(Collectors.toMap(entry -> entry.getTask(), entry -> entry.isRunning()));
	}
	
	@Descriptor("Restart all tasks (with the provided identity hash code, if any) which have been cancelled. Returns the "
			+ "tasks that have been restarted.")
	public Collection<Runnable> restartTask(
			@Descriptor("The time unit for delay and period, such as MILLISECONDS, MINUTES or HOURS. Default is MINUTES.")
			@Parameter(names= {"-u", "--unit"}, absentValue="MINUTES")
			final String timeUnit,
			@Descriptor("The hash code of the task to be checked. If absent or equal to -1, all tasks will be checked.")
			@Parameter(names= {"-h", "--hashcode"}, absentValue="-1")
			final int identityHashCode,
			@Descriptor("The initial delay before the first task execution") long delay,
			@Descriptor("The period between two task executions") long period
		) throws InterruptedException, ExecutionException, TimeoutException {
		if (period <= 0) {
			System.out.println("Period must be positive, got "+ period);
			return Collections.emptyList();
		} 
		final TimeUnit unit;
		try {
			unit = TimeUnit.valueOf(timeUnit.toUpperCase());
		} catch (IllegalArgumentException e) {
			System.out.println("No such time unit: " + timeUnit);
			return Collections.emptyList();
		}
		final long period1 = TimeUnit.MILLISECONDS.convert(period, unit);
		return config.<Collection<Runnable>> thenApplyAsync(cfg -> {
			if (period1 < cfg.minPeriodMs()) {
				System.out.println("Period is below the configured threshold of " + cfg.minPeriodMs() + " ms.");
				return Collections.emptyList();
			}
			Stream<Map.Entry<TaskWrapper, ScheduledFuture<?>>> stream = futures.entrySet().stream();
			if (identityHashCode != -1)
				stream = stream
					.filter(entry -> System.identityHashCode(entry.getKey().getTask()) == identityHashCode);
			final Collection<TaskWrapper> tasks = stream
				.filter(entry -> entry.getValue().isCancelled())
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
			tasks.forEach(task -> submit(task, delay, period1));
			return tasks.stream()
					.map(TaskWrapper::getTask)
					.collect(Collectors.toList());
		}).get(30, TimeUnit.SECONDS);
	}
	
	@Descriptor("Get total run time for all tasks or a specific task")
	public long getExecTime(
			@Descriptor("The time unit. Default is \"SECONDS\".")
			@Parameter(names= {"-u", "--unit"}, absentValue="SECONDS")
			final String timeUnit,
			@Descriptor("The hash code of the task to be measured. If absent or equal to -1, all tasks will be included.")
			@Parameter(names= {"-h", "--hashcode"}, absentValue="-1")
			final int identityHashCode
			) {
		final TimeUnit unit = TimeUnit.valueOf(timeUnit.trim().toUpperCase());
		Stream<TaskWrapper> stream = futures.keySet().stream();
		if (identityHashCode != -1)
			stream = stream.filter(task -> System.identityHashCode(task.getTask()) == identityHashCode);
		return stream
			.mapToLong(TaskWrapper::getExecutionTimeMillis)
			.map(millis -> unit.convert(millis, TimeUnit.MILLISECONDS))
			.sum();
	}
	
	@Descriptor("Get the run time fraction for all tasks or a specific task")
	public float getExecTimeFraction(
			@Descriptor("The hash code of the task to be measured. If absent or equal to -1, all tasks will be included.")
			@Parameter(names= {"-h", "--hashcode"}, absentValue="-1")
			final int identityHashCode
			) {
		final long activeTime = getExecTime(TimeUnit.MILLISECONDS.toString(), identityHashCode);
		final long totalTime = System.currentTimeMillis() - startTimeMillis;
		return ((float) activeTime) / ((float) totalTime);
	}
	
	@Descriptor("Get the idle time of the service")
	public long getIdleTime(
			@Descriptor("The time unit. Default is \"SECONDS\".")
			@Parameter(names= {"-u", "--unit"}, absentValue="SECONDS")
			final String timeUnit
			) {
		final TimeUnit unit = TimeUnit.valueOf(timeUnit.trim().toUpperCase());
		final long activeTime = getExecTime(TimeUnit.MILLISECONDS.toString(), -1);
		final long totalTime = System.currentTimeMillis() - startTimeMillis;
		return unit.convert(totalTime - activeTime, TimeUnit.MILLISECONDS);
	}
	
	
	
}
