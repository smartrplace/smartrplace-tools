/**
 * ﻿Copyright 2019 Smartrplace UG
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
package org.smartrplace.apps.humidity.warning.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.ogema.core.application.AppID;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.CompoundResourceEvent;
import org.ogema.core.resourcemanager.pattern.PatternChangeListener;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.model.locations.Room;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.smartrplace.apps.humidity.warning.impl.gui.HumidityPage;
import org.smartrplace.apps.humidity.warning.impl.pattern.HumidityPattern;
import org.smartrplace.apps.humidity.warning.impl.pattern.WarningConfigurationPattern;
import org.smartrplace.apps.humidity.warning.model.WarningConfiguration;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.services.MessagingService;
import de.iwes.widgets.api.services.NameService;
import de.iwes.widgets.api.widgets.WidgetApp;

@Component(service=Application.class)
public class HumidityWarningApp implements Application, PatternListener<WarningConfigurationPattern>, PatternChangeListener<WarningConfigurationPattern> {
	
	private final static String BASE_RESOURCE = "humidityWarningAppBase";
	private ApplicationManager appMan;
	private Logger logger;
	private NameService nameService;
	private MessagingService messagingService;
	private final Map<Room, HumidityController> configs = new HashMap<>();
	private HumidityListener humidityListener;
	private WidgetApp wapp;
	private ResourceList<WarningConfiguration> configsBase;
	
	@Reference
	private OgemaGuiService widgetService;

	@SuppressWarnings("unchecked")
	@Override
	public void start(ApplicationManager appManager) {
		this.appMan = appManager;
		this.logger = appManager.getLogger();
		this.nameService = widgetService.getNameService();
		this.messagingService = widgetService.getMessagingService();
		messagingService.registerMessagingApp(appManager.getAppID(), "Humidity Warning App");
		humidityListener = new HumidityListener(configs, appManager.getResourcePatternAccess());
		appManager.getResourcePatternAccess().addPatternDemand(WarningConfigurationPattern.class, this, AccessPriority.PRIO_LOWEST);
		appManager.getResourcePatternAccess().addPatternDemand(HumidityPattern.class, humidityListener, AccessPriority.PRIO_LOWEST);
		this.configsBase = appManager.getResourceManagement().createResource(BASE_RESOURCE, ResourceList.class);
		configsBase.setElementType(WarningConfiguration.class);
		wapp = widgetService.createWidgetApp("/org/smartrplace/apps/humidity-warning", appManager);
		wapp.createLazyStartPage(page -> new HumidityPage(page, appManager.getResourcePatternAccess(), configsBase));
	}

	@Override
	public void stop(AppStopReason reason) {
		final ApplicationManager appMan = this.appMan;
		final MessagingService messagingService = this.messagingService;
		final AppID appId = appMan != null ? appMan.getAppID() : null;
		final WidgetApp wapp = this.wapp;
		if (appMan != null) {
			try {
				appMan.getResourcePatternAccess().removePatternDemand(WarningConfigurationPattern.class, this);
				appMan.getResourcePatternAccess().removePatternDemand(HumidityPattern.class, humidityListener);
			} catch (Exception ignore) {}
		}
		if (messagingService != null && appId != null) {
			try {
				messagingService.unregisterMessagingApp(appId);
			} catch (Exception ignore) {}
		}
		this.wapp = null;
		this.appMan = null;
		this.logger = null;
		this.humidityListener = null;
		this.messagingService = null;
		this.nameService = null;
		this.configsBase = null;
		if (wapp != null)
			ForkJoinPool.commonPool().submit(wapp::close);
	}

	@Override
	public void patternAvailable(final WarningConfigurationPattern pattern) {
		newPattern(pattern);
		appMan.getResourcePatternAccess().addPatternChangeListener(pattern, this, WarningConfigurationPattern.class);
	}

	private HumidityController newPattern(final WarningConfigurationPattern pattern) {
		final Room room = pattern.room.getLocationResource();
		if (configs.containsKey(room)) {
			logger.warn("Duplicate warning configuration {}, {}",pattern.model, configs.get(room).getConfig());
			return null;
		}
		final HumidityController controller = new HumidityController(pattern, appMan, messagingService, nameService);
		configs.put(room, controller); 
		humidityListener.patterns.values().stream()
			.filter(sensor -> sensor.room.equalsLocation(room))
			.forEach(sensor -> controller.addSensor(sensor, false));
		controller.valueChanged();
		return controller;
	}
	
	private HumidityController patternGone(final WarningConfigurationPattern pattern) {
		final Room room = pattern.room.getLocationResource();
		final HumidityController controller;
		if (configs.containsKey(room)) {
			controller = configs.remove(room);
		}
		else {
			final Optional<HumidityController> opt = configs.values().stream()
				.filter(c -> c.getConfig().model.equalsLocation(pattern.model))
				.findAny();
			if (opt.isPresent()) {
				controller = opt.get();
				configs.values().remove(controller);
			} else
				controller = null;
		}
		if (controller != null) {
			controller.close();
		}
		return controller;
	}
	
	@Override
	public void patternUnavailable(WarningConfigurationPattern pattern) {
		patternGone(pattern);
		appMan.getResourcePatternAccess().removePatternChangeListener(pattern, this);
	}

	@Override
	public void patternChanged(WarningConfigurationPattern instance, List<CompoundResourceEvent<?>> changes) {
		patternGone(instance);
		newPattern(instance);
	}
	
	private final static class HumidityListener implements PatternListener<HumidityPattern>, PatternChangeListener<HumidityPattern> {
		
		private final Map<String, HumidityPattern> patterns = new HashMap<>();
		private final Map<Room, HumidityController> configs;
		private final ResourcePatternAccess rpa;
		
		public HumidityListener(Map<Room, HumidityController> configs, ResourcePatternAccess rpa) {
			this.configs = configs;
			this.rpa = rpa;
		}

		@Override
		public void patternChanged(final HumidityPattern pattern, List<CompoundResourceEvent<?>> changes) {
			patterns.remove(pattern.model.getLocation());
			patterns.put(pattern.model.getLocation(), pattern);
		}
		
		private void newSensor(HumidityPattern pattern) {
			patterns.put(pattern.model.getLocation(), pattern);
			final HumidityController controller = configs.get(pattern.room.getLocationResource());
			if (controller != null)
				controller.addSensor(pattern, true);
		}
	
		private void sensorGone(HumidityPattern pattern) {
			patterns.remove(pattern.model.getLocation());
			configs.values().forEach(controller -> controller.removeSensor(pattern));
		}
		
		@Override
		public void patternAvailable(HumidityPattern pattern) {
			newSensor(pattern);
			rpa.addPatternChangeListener(pattern, this, HumidityPattern.class);
		}

		@Override
		public void patternUnavailable(HumidityPattern pattern) {
			sensorGone(pattern);
			rpa.removePatternChangeListener(pattern, this);
		}
		
	}
	
}
