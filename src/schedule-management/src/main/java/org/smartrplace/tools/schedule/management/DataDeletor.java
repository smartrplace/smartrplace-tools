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
package org.smartrplace.tools.schedule.management;

import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.core.timeseries.TimeSeries;
import org.smartrplace.tools.schedule.management.ScheduleMgmtPage.TimeSeriesDependentWidget;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.extended.plus.SelectorTemplate;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirmData;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.form.checkbox.Checkbox;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;

class DataDeletor extends PageSnippet implements TimeSeriesDependentWidget {

	private static final long serialVersionUID = 1L;

	private final SelectorTemplate<ReadOnlyTimeSeries> scheduleSelector;
	private final Header deleteHeader;
	private final Label startLabel;
	private final Datepicker startPicker;
	private final Label endLabel;
	private final Datepicker endPicker;
	private final Checkbox fullScheduleSelected;
	private final ButtonConfirm deleteButton;
	private final Alert alert;
	
	DataDeletor(WidgetPage<?> page, String id, SelectorTemplate<ReadOnlyTimeSeries> scheduleSelector, final Alert alert) {
		super(page, id, true);
		this.alert = alert;
		this.scheduleSelector = scheduleSelector;
		this.deleteHeader = new Header(page, id + "__XX__deleteHeader", "Delete values");
		deleteHeader.setDefaultHeaderType(3);
		this.startLabel = new Label(page, id + "__XX__startLabel", "Start time");
		this.endLabel = new Label(page, id + "__XX__endLabel", "End time");
		this.startPicker = new Datepicker(page, id + "__XX__startPicker");
		this.endPicker = new Datepicker(page, id + "__XX__endPicker");
		this.fullScheduleSelected = new Checkbox(page, id + "__XX__fullScheduleSelected") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				boolean startCovered = false;
				boolean endCovered = false;
				SampledValue start = getValue(true, req);
				SampledValue end = getValue(false, req);
				if (start != null) {
					long selectedStart = startPicker.getDateLong(req);
					startCovered = (selectedStart/1000 <= start.getTimestamp()/1000); // ignore millis 
				}
				if (end != null) {
					long selectedEnd = endPicker.getDateLong(req);
					endCovered = (selectedEnd/1000 >= end.getTimestamp()/1000); 
				}
				Map<String,Boolean> map = getCheckboxList(req);
				map.put("Include schedule start", startCovered);
				map.put("Include schedule end", endCovered);
				setCheckboxList(map, req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				Map<String,Boolean> map = getCheckboxList(req);
				boolean startCovered = map.get("Include schedule start");
				boolean endCovered = map.get("Include schedule end");
				if (startCovered) {
					SampledValue start = getValue(true, req);
					if (start != null) {
						final long selectedStart = startPicker.getDateLong(req);
						final long scheduleStart = start.getTimestamp();
						if (selectedStart/1000 > scheduleStart/1000) // ignore millis  
							startPicker.setDate(scheduleStart, req);
					}
				}
				if (endCovered) {
					SampledValue end = getValue(false, req);
					if (end != null) {
						final long selectedEnd = endPicker.getDateLong(req);
						final long scheduleEnd = end.getTimestamp();
						if (selectedEnd/1000 < scheduleEnd/1000)  
							endPicker.setDate(scheduleEnd, req);
					}
				}
				
			}
			
		};
		this.deleteButton = new ButtonConfirm(page, id + "__XX__deleteButton", "Delete values") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				TimeSeries schedule = getSchedule(req);
				long start = startPicker.getDateLong(req);
				long end = endPicker.getDateLong(req);
				int size = schedule.size(start, end);
				schedule.deleteValues(start, end);
				alert.showAlert(size + " data points deleted", true, req);
			}
			
		};
		deleteButton.setDefaultConfirmPopupTitle("Delete interval");
		deleteButton.setDefaultCancelBtnMsg("Cancel");
		deleteButton.setDefaultConfirmBtnMsg("Delete");
		deleteButton.setDefaultConfirmMsg("Do you really want to delete the selected interval?");
		deleteButton.addDefaultStyle(ButtonConfirmData.CANCEL_LIGHT_BLUE);
		deleteButton.addDefaultStyle(ButtonConfirmData.CONFIRM_RED);
		
		buildWidget();
		setDependencies();
	}
	
	private final void buildWidget() {
		this.append(deleteHeader, null);
		StaticTable tab = new StaticTable(3, 2, new int[]{2,3});
		tab.setContent(0, 0, startLabel).setContent(0, 1, startPicker)
			.setContent(1, 0, endLabel).setContent(1, 1, endPicker)
			.setContent(2, 0, fullScheduleSelected).setContent(2, 1, deleteButton);
		this.append(tab, null);
	}
	
	private final void setDependencies() {
		scheduleSelector.triggerAction(startPicker, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		scheduleSelector.triggerAction(endPicker, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		scheduleSelector.triggerAction(fullScheduleSelected, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST,1);
		startPicker.triggerAction(fullScheduleSelected, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		endPicker.triggerAction(fullScheduleSelected, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		fullScheduleSelected.triggerAction(startPicker, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		fullScheduleSelected.triggerAction(endPicker, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deleteButton.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deleteButton.triggerAction(scheduleSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	private TimeSeries getSchedule(OgemaHttpRequest req) {
		final ReadOnlyTimeSeries schedule = scheduleSelector.getSelectedItem(req);
		if (!(schedule instanceof TimeSeries) || schedule.isEmpty()) {
			return null;
		}
		return (TimeSeries) schedule;
	}
	
	private SampledValue getValue(boolean startOrEnd, OgemaHttpRequest req) {
		TimeSeries schedule = getSchedule(req);
		if (schedule == null)
			return null;
		final SampledValue sv;
		if (startOrEnd)
			sv = schedule.getNextValue(Long.MIN_VALUE);
		else 
			sv = schedule.getPreviousValue(Long.MAX_VALUE);
		return sv;
	}

	@Override
	public void update(ReadOnlyTimeSeries newSchedule, OgemaHttpRequest req) {
		if (!(newSchedule instanceof TimeSeries) || newSchedule.isEmpty()) {
			long now = System.currentTimeMillis();
			startPicker.setDate(now, req);
			endPicker.setDate(now, req);
			return;
		}
		TimeSeries ts = (TimeSeries) newSchedule;
		SampledValue sv = ts.getNextValue(Long.MIN_VALUE);
		long date = (sv != null ? sv.getTimestamp() : System.currentTimeMillis());
		startPicker.setDate(date, req);
		
		sv = ts.getPreviousValue(Long.MAX_VALUE);
		date = (sv != null ? sv.getTimestamp() : System.currentTimeMillis());
		endPicker.setDate(date, req);
	}
 	
}
