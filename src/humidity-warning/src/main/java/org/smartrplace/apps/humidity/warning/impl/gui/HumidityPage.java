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
package org.smartrplace.apps.humidity.warning.impl.gui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.humidity.warning.impl.Utils;
import org.smartrplace.apps.humidity.warning.impl.pattern.HumidityPattern;
import org.smartrplace.apps.humidity.warning.impl.pattern.TemperaturePattern;
import org.smartrplace.apps.humidity.warning.impl.pattern.WarningConfigurationPattern;
import org.smartrplace.apps.humidity.warning.model.WarningConfiguration;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.services.NameService;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.DynamicTableData;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.checkbox.Checkbox2;
import de.iwes.widgets.html.form.checkbox.CheckboxEntry;
import de.iwes.widgets.html.form.checkbox.DefaultCheckboxEntry;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.slider.Slider;
import de.iwes.widgets.html.form.textfield.ValueInputField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.icon.Icon;
import de.iwes.widgets.html.icon.IconType;

public class HumidityPage {
	
	private final static long MINUTE = 60 * 1000; // millis in a minute 
	private final WidgetPage<?> page;
	private final Header header;
	private final Alert alert;
	private final Label outsideLabel;
	private final Label outsideTemp;
	private final DynamicTable<RoomConfig> roomTable;
	
	@SuppressWarnings("serial")
	public HumidityPage(final WidgetPage<?> page, final ResourcePatternAccess rpa, final ResourceList<WarningConfiguration> configsBase) {
		this.page = page;
		this.header = new Header(page, "header", "Humidity Warnings");
		this.alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		this.header.setDefaultColor("blue");
		this.outsideLabel = new Label(page, "outsideLab");
		outsideLabel.setDefaultText("Outside temperature:");
		outsideTemp = new Label(page, "outsideTemp") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final OptionalDouble opt = rpa.getPatterns(TemperaturePattern.class, AccessPriority.PRIO_LOWEST).stream()
					.filter(sensor -> sensor.room != null)
					.filter(sensor -> sensor.room.type().isActive() && sensor.room.type().getValue() == 0)
					.mapToDouble(sensor -> sensor.reading.getCelsius())
					.average();
				if (!opt.isPresent())
					setText("unknown", req);
				else
					setText(String.format("%.2f°C", opt.getAsDouble()), req);
			}
			
		};
		
		this.roomTable = new DynamicTable<RoomConfig>(page, "roomTable") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				updateRows(getByRooms(rpa), req);
			}
			
		};
		roomTable.addDefaultStyle(DynamicTableData.BOLD_HEADER);
		roomTable.setRowTemplate(new RowTemplate<RoomConfig>() {
			
			private final Map<String, Object> header = new LinkedHashMap<>();
			
			{
				header.put("room", "Room");
				header.put("sensors", "Sensors");
				header.put("humidity", "Current humidity");
				header.put("temp", "Temperature");
				header.put("dew", "Dew point");
				header.put("warnings", "Warnings");

			}
			
			@Override
			public String getLineId(RoomConfig object) {
				return object.room.getLocation();
			}
			
			@Override
			public Map<String, Object> getHeader() {
				return header;
			}
			
			@Override
			public Row addRow(final RoomConfig object, final OgemaHttpRequest req) {
				final Row row = new Row();
				final String id = getLineId(object);
				final Label roomLabel = new RoomLabel(roomTable, "room_" + id , req, object);
				final Label sensorsLabel = new SensorsLabel(roomTable, "sensors_" + id, req, object);
				final Label humidityLabel = new HumidityLabel(roomTable, "humidity_" + id, req, object);
				final Label tempLabel = new TemperatureLabel(roomTable, "temp_" + id, req, object);
				final Label dewPointLabel = new DewPointLabel(roomTable, "dew_" + id, req, object);
				
				final PageSnippet warningsSnippet = new PageSnippet(roomTable, "warnings_" + id, req);
				warningsSnippet.append(new WarningsFlex(warningsSnippet, "lowFlex_" + id, req, object, true, configsBase), req)
					.append(new WarningsFlex(warningsSnippet, "highFlex_" + id, req, object, false, configsBase), req);
				
				row.addCell("room", roomLabel, 1);
				row.addCell("sensors", sensorsLabel, 1);
				row.addCell("humidity", humidityLabel, 1);
				row.addCell("temp", tempLabel, 1);
				row.addCell("dew", dewPointLabel, 1);
				row.addCell("warnings", warningsSnippet, 7);

				return row;
			}
		});
		buildPage();
		setDependencies();
	}
	
	private final void buildPage() {
		page.append(header).linebreak().append(alert).append(new StaticTable(1, 3, new int[] {2,2,8})
			.setContent(0, 0, outsideLabel).setContent(0, 1, outsideTemp)
		).linebreak().append(roomTable);
	}
	
	private final void setDependencies() {
		
	}
	
	@SuppressWarnings("serial")
	private static final class WarningsFlex extends Flexbox {

		private final Checkbox2 active;
		private final Slider humiditySlider; // null for dew point
		private final Icon icon;
		private final Label timeoutLabel;
		private final ValueInputField<Integer> timeoutMinutes;
		
		public WarningsFlex(final OgemaWidget parent, final String id, final OgemaHttpRequest req, final RoomConfig room, final boolean highOrLow, 
				final ResourceList<WarningConfiguration> cfgsBase) {
			super(parent, id, req);
			this.active = new Checkbox2(this, "check_" + highOrLow + "_" + id, req) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					final boolean active = room.warningConfig != null && (highOrLow ? room.warningConfig.upperThresholdHumidity.isActive() :
						room.warningConfig.lowerThresholdHumidity.isActive());
					setState("", active, req);
				}
				
				@Override
				public synchronized void onPOSTComplete(String data, OgemaHttpRequest req) {
					final boolean active = isChecked("", req);
					if (!active && room.warningConfig != null) {
						if (highOrLow)
							room.warningConfig.upperThresholdHumidity.delete();
						else
							room.warningConfig.lowerThresholdHumidity.delete();
						if (!room.warningConfig.upperThresholdHumidity.isActive() && !room.warningConfig.lowerThresholdHumidity.isActive()) { 
							room.warningConfig.model.delete();
							room.warningConfig = null;
						}
					} else if (active) {
						WarningConfigurationPattern cfg = room.warningConfig;
						if (cfg == null) {
							final WarningConfiguration cfg0 = cfgsBase.add();
							cfg0.room().setAsReference(room.room);
							cfg = new WarningConfigurationPattern(cfg0);
							room.warningConfig = cfg;
						}
						final int value = humiditySlider.getValue(req);
						final FloatResource target  = highOrLow ? cfg.upperThresholdHumidity : cfg.lowerThresholdHumidity;
						target.<FloatResource> create().setValue(((float) value)/100);
						target.activate(false);
						cfg.model.activate(false);
					}
				}
				
			};
			final boolean nowActive = room.warningConfig != null && (highOrLow ? room.warningConfig.upperThresholdHumidity.isActive() :
				room.warningConfig.lowerThresholdHumidity.isActive());
			final CheckboxEntry entry = new DefaultCheckboxEntry("", highOrLow ? "High humidity" : "Low humidity", nowActive);
