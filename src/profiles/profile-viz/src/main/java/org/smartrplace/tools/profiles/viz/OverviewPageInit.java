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
package org.smartrplace.tools.profiles.viz;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.model.sensors.TemperatureSensor;
import org.osgi.service.component.ComponentServiceObjects;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.Profile;
import org.smartrplace.tools.profiles.ProfileGeneration;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.filedownload.Download;
import de.iwes.widgets.html.filedownload.DownloadData;
import de.iwes.widgets.html.fileupload.FileUpload;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.html5.SimpleGrid;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.html.plot.api.PlotType;
import de.iwes.widgets.reswidget.scheduleplot.api.TimeSeriesPlot;
import de.iwes.widgets.reswidget.scheduleplot.plotlyjs.SchedulePlotlyjs;
import de.iwes.widgets.reswidget.scheduleviewer.api.SchedulePresentationData;
import de.iwes.widgets.reswidget.scheduleviewer.DefaultSchedulePresentationData;
import de.iwes.widgets.template.DisplayTemplate;

class OverviewPageInit {

	private final WidgetPage<?> page;
	private final Header header;
	private final Alert alert;
	private final ProfileDropdown profilesSelector;
	private final Button downloadBtn;
	private final ButtonConfirm deleteBtn;
	private final Download download;
	private final FileUpload upload;
	private final Button uploadBtn;
	private final TemplateMultiselect<DataPoint> scheduleSelector;
	private final TimeSeriesPlot<?, ?, ?> schedulePlot;
	
