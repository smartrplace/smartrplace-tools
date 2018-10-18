package org.smartrplace.tools.profiles.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.administration.FrameworkClock;
import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.LongValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.ogema.tools.timeseries.api.MemoryTimeSeries;
import org.ogema.tools.timeseries.implementations.TreeTimeSeries;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.Profile;
import org.smartrplace.tools.profiles.ProfileGeneration;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;
import org.smartrplace.tools.profiles.DataPoint.DataType;
import org.smartrplace.tools.profiles.utils.ProfileImpl;
import org.smartrplace.tools.profiles.utils.StandardDataPoints;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

@Component(
		service=ProfileGeneration.class,
		configurationPid=ProfileGeneratorConfiguration.PID,
		configurationPolicy=ConfigurationPolicy.OPTIONAL
)
@Designate(ocd=ProfileGeneratorConfiguration.class)
public class ProfileGenerationImpl implements ProfileGeneration {
	
	private static final String SEPARATOR = "_X_";
	private final ConcurrentMap<String, ComponentServiceObjects<ProfileTemplate>> templates = new ConcurrentHashMap<>(8);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private Path profilesFolder;
	
	@Activate
	protected void activate(BundleContext ctx, ProfileGeneratorConfiguration config) throws IOException {
		final String configPath = config == null ? "" : config.storagePath();
		final Path p;
		try {
			p = !configPath.isEmpty() ? Paths.get(configPath) : ctx.getDataFile("profiles").toPath();
		} catch (InvalidPathException e) {
			throw new ComponentException("Invalid storage path configured: " + configPath);
		}
		Files.createDirectories(p);
		this.profilesFolder = p;
		LoggerFactory.getLogger(getClass()).info("ProfileGenerationImpl starting; storing files in directory {}", p);
	}
	
	@Reference(
			service=FrameworkClock.class,
			cardinality=ReferenceCardinality.OPTIONAL,
			policy=ReferencePolicy.DYNAMIC,
			policyOption=ReferencePolicyOption.GREEDY
	)
	private volatile ComponentServiceObjects<FrameworkClock> clock;
	
	@Reference(
			service=ProfileTemplate.class,
			cardinality=ReferenceCardinality.MULTIPLE,
			policy=ReferencePolicy.DYNAMIC,
			policyOption=ReferencePolicyOption.GREEDY,
			bind="addTemplate",
			unbind="removeTemplate"
	)
	protected void addTemplate(final ComponentServiceObjects<ProfileTemplate> templateRef) {
		final String id = getId(templateRef);
		if (id == null)
			throw new ComponentException("Profile template without id: " + templateRef);
		final ComponentServiceObjects<ProfileTemplate> old = templates.put(id, templateRef);
		if (old != null) {
			final ProfileTemplate oldTemplate = old.getService();
			final ProfileTemplate newTemplate = templateRef.getService();
			try {
				LoggerFactory.getLogger(getClass()).warn("Duplicate profile template id {}: {}, {}", id, oldTemplate, newTemplate);
			} finally {
				templateRef.ungetService(newTemplate);
				old.ungetService(oldTemplate);
			}
		}
	}
	
	protected void removeTemplate(final ComponentServiceObjects<ProfileTemplate> templateRef) {
		final String id = getId(templateRef);
		if (id == null)
			return;
		templates.remove(id, templateRef);
	}
	
	private static String getId(final ComponentServiceObjects<ProfileTemplate> service) {
		Object id0 = service.getServiceReference().getProperty(ProfileTemplate.ID_PROPERTY);
		String id = id0 != null ? id0.toString() : null;
		if (id == null) {
			final ProfileTemplate template = service.getService();
			try {
				id = template.id();
			} finally {
				service.ungetService(template);
			}
		} 
		return id;
	}

