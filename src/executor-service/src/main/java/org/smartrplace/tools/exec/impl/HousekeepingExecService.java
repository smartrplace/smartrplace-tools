package org.smartrplace.tools.exec.impl;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
import org.osgi.service.component.annotations.Modified;
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
				"osgi.command.function=getTasks",
				"osgi.command.function=isTaskAlive",
				"osgi.command.function=restartTask",
				"osgi.command.function=stopTask"
		}
)
@Designate(ocd=HousekeepingServiceConfig.class)
public class HousekeepingExecService {

	private final Logger logger = LoggerFactory.getLogger(HousekeepingExecService.class);
	private final CompletableFuture<ScheduledExecutorService> exec = new CompletableFuture<ScheduledExecutorService>();
	private final ConcurrentMap<Runnable, CompletableFuture<?>> submissionFutures = new ConcurrentHashMap<>();
	private final ConcurrentMap<Runnable, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
	private volatile HousekeepingServiceConfig config;

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
		
		if (period1 <= 0 || period1 <= config.minPeriodMs()) {
			logger.warn("Task service period too small: {}. Minimum period set: {}", period1, config.minPeriodMs());
			return;
		}
		final Runnable task = taskService.getService();
		logger.info("New housekeeping task {} with period {}, initial delay {}", task, period1, delay1);
		submit(task, delay1, period1);
	}
	
	private final void submit(final Runnable task, final long delay, final long period) {
		submissionFutures.put(task, exec.thenAcceptAsync(ex -> {
			futures.put(task, ex.scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS));
			submissionFutures.remove(task);
		}));
	}
	
	protected void removeTask(ComponentServiceObjects<Runnable> taskService) {
		final Runnable task = taskService.getService();
		final CompletableFuture<?> submissionFuture = submissionFutures.remove(task);
		if (submissionFuture != null)
			submissionFuture.cancel(true);
		final ScheduledFuture<?> future = futures.remove(task);
		if (future != null) {
			future.cancel(true);
			logger.info("Removing housekeeping task {}", task);
		}
		taskService.ungetService(task);
		taskService.ungetService(task); // does this work?
	}
	
	@Activate
	protected void activate(HousekeepingServiceConfig config) {
		this.config = config;
		this.exec.complete(Executors.newSingleThreadScheduledExecutor((task) -> new Thread(task, "housekeeping-thread")));
		logger.debug("Housekeeping executor started with configuration: min period: {} ms, wait time: {} ms", config.minPeriodMs(), config.waitTimeOnShutdownMs());
	}
	
	@Modified
	protected void modified(HousekeepingServiceConfig config) {
		logger.debug("Housekeeping executor received new configuration. Min period: {} ms, wait time: {} ms", config.minPeriodMs(), config.waitTimeOnShutdownMs());
		this.config = config;
	}
	
	@Deactivate
	protected void deactivate() {
		this.exec.cancel(false);
		ScheduledExecutorService exec = null;
		try {
			exec = this.exec.getNow(null);
		} catch (CancellationException | CompletionException expected) {}
		if (exec != null) {
			exec.shutdown();
			final long waitTime = config.waitTimeOnShutdownMs();
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
	}
	
	@Descriptor("Get all active tasks. Returns a map task -> hash code.")
	public Map<Runnable, Integer> getTasks() {
		return futures.keySet().stream()
			.collect(Collectors.toMap(Function.identity(), System::identityHashCode));
	}
	
	@Descriptor("Stop all tasks with the provided identity hash code. Returns the cancelled tasks.")
	public Collection<Runnable> stopTask(@Descriptor("The hash code of the task to be canceled") int identityHashCode) {
		return futures.entrySet().stream()
			.filter(entry -> System.identityHashCode(entry.getKey()) == identityHashCode)
			.filter(entry -> entry.getValue().cancel(true))
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
	}
	
	@Descriptor("Check if a task with the provided identity hash code is still running.")
	public Map<Runnable, Boolean> isTaskAlive (
			@Descriptor("The hash code of the task to be checked. If absent, all tasks will be checked.")
			@Parameter(names= {"-h", "--hashcode"}, absentValue="-1")
			int identityHashCode) {
		Stream<Map.Entry<Runnable, ScheduledFuture<?>>> stream = futures.entrySet().stream();
		if (identityHashCode != -1)
			stream = stream
				.filter(entry -> System.identityHashCode(entry.getKey()) == identityHashCode);
		return stream
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> !entry.getValue().isCancelled()));
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
		) {
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
		if (period1 < config.minPeriodMs()) {
			System.out.println("Period is below the configured threshold of " + config.minPeriodMs() + " ms.");
			return Collections.emptyList();
		}
		Stream<Map.Entry<Runnable, ScheduledFuture<?>>> stream = futures.entrySet().stream();
		if (identityHashCode != -1)
			stream = stream
				.filter(entry -> System.identityHashCode(entry.getKey()) == identityHashCode);
		final Collection<Runnable> tasks = stream
			.filter(entry -> entry.getValue().isCancelled())
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		tasks.forEach(task -> submit(task, delay, period1));
		return tasks;
	}
	
}