	@SuppressWarnings("serial")
	public OverviewPageInit(final WidgetPage<?> page, final ApplicationManager appMan,
			final ComponentServiceObjects<ProfileGeneration> service) {
		this.page = page;
		page.setTitle("Profile visualization");
		this.header = new Header(page, "header", "Profiles visualization");
		header.setDefaultColor("blue");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		this.alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		this.profilesSelector = new ProfileDropdown(page, "profileDropdown", service, alert);
		this.scheduleSelector = new TemplateMultiselect<DataPoint>(page, "scheduleSelector") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final Profile profile = profilesSelector.getSelectedProfile();
				if (profile == null) {
					update(Collections.emptyList(), req);
					return;
				}
				// TODO select primary and/or context data, etc
				Stream<Map.Entry<DataPoint, Object>> stream = profile.getPrimaryData().entrySet().stream();
				stream = Stream.concat(stream, profile.getContextData().entrySet().stream());
				stream = Stream.concat(stream, profile.getDerivedData().entrySet().stream());
				final List<DataPoint> points = stream
						.filter(entry -> entry.getValue() instanceof ReadOnlyTimeSeries)
						.map(Map.Entry::getKey)
						.collect(Collectors.toList());
				update(points, req);
				selectItems(points, req);
			}
			
		};
		this.schedulePlot = new SchedulePlotlyjs(page, "schedulePlot", false) {
			
			private Class<?> getTypeInfo(final DataPoint dp) {
				final Class<?> type = dp.typeInfo();
				if (type == TemperatureSensor.class)
					return TemperatureResource.class;
				return type;
			}
			
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<DataPoint> points = scheduleSelector.getSelectedItems(req);
				final Profile profile = profilesSelector.getSelectedProfile();
				if (profile == null || points == null || points.isEmpty()) {
					getScheduleData(req).setSchedules(Collections.emptyMap());
					return;
				}
				// TODO select primary and/or context data, etc
				Stream<Map.Entry<DataPoint, Object>> stream = profile.getPrimaryData().entrySet().stream();
				stream = Stream.concat(stream, profile.getContextData().entrySet().stream());
				stream = Stream.concat(stream, profile.getDerivedData().entrySet().stream());
				final Map<String, SchedulePresentationData> schedules = stream
					.filter(entry -> points.contains(entry.getKey()))
					.filter(entry -> entry.getValue() instanceof ReadOnlyTimeSeries)
					.collect(Collectors.toMap(
						entry -> entry.getKey().label(req.getLocale()),
						entry -> new DefaultSchedulePresentationData((ReadOnlyTimeSeries) entry.getValue(), getTypeInfo(entry.getKey()), 
								entry.getKey().label(req.getLocale()), InterpolationMode.LINEAR)
					));
				getScheduleData(req).setSchedules(schedules);
			}
			
		};
		schedulePlot.getDefaultConfiguration().setPlotType(PlotType.LINE);
		this.download = new Download(page, "download", appMan) {
			
			public void onGET(OgemaHttpRequest req) {
				final Profile profile = profilesSelector.getSelectedProfile();
				if (profile != null) {
					final Consumer<OutputStream> consumer = stream -> {
						final ProfileGeneration pg = service.getService();
						try {
							pg.store(profile, stream);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						} finally {
							service.ungetService(pg);
						}
					};
					setSource(consumer, true, "application/json", req);
				}
			}
			
		};
		this.downloadBtn = new Button(page, "downloadBtn", "Download profile") {
			
			public void onGET(OgemaHttpRequest req) {
				final Profile profile = profilesSelector.getSelectedProfile();
				if (profile == null) {
					disable(req);
				}
				else {
					enable(req);
				}
			}
			
			public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
				final Profile profile = profilesSelector.getSelectedProfile();
				if (profile == null) {
					download.disable(req);
				}
				else {
					download.enable(req);
				}
			}
			
		};
		this.deleteBtn = new ButtonConfirm(page, "deleteBtn", "Delete profile") {
			
			public void onGET(OgemaHttpRequest req) {
				final Profile profile = profilesSelector.getSelectedProfile();
				if (profile == null) {
					disable(req);
				}
				else {
					enable(req);
				}
			}
			
			public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
				final String id = profilesSelector.getSelectedItem(req);
				if (id == null)
					return;
				final ProfileGeneration pg = service.getService();
				boolean success = false;
				Exception e = null;
				try {
					success = pg.removeStoredProfile(id);
				} catch (IOException io) {
					e = io;
				} finally {
					service.ungetService(pg);
				}
				if (success) 
					alert.showAlert("Profile " + id + " has been deleted", true, req);
				else {
					final StringBuilder msg = new StringBuilder();
					msg.append("Profile deletion failed");
					if (e != null)
						msg.append(": ").append(e);
					alert.showAlert(msg.toString(), false, req);
				}
			}
			
		};
		deleteBtn.setDefaultConfirmPopupTitle("Confirm deletion");
		deleteBtn.setDefaultConfirmMsg("Do you really want to delete the selected profile?");
		this.upload = new FileUpload(page, "upload", appMan) {
			
			public void onFinished(org.apache.commons.fileupload.FileItem item, OgemaHttpRequest req) {
				if (item == null)
					return;
				final ProfileGeneration pg = service.getService();
				try {
					final Profile profile = pg.read(item.getInputStream()); // TODO cathc exception and display in alert
					pg.storeProfile(profile);
				} catch (IOException e) {
					appMan.getLogger().warn("Upload failed",e);
				} finally {
					service.ungetService(pg);
				}
			}
			
		};
		this.uploadBtn = new Button(page, "uploadBtn", "Upload file");
		buildPage();
		setDependencies();
	}
	
	private final void buildPage() {
		final SimpleGrid grid = new SimpleGrid(page, "grid", true);
		grid.setDefaultAppendFillColumn(true);
		grid.setDefaultPrependFillColumn(true);
		grid.setDefaultColumnGap("1em");
		grid.setDefaultRowGap("1em");
		grid.addItem("Select profile", false, null).addItem(profilesSelector,false, null)
				.addItem("Delete", true, null).addItem(deleteBtn, false, null)
				.addItem("Download", true, null).addItem(downloadBtn, false, null)
				.addItem(upload, true, null).addItem(uploadBtn, false, null)
				.addItem("Select time series", true, null).addItem(scheduleSelector, false, null);
		page.append(header).linebreak().append(alert)
			.append(grid)
			.append(schedulePlot).linebreak()
			.append(download);
	}

	private final void setDependencies() {
		profilesSelector.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		profilesSelector.triggerAction(downloadBtn, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		profilesSelector.triggerAction(deleteBtn, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		profilesSelector.triggerAction(scheduleSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		profilesSelector.triggerAction(schedulePlot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		scheduleSelector.triggerAction(schedulePlot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		downloadBtn.triggerAction(download, TriggeringAction.POST_REQUEST, DownloadData.GET_AND_STARTDOWNLOAD);
		uploadBtn.triggerAction(upload, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);
		upload.triggerAction(profilesSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deleteBtn.triggerAction(profilesSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deleteBtn.triggerAction(deleteBtn, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		deleteBtn.triggerAction(downloadBtn, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		deleteBtn.triggerAction(schedulePlot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
	}
	
	@SuppressWarnings("serial")
	private static class ProfileDropdown extends TemplateDropdown<String> {
		
		private final ComponentServiceObjects<ProfileGeneration> service;
		private final Alert alert;
		private final ThreadLocal<Profile> profile = new ThreadLocal<>();

		public ProfileDropdown(WidgetPage<?> page, String id, 
				final ComponentServiceObjects<ProfileGeneration> service, final Alert alert) {
			super(page, id);
			setDefaultAddEmptyOption(true);
			this.service = service;
			this.alert = alert;
			setTemplate(new DisplayTemplate<String>() {
				
				@Override
				public String getLabel(String item, OgemaLocale req) {
					return item;
				}
				
				@Override
				public String getId(String item) {
					return item;
				}
			});
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final ProfileGeneration pg = service.getService();
			try {
				update(pg.getStoredProfileIds(null), req);
				final Profile selected = profile.get();
				if (selected != null) 
					selectItem(selected.id(), req);
				else
					selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
			} catch (IOException e) {
				LoggerFactory.getLogger(getClass()).warn("Could not update profiles",e);
				update(Collections.emptyList(), req);
			} finally {
				service.ungetService(pg);
			}
		}
		
		@Override
		public void onPOSTComplete(String data, OgemaHttpRequest req) {
			final ProfileGeneration pg = service.getService();
			try {
				final String id = getSelectedItem(req);
				if (id == null)
					profile.set(null);
				else
					profile.set(pg.getStoredProfile(id));
			} catch (IOException e) {
				alert.showAlert("Failed to load profile " + getSelectedItem(req), false, req);
			} finally {
				service.ungetService(pg);
			}
		}
		
		public Profile getSelectedProfile() {
			return profile.get();
		}
		
	}
	
}