	@Override
	public Profile run(ProfileTemplate template, Consumer<State> switchFunction,
			NavigableMap<Long, State> stateDurations, Map<DataPoint, Object> data0, State endState) throws InterruptedException {
		if (stateDurations.firstKey() <= 0)
			throw new IllegalArgumentException("State end times contain a non-positive value: "+ stateDurations.firstKey());
		Objects.requireNonNull(switchFunction, "Switch function is null");
		if (data0.entrySet().stream()
			.filter(entry -> entry.getValue() == null || entry.getKey() == null)
			.findAny()
			.isPresent())
			throw new NullPointerException("Null data passed");
		final Optional<DataPoint> startTime = template.contextData().stream()
			.filter(StandardDataPoints::isStartTime)
			.findAny();
		final boolean hasStartTime = startTime.isPresent();
		final Map<DataPoint, Object> data;
		if (hasStartTime && !data0.keySet().stream().filter(StandardDataPoints::isStartTime).findAny().isPresent()) {
			data = new LinkedHashMap<>(data0);
			data.put(startTime.get(), time());
		} else {
			data = data0;
		}
		if (stateDurations.entrySet().stream()
				.filter(entry -> entry.getValue() == null || entry.getKey() == null)
				.findAny()
				.isPresent())
				throw new NullPointerException("Null data passed");
		Optional<DataPoint> missing = template.primaryData().stream()
			.filter(dp -> !dp.optional())
			.filter(dp -> !data0.containsKey(dp))
			.findAny();
		if (!missing.isPresent()) {
			missing = template.contextData().stream()
				.filter(dp -> !dp.optional())
				.filter(dp -> !data.containsKey(dp))
				.findAny();
		}
		if (missing.isPresent())
			throw new IllegalArgumentException("Missing data for " + missing.get().label(OgemaLocale.ENGLISH));
		final String profileId = getProfileId(template.id());
		logger.info("Starting profile recording for template {}, id {}", template.id(), profileId);
		final long start = time();
		final float factor = factor();
		final Map<DataPoint, Object> copy = new LinkedHashMap<>(data);
		logger.debug("Supported data points {}", copy.keySet());
		final Map<DataPoint, ValueListener> listeners = new HashMap<>(copy.size());
		long lastStart = 0;
		try {
			for (Map.Entry<DataPoint, Object> entry : copy.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof Sensor)
					value = ((Sensor) value).reading();
				if (value instanceof SingleValueResource && entry.getKey().dataType() == DataType.TIME_SERIES) {
					final ValueListener listener = new ValueListener(start, (SingleValueResource) value);
					((SingleValueResource) value).addValueListener(listener, true);
					listeners.put(entry.getKey(), listener);
				}
			}
			for (Map.Entry<Long, State> entry : stateDurations.entrySet()) {
				switchFunction.accept(entry.getValue());
				logger.debug("State switched to {}", entry.getValue());
				Thread.sleep(Math.max(1, (long) ((entry.getKey() - lastStart)/factor)));
				lastStart = entry.getKey();
			}
			final long end = time() - start;
			for (Map.Entry<DataPoint, Object> entry : copy.entrySet()) {
				if (entry.getKey().dataType() != DataType.TIME_SERIES)
					continue;
				Object value = entry.getValue();
				if (value instanceof Sensor)
					value = ((Sensor) value).reading();
				if (value instanceof SingleValueResource)
					listeners.get(entry.getKey()).resourceChanged(end, (SingleValueResource) value);
			}
			final Map<DataPoint, Object> resultsPrimary = new LinkedHashMap<>();
			final Map<DataPoint, Object> resultsContext = new LinkedHashMap<>();
			for (Map.Entry<DataPoint, Object> entry : copy.entrySet()) {
				final DataPoint dp = entry.getKey();
				final Object result;
				switch (dp.dataType()) {
				case TIME_SERIES:
					final ValueListener listener = listeners.get(dp);
					if (listener == null)
						throw new IllegalStateException("Listener not found for data point " + dp);
					final List<SampledValue> values = listener.getValues();
					if (values.isEmpty())
						result = MemoryTimeSeries.EMPTY_TIME_SERIES;
					else {
						result = new TreeTimeSeries(values.iterator().next().getValue().getClass());
						((TreeTimeSeries) result).addValues(values);
					}
					break;
				case STRING:
					final Object value = entry.getValue();
					result = value instanceof PhysicalElement ? ResourceUtils.getHumanReadableName((PhysicalElement) value) :  
							value instanceof StringResource ? ((StringResource) value).getValue() : value.toString();
					break;
				case SINGLE_VALUE:
					Object value3 = entry.getValue();
					if (value3 instanceof Sensor)
						value3 = ((Sensor) value3).reading();
					result = value3 instanceof Number ? (Number) value3 : value3 instanceof SingleValueResource ?
							(Number) ValueResourceUtils.getValue((ValueResource) value3) : null;
					if (result == null)
						throw new IllegalArgumentException("Cannot convert " + value3 + " to a number");
					break;
				default:
					throw new IllegalStateException();
				}
				if (template.primaryData().contains(dp))
					resultsPrimary.put(dp, result);
				else
					resultsContext.put(dp, result);
			}
			if (endState != null && switchFunction != null)
				switchFunction.accept(endState);
			logger.info("Profile generation completed: {}, primary result keys: {}", profileId, resultsPrimary.keySet());
			return new ProfileImpl(profileId, resultsPrimary, resultsContext, stateDurations, template);
		} finally {
			listeners.forEach((dp, listener) -> {
				try {
					((SingleValueResource) copy.get(dp)).removeValueListener(listener);
				} catch (Exception ignore) {}
			});
		}
	}
	
	private final String getProfileId(final String templateId) {
		return templateId + SEPARATOR + time();
	}
	
	private final String getTemplateId(final String profileId) {
		final int sepIdx = profileId.lastIndexOf(SEPARATOR);
		if (sepIdx < 0)
			return null;
		return profileId.substring(0, sepIdx);
	}
	
	private static String getProfileIdFromFilename(final String filename) {
		final int idx = filename.lastIndexOf('.');
		if (idx < 0)
			return null;
		return filename.substring(0, idx);
	}
	
	@Override
	public void store(Profile profile, OutputStream out) throws IOException {
		if (!(profile instanceof ProfileImpl)) // TODO
			throw new UnsupportedOperationException("Serialization not implemented for class " + profile.getClass());
		SerializationUtils.write(out, (ProfileImpl) profile);
	}
	
	@Override
	public void store(Profile profile, Writer out) throws IOException {
		if (!(profile instanceof ProfileImpl)) // TODO
			throw new UnsupportedOperationException("Serialization not implemented for class " + profile.getClass());
		SerializationUtils.write(out, (ProfileImpl) profile);
	}
	
	@Override
	public Profile read(InputStream in) throws IOException {
		return SerializationUtils.read(in, templates);
	}
	
	@Override
	public Profile read(Reader in) throws IOException {
		return SerializationUtils.read(in, templates);
	}
	
	@Override
	public void storeProfile(Profile profile) throws IOException {
		final Path file = profilesFolder.resolve(profile.id() + ".json");
		try (final Writer writer 
				= Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			store(profile, writer);
		}
		logger.info("Profile stored: {}", file);;
	}
	
	@Override
	public Collection<String> getStoredProfileIds(String templateId) throws IOException {
		synchronized (this) {
			try (final Stream<Path> files = Files.list(profilesFolder)) { // must be closed
				Stream<String> stream = files
						.map(p -> getProfileIdFromFilename(p.getFileName().toString()))
						.filter(Objects::nonNull);
				if (templateId != null) 
					stream = stream.filter(p -> templateId.equals(getTemplateId(p)));
				return stream.collect(Collectors.toList());
			} 
		}
	}
	
	@Override
	public Profile getStoredProfile(String profileId) throws IOException {
		Objects.requireNonNull(profileId);
		final Path file = profilesFolder.resolve(profileId + ".json");
		if (!Files.isRegularFile(file))
			return null;
		try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			return read(reader);
		}
	}
	
	@Override
	public boolean removeStoredProfile(String profileId) throws IOException {
		Objects.requireNonNull(profileId);
		final Path file = profilesFolder.resolve(profileId + ".json");
		if (!Files.isRegularFile(file))
			return false;
		Files.delete(file);
		return true;
	}
	
	private class ValueListener implements ResourceValueListener<SingleValueResource> {

		private final long startTime;
		private final List<SampledValue> values = new ArrayList<>();
		
		public ValueListener(final long startTime, final SingleValueResource resource) {
			this.startTime = startTime;
			resourceChanged(0, resource);
		}
		
		@Override
		public void resourceChanged(SingleValueResource resource) {
			final long t = time() - startTime;
			resourceChanged(t, resource);
		}
		
		final void resourceChanged(final long t, final SingleValueResource resource) {
			final Value v;
			if (resource instanceof FloatResource)
				v = new FloatValue(((FloatResource) resource).getValue());
			else if (resource instanceof IntegerResource)
				v = new IntegerValue(((IntegerResource) resource).getValue());
			else if (resource instanceof BooleanResource)
				v = new BooleanValue(((BooleanResource) resource).getValue());
			else if (resource instanceof TimeResource)
				v = new LongValue(((TimeResource) resource).getValue());
			else
				throw new IllegalArgumentException();
			values.add(new SampledValue(v, t, Quality.GOOD));
		}
		
		public List<SampledValue> getValues() {
			return values;
		}
		
	}
	
	private final long time() {
		final ComponentServiceObjects<FrameworkClock> service = this.clock;
		if (service == null)
			return System.currentTimeMillis();
		FrameworkClock clock = null;
		try {
			clock = service.getService();
			return clock.getExecutionTime();
		} catch (IllegalArgumentException e) {
			return System.currentTimeMillis();
		} finally {
			if (clock != null)
				service.ungetService(clock);
		}
	}
	
	private final float factor() {
		final ComponentServiceObjects<FrameworkClock> service = this.clock;
		if (service == null)
			return 1;
		FrameworkClock clock = null;
		try {
			clock = service.getService();
			return clock.getSimulationFactor();
		} catch (IllegalArgumentException e) {
			return 1;
		} finally {
			if (clock != null)
				service.ungetService(clock);
		}
	}

}
