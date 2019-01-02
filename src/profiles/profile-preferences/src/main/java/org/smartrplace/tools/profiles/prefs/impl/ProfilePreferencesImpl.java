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
package org.smartrplace.tools.profiles.prefs.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.tools.resource.util.ResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;
import org.smartrplace.tools.profiles.prefs.ProfileData;
import org.smartrplace.tools.profiles.prefs.ProfilePreferences;
import org.smartrplace.tools.profiles.prefs.StateDuration;
import org.smartrplace.tools.profiles.prefs.model.DataPointConfig;
import org.smartrplace.tools.profiles.prefs.model.ProfileConfiguration;

@Component(service=ProfilePreferences.class)
public class ProfilePreferencesImpl implements ProfilePreferences, Application {

	private static final String BASE_RESOURCE = "smartrplaceProfilePreferences";
	private final CompletableFuture<ApplicationManager> appManFuture = new CompletableFuture<>();
	private ServiceRegistration<Application> appReg;
	
	@Activate
	protected void activate(BundleContext ctx) {
		this.appReg = ctx.registerService(Application.class, this, null);
	}
	
	@Deactivate
	protected void deactivate() {
		ForkJoinPool.commonPool().submit(() -> {
			try {
				appReg.unregister();
			} catch (Exception ignore) {}
		});
	}
	
	@Override
	public void start(ApplicationManager appManager) {
		this.appManFuture.complete(appManager);
	}
	
	@Override public void stop(AppStopReason reason) {}
	
	@Override
	public Future<?> storeProfileConfiguration(ProfileTemplate template, String id, 
				Map<DataPoint, Resource> resourceSettings, List<StateDuration> durations, State endState, OnOffSwitch onOffSwitch) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(template);
		Objects.requireNonNull(resourceSettings);
		if (durations != null) {
			if (durations.size() != template.states().size())
				throw new IllegalArgumentException("Size of durations list does not match template states size");
			final Iterator<StateDuration> durIt = durations.iterator();
			final Iterator<State> statesIt = template.states().iterator();
			while (durIt.hasNext()) {
				final StateDuration dur = durIt.next();
				final State state = statesIt.next();
				if (dur.getState() != state)
					throw new IllegalArgumentException("Invalid state order in durations list");
			}
		}
		return appManFuture.thenAcceptAsync(appMan -> {
			@SuppressWarnings("unchecked")
			final ResourceList<ProfileConfiguration> list = 
					appMan.getResourceManagement().createResource(BASE_RESOURCE, ResourceList.class);
			list.setElementType(ProfileConfiguration.class);
			list.activate(false);
			final ResourceTransaction trans = appMan.getResourceAccess().createResourceTransaction();
			// FIXME name not unqiue!
			final ProfileConfiguration cfg = list.getSubResource(ResourceUtils.getValidResourceName(id), ProfileConfiguration.class);
			trans.delete(cfg); // if config existed already
			trans.create(cfg);
			trans.setString(cfg.id(), id);
			trans.setString(cfg.profileTemplateId(), template.id());
			final ResourceList<DataPointConfig> configs = cfg.dataPoints();
			trans.create(configs);
			resourceSettings.forEach((dataPoint, resource) -> {
				final DataPointConfig dp = configs.getSubResource(ResourceUtils.getValidResourceName(dataPoint.id()), DataPointConfig.class);
				trans.create(dp);
				trans.setString(dp.dataPointId(), dataPoint.id());
				trans.setAsReference(dp.target(), resource);
			});
			if (onOffSwitch != null)
				trans.setAsReference(cfg.onOffSwitch(), onOffSwitch);
			if (endState != null)
				trans.setString(cfg.endState(), endState.id());
			if (durations != null)
				StateDuration.serialize(durations, cfg.durations(), trans);
			trans.activate(cfg, false, true);
			trans.commit();
		});
	}
	
	@Override
	public Future<ProfileData> loadProfileConfiguration(ProfileTemplate template, String id) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(template);
		return appManFuture.thenApplyAsync(appMan -> {
			final Optional<ProfileConfiguration> opt = appMan.getResourceAccess().getResources(ProfileConfiguration.class).stream()
				.filter(cfg -> id.equals(cfg.id().getValue()))
				.filter(cfg -> template.id().equals(cfg.profileTemplateId().getValue()))
				.findAny();
			if (!opt.isPresent())
				return null;
			final ProfileConfiguration cfg = opt.get();
			final Stream<DataPoint> dps = Stream.concat(template.primaryData().stream(), template.contextData().stream());
			final List<String> dpIds = dps.map(DataPoint::id).collect(Collectors.toList());
			final Map<DataPoint, Resource> map = cfg.dataPoints().getAllElements().stream()
				.filter(dpc -> dpIds.contains(dpc.dataPointId().getValue()))
				.filter(dpc -> dpc.target().isReference(false))
				.collect(Collectors.toMap(dpc -> getDataPoint(template, dpc), DataPointConfig::target));
			final OnOffSwitch oo = cfg.onOffSwitch().isActive() ? cfg.onOffSwitch() : null;
			final String endStateId = cfg.endState().isActive() ? cfg.endState().getValue() : null;
			final State endState = endStateId == null ? null : template.states().stream()
				.filter(st -> endStateId.equals(st.id()))
				.findAny().orElse(null);
			final List<org.smartrplace.tools.profiles.prefs.model.StateDuration> durs = cfg.durations().getAllElements();
			final List<StateDuration> durObjects;
			if (durs.size() == template.states().size()) {
				final List<StateDuration> list0 = durs.stream()
					.map(d -> StateDuration.deserialize(d, template))
					.collect(Collectors.toList());
				if (list0.stream().filter(Objects::isNull).findAny().isPresent())
					durObjects = null;
				else
					durObjects = list0;
			} else {
				durObjects = null;
			}
			return new ProfileData(map, oo, endState, durObjects);
		});
	}
	
	@Override
	public Future<Collection<String>> getProfileIds(String templateId) {
		Objects.requireNonNull(templateId);
		return appManFuture.thenApplyAsync(appMan -> {
			 return appMan.getResourceAccess().getResources(ProfileConfiguration.class).stream()
				.filter(cfg -> templateId.equals(cfg.profileTemplateId().getValue()))
				.map(ProfileConfiguration::id)
				.map(StringResource::getValue)
				.filter(value -> value != null && !value.isEmpty())
				.collect(Collectors.toList());
		});
	}
	
	private static DataPoint getDataPoint(final ProfileTemplate template, final DataPointConfig dpc) {
		final String id = dpc.dataPointId().getValue();
		final Stream<DataPoint> dps = Stream.concat(template.primaryData().stream(), template.contextData().stream());
		return dps.filter(dp -> id.equals(dp.id())).findAny().orElse(null);
	}
	
	
}
