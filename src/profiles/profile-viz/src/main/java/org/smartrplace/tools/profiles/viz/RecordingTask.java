package org.smartrplace.tools.profiles.viz;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.osgi.service.component.ComponentServiceObjects;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.DataPoint.DataType;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

import org.smartrplace.tools.profiles.Profile;
import org.smartrplace.tools.profiles.ProfileGeneration;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;
import org.smartrplace.tools.profiles.utils.StandardDataPoints;

class RecordingTask implements Callable<Profile> {

	private final ComponentServiceObjects<ProfileGeneration> generator;
	private final ComponentServiceObjects<ProfileTemplate> service; 
	private final ProfileTemplate template;
	private final Consumer<State> switchFunction;
	private final Map<DataPoint, Resource> input;
	private final List<Long> durations;
	private final State endState;
	
	public RecordingTask(ComponentServiceObjects<ProfileGeneration> generator, ComponentServiceObjects<ProfileTemplate> service, 
				Consumer<State> switchFunction, Map<DataPoint, Resource> input, List<Long> durations, State endState) {
		this.generator = Objects.requireNonNull(generator);
		this.service = Objects.requireNonNull(service);
		this.switchFunction = Objects.requireNonNull(switchFunction);
		this.input = Objects.requireNonNull(input);
		this.durations = Objects.requireNonNull(durations);
		this.template = service.getService();
		this.endState = endState; // may be null
		if (durations.size() != template.states().size() || durations.stream().anyMatch(Objects::isNull)) {
			final List<State> states = template.states();
			service.ungetService(template);
			throw new IllegalArgumentException("Duration array size does not match states array size. Durations: " + durations
					+ ", states: " + states);
		}
		Optional<DataPoint> missing = template.primaryData().stream()
				.filter(dp -> !dp.optional())
				.filter(dp -> !input.containsKey(dp))
				.findAny();
			if (!missing.isPresent()) {
				missing = template.contextData().stream()
					.filter(dp -> !dp.optional())
					.filter(dp -> !input.containsKey(dp))
					.filter(dp -> !StandardDataPoints.isStartTime(dp))
					.findAny();
			}
			if (missing.isPresent())
				throw new IllegalArgumentException("Missing data for " + missing.get().label(OgemaLocale.ENGLISH));
	}

	private final Profile callInternal(final ProfileGeneration generator, final ProfileTemplate template) throws InterruptedException, IOException {
		final Profile profile = generator.run(template, switchFunction, getStateEndTimes(template), convertToInputData(input), endState);
		generator.storeProfile(profile);
		return profile;
	}
	
	private static Map<DataPoint, Object> convertToInputData(final Map<DataPoint, Resource> input) {
		return input.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> {
				final Resource value = entry.getValue();
				final DataPoint.DataType type = entry.getKey().dataType();
				if (type == DataType.SINGLE_VALUE && value instanceof SingleValueResource)
					return ValueResourceUtils.getValue((ValueResource) value);
				else if (type == DataType.STRING) {
					if (value instanceof SingleValueResource)
						return ValueResourceUtils.getValue((SingleValueResource) value);
					else
						return ResourceUtils.getHumanReadableName(value);
				}
				else if (type == DataType.TIME_SERIES)
					return value;
				else 
					throw new IllegalArgumentException("Cannot determine input data type for " + entry.getKey() + ": " + entry.getValue());
			}));
	}
	
	private final NavigableMap<Long, State> getStateEndTimes(final ProfileTemplate template) {
		final List<State> states = template.states();
		final NavigableMap<Long, State> endTimes = new TreeMap<>();
		long lastEndTime = 0;
		final Iterator<Long> durIt = durations.iterator();
		for (State state: states) {
			final long d = durIt.next();
			lastEndTime += d;
			endTimes.put(lastEndTime, state);
		}
		return endTimes;
	}
	
	@Override
	public Profile call() throws InterruptedException, IOException {
		try {
			final ProfileGeneration generator = this.generator.getService();
			try {
				return callInternal(generator, template);
			} catch (Throwable e) {
				LoggerFactory.getLogger(getClass()).error("Profile generation failed.",e);
				throw e;
			} finally {
				this.generator.ungetService(generator);
			}
		} finally {
			service.ungetService(template); // we accessed the service in the constructor
		}
	}
	
}