//			active.addDefaultEntry(entry);
			active.addEntry(entry, req);
			final int defaultThreshold = room.getHumidityThreshold(highOrLow);
			// TODO introduce classes
			humiditySlider = new Slider(this, "humSlider_" + highOrLow + "_" + id, req, 0, 100, defaultThreshold) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					setValue(room.getHumidityThreshold(highOrLow), req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					final int value = getValue(req);
					room.setHumidityThreshold(highOrLow, value); // does nothing if config is not active already
				}
				
			};
			humiditySlider.setMargin("-0.5em", true, false, false, false, req);
			humiditySlider.setMargin("0.5em", false, true, false, true, req);
			this.icon = new Icon(this, "icon_" + highOrLow + "_" + id, req) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					if (!active.isChecked("", req)) {
						setIconType(null, req);
						return;
					}
					final boolean violated = room.thresholdViolated(highOrLow);
					setIconType(violated ? IconType.FAIL : IconType.CHECK_MARK, req);	
				}
				
			};
			this.timeoutLabel = new Label(this, "timeoutLab_" + highOrLow + "_" + id, req) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					final WarningConfigurationPattern cfg = room.warningConfig;
					if (cfg == null || !active.isChecked("", req)) {
						setWidgetVisibility(false, req);
						return;
					}
					setWidgetVisibility(true, req);
				}
				
			};
			timeoutLabel.setText("Timeout in minutes", req);
			timeoutLabel.setToolTip("A message will be sent only after the threshold has been violated for this long.", req);
			timeoutLabel.setMargin("0.5em", true, true, false, false, req);
			this.timeoutMinutes = new ValueInputField<Integer>(this, "timeout_" + highOrLow + "_" + id, Integer.class, req) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					final WarningConfigurationPattern cfg = room.warningConfig;
					if (cfg == null || !active.isChecked("", req)) {
						setWidgetVisibility(false, req);
						return;
					}
					setWidgetVisibility(true, req);
					final TimeResource timeout = highOrLow ? cfg.upperTimeout : cfg.lowerTimeout;
					if (!timeout.isActive())
						setNumericalValue(0, req);
					else
						setNumericalValue((int) (timeout.getValue() / MINUTE), req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					final WarningConfigurationPattern cfg = room.warningConfig;
					if (cfg == null || !active.isChecked("", req)) {
						return;
					}
					final TimeResource timeout = highOrLow ? cfg.upperTimeout : cfg.lowerTimeout;
					timeout.create();
					timeout.setValue(getNumericalValue(req) * MINUTE);
					timeout.activate(false);
				}
				
			};
			timeoutMinutes.setLowerBound(0, req);
			timeoutMinutes.setMargin("0.5em", true, true, false, true, req);

			buildWidget(req);
			setDependencies(req);
			
		}
		
		private final void buildWidget(final OgemaHttpRequest req) {
			addItem(active, req).addItem(humiditySlider, req)
				.addItem(timeoutLabel, req).addItem(timeoutMinutes, req).addItem(icon, req);
		}
		
		private final void setDependencies(final OgemaHttpRequest req) {
			active.triggerAction(humiditySlider, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			active.triggerAction(icon, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			active.triggerAction(timeoutLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			active.triggerAction(timeoutMinutes, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			humiditySlider.triggerAction(icon, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
		}
		
	}
	
	@SuppressWarnings("serial")
	private static final class RoomLabel extends Label {

		private final RoomConfig room;
		
		public RoomLabel(OgemaWidget parent, String id, OgemaHttpRequest req, RoomConfig room) {
			super(parent, id, req);
			this.room = room;
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final String name = Utils.getName(room.room, getNameService(), req.getLocale());
			setText(name, req);
			setToolTip(room.room.getLocation(), req);
		}
	}
	
	@SuppressWarnings("serial")
	private static final class SensorsLabel extends Label {

		private final RoomConfig room;
		
		public SensorsLabel(OgemaWidget parent, String id, OgemaHttpRequest req, RoomConfig room) {
			super(parent, id, req);
			this.room = room;
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final NameService nameService = getNameService();
			final OgemaLocale locale = req.getLocale();
			final StringBuilder sb = new StringBuilder();
			final StringBuilder tooltipBuilder = new StringBuilder();
			boolean first = true;
			for (HumidityPattern sensor: room.humiditySensors) {
				if (!first) {
					sb.append("<br>");
					tooltipBuilder.append(',').append('\n');
				}
				first = false;
				sb.append(Utils.getName(sensor.model, nameService, locale));
				tooltipBuilder.append(sensor.model.getLocation());
			}
			setHtml(sb.toString(), req);
			setToolTip(tooltipBuilder.toString(), req);
		}
	}
	
	@SuppressWarnings("serial")
	private static final class HumidityLabel extends Label {

		private final RoomConfig room;
		
		public HumidityLabel(OgemaWidget parent, String id, OgemaHttpRequest req, RoomConfig room) {
			super(parent, id, req);
			this.room = room;
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (HumidityPattern sensor: room.humiditySensors) {
				if (!first)
					sb.append("<br>");
				if (!sensor.reading.isActive())
					sb.append("No value");
				else {
					final int value = (int) (sensor.reading.getValue() * 100);
					sb.append(value).append('%');
				}
			}
			setHtml(sb.toString(), req);
		}
	}
	
	@SuppressWarnings("serial")
	private static final class TemperatureLabel extends Label {

		private final RoomConfig room;
		
		public TemperatureLabel(OgemaWidget parent, String id, OgemaHttpRequest req, RoomConfig room) {
			super(parent, id, req);
			this.room = room;
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final float temp = room.getAverageTemperatureCelsius();
			if (Float.isNaN(temp))
				setText("-", req);
			else
				setText(String.format("%.2f",temp) + "°C", req);
		} 
	}
	
	@SuppressWarnings("serial")
	private static final class DewPointLabel extends Label {

		private final RoomConfig room;
		
		public DewPointLabel(OgemaWidget parent, String id, OgemaHttpRequest req, RoomConfig room) {
			super(parent, id, req);
			this.room = room;
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final float temp = room.getDewPointCelsius();
			if (Float.isNaN(temp))
				setText("-", req);
			else
				setText(String.format("%.2f",temp) + "°C", req);
		} 
	}
	
	private final static Collection<RoomConfig> getByRooms(final ResourcePatternAccess rpa) {
		final List<HumidityPattern> sensors = rpa.getPatterns(HumidityPattern.class, AccessPriority.PRIO_LOWEST);
		final Map<String, RoomConfig> rooms = new LinkedHashMap<>();
		for (HumidityPattern sensor: sensors) {
			final Room r = sensor.room;
			if (r == null)
				continue;
			final String loc = r.getLocation();
			RoomConfig cfg = rooms.get(loc);
			if (cfg == null) {
				cfg = new RoomConfig(r);
				rooms.put(loc, cfg);
			}
			cfg.humiditySensors.add(sensor);
		}
		rpa.getPatterns(WarningConfigurationPattern.class, AccessPriority.PRIO_LOWEST).forEach(warning -> {
			final Room r = warning.room;
			RoomConfig cfg = rooms.get(r.getLocation());
			if (cfg == null) {
				cfg = new RoomConfig(r);
				rooms.put(r.getLocation(), cfg);
			}
			cfg.warningConfig = warning;
		});
		rpa.getPatterns(TemperaturePattern.class, AccessPriority.PRIO_LOWEST).forEach(tempSens -> {
			final Room r = tempSens.room;
			if (r == null)
				return;
			final RoomConfig cfg = rooms.get(r.getLocation());
			if (cfg == null)
				return;
			cfg.temperatureSensors.add(tempSens);
		});
		rooms.values().forEach(r -> r.calcAverages());
		return rooms.values();
	}

}
