/**
 * Copyright 2018 Smartrplace UG
 *
 * Schedule management is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Schedule management is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartrplace.tools.schedule.management;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.core.timeseries.TimeSeries;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.recordeddata.RecordedDataStorage;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.timeseries.api.MemoryTimeSeries;
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;
import org.ogema.tools.timeseries.implementations.TreeTimeSeries;
import org.osgi.framework.FrameworkUtil;
import org.smartrplace.tools.schedule.management.imports.DataGenerator;
import org.smartrplace.tools.schedule.management.imports.FileBasedDataGenerator;
import org.smartrplace.tools.schedule.management.imports.OgemaDataSource;
import org.smartrplace.tools.schedule.management.persistence.FileBasedPersistence;
import org.smartrplace.tools.schedule.management.persistence.OgemaTimeSeriesPersistence;
import org.smartrplace.tools.schedule.management.persistence.TimeSeriesPersistence;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.extended.mode.UpdateMode;
import de.iwes.widgets.api.extended.plus.SelectorTemplate;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.accordion.Accordion;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.filedownload.FileDownload;
import de.iwes.widgets.html.filedownload.FileDownloadData;
import de.iwes.widgets.html.fileupload.FileUpload;
import de.iwes.widgets.html.fileupload.FileUploadListener;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.checkbox.Checkbox;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.form.textfield.ValueInputField;
import de.iwes.widgets.html.plot.api.PlotType;
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.html.schedulemanipulator.ScheduleManipulator;
import de.iwes.widgets.html.selectiontree.SelectionTree;
import de.iwes.widgets.latch.LatchWidget;
import de.iwes.widgets.latch.LatchWidgetData;
import de.iwes.widgets.resource.timeseries.OnlineTimeSeries;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.resource.widget.dropdown.ResourceTypeDropdown;
import de.iwes.widgets.reswidget.scheduleplot.flot.ScheduleDataFlot;
import de.iwes.widgets.reswidget.scheduleplot.flot.SchedulePlotFlot;
import de.iwes.widgets.reswidget.scheduleviewer.DefaultSchedulePresentationData;
import de.iwes.widgets.reswidget.scheduleviewer.api.SchedulePresentationData;
import de.iwes.widgets.template.DisplayTemplate;
import de.iwes.widgets.template.LabelledItem;

/*
 * TODO
 *  - tab: schedule manipulation
 *  - tab: store schedule -> REST format not working yet
 *  - evaluate config id to set selected schedule initially
 *  - upon data import, recalculate start and end time for plot (and main overview)
 *  - use widget groups to reduce requests
 */
class ScheduleMgmtPage {

	private final WidgetPage<?> page;
	private final Header header;
	private final Alert alert;
	private final TimeSeriesSelector mainSelector; 
	private final Accordion accordion;
	
	// load existing schedule
	private final DataSourceWidget scheduleSelector;
	private final Label firstTimestamp;
	private final Label lastTimestamp;
	private final Button nrPointsTrigger;
	private final Label nrPoints;
	private final Button selectExistingSchedule;
	private final Button copyExistingSchedule;
	
	// create new schedule
	private final PageSnippet creatorTriggers;
	private final Dropdown createScheduleSelector; // select type: Memory, schedule, slots
	private final Button createMemorySchedule;
	private final Button openScheduleCreationPopup;
	private final Popup scheduleCreationPopup;
	private final ResourceTypeDropdown parentTypeSelector;
	private final ResourceDropdown<SingleValueResource> parentResourceSelector;
	private final TextField scheduleCreationNameField;
	private final Button scheduleCreationSubmit;

	private final Button openSlotsCreationButton;
	private final Popup slotsCreationPopup;
	// think of CloseableDataRecorder... but we do not want to introduce a direct dependency
	private final TemplateDropdown<DataRecorder> slotsSelector;
	private final TextField newSlotsId;
	private final Button slotsCreationSubmit;
	
	// load data
	private final TemplateDropdown<LabelledItem> importSelector;
	private final PageSnippet importTriggers;
	private final Button openScheduleImportButton;
	private final Popup scheduleImportPopup;
	private final DataSourceWidget loadScheduleDataSelector;
	private final ImportOptionsCheckbox loadScheduleReplaceCheckbox;
	private final ValueInputField<Integer> loadScheduleRepeatTimes;
	private final Button loadScheduleDataSubmit;
	
	private final Button openFileImportButton;
	private final Popup fileImportPopup;
	private final Label fileImportDescription;
	private final Label fileImportOptionsLabel;
	private final Label fileImportSupportedFilesLabel;
	private final TextField fileImportDelimiterField;
	private final ImportOptionsCheckbox fileImportReplaceCheckbox;
	private final ValueInputField<Integer> fileImportRepeatTimes;
	private final FileUpload fileImportUpload;
	private final Button loadFileDataSubmit;
	private final FileUploadLatch fileImportWaiter;
	
	// plot data
	private final Datepicker plotStartTime;
	private final Datepicker plotEndTime;
	private final TemplateDropdown<PlotType> plotInterpolationMode;
	private final Button plotTrigger;
	private final SchedulePlotFlot plot;
	
	// manipulate data
	private final ScheduleManipulator scheduleManipulator;
	private final DataDeletor dataDeletion;
	
	// store data
	private final TemplateDropdown<TimeSeriesPersistence> persistenceSelector;
	private final PageSnippet saveTriggers;
		// ogema persistence
	private final Button openOgemaPersistencePopup;
	private final Popup ogemaPersistencePopup;
	// DataSourceWidget is also suitable for targets
	private final DataSourceWidget ogemaPersistenceSelector;
	private final Checkbox ogemaPersistenceReplaceCheckbox;
	private final Button ogemaPersistenceSave;
		// file based persistence
	private final Button openFilePersistencePopup;
	private final Popup filePersistencePopup;
	private final TextField filePersistenceDelimiterField;
	private final Label filePersistentOptionLabel;
	private final Button generateDownloadFile;
	private final FileDownload filePersistenceDownload;
	private final Button filePersistenceDownloadStart;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	ScheduleMgmtPage(
			WidgetPage<?> page, 
			DataSourceFactory dataSources, 
			Set<DataProvider<?>> dataSources2,
			FileBasedDataGeneratorFactory fileSources, 
			ApplicationManager am) {
		this.page = page;
		this.header = new Header(page, "header", "Schedule management");
		header.addDefaultStyle(HeaderData.CENTERED);
		this.alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		
		this.mainSelector = new TimeSeriesSelector(page, "mainSelector");
		this.firstTimestamp = new Label(page, "firstTimestamp") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				SampledValue first = null;
				if (rots != null)
					first = rots.getNextValue(Long.MIN_VALUE);
				if (first == null) {
					setText("", req);
					return;
				}
				setText(TimeUtils.getDateAndTimeString(first.getTimestamp()), req);
			}
			
		};
		this.lastTimestamp = new Label(page, "lastTimestamp") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				SampledValue last = null;
				if (rots != null)
					last = rots.getPreviousValue(Long.MAX_VALUE);
				if (last == null) {
					setText("", req);
					return;
				}
				setText(TimeUtils.getDateAndTimeString(last.getTimestamp()), req);
			}
			
		};
		this.nrPointsTrigger = new Button(page, "nrPointsTrigger","Calculate number of points");
		this.nrPoints = new Label(page, "nrPoints", "") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				if (rots == null) {
					setText("", req);
					return;
				}
				setText(String.valueOf(rots.size()), req);
			}
			
		};
		
		this.accordion = new Accordion(page, "mainTabsAccordion", true);
		
		this.scheduleSelector = new DataSourceWidget(page, "scheduleSelector", dataSources, dataSources2);
		
		this.selectExistingSchedule = new Button(page, "selectExistingSchedule", "Select active schedule") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = scheduleSelector.getSelectedSchedule(req);
				if (rots == null) { 
					disable(req);
					setText("", req);
				}
				else {
					enable(req);
					setText("Select active schedule",req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = scheduleSelector.getSelectedSchedule(req);
				if (rots == null) 
					return;
				mainSelector.selectItem(rots, req);
			}
			
		};
		this.copyExistingSchedule = new Button(page, "copyExistingSchedule", "Copy schedule") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = scheduleSelector.getSelectedSchedule(req);
				if (!(rots instanceof Schedule || rots instanceof RecordedData) ) { // not exactly clear why, but it's fine with online schedules
					disable(req);
					setText("", req);
				}
				else {
					enable(req);
					setText("Copy schedule and select",req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = scheduleSelector.getSelectedSchedule(req);
				if (rots == null) 
					return;
				MemoryTimeSeries mts = new TreeTimeSeries(rots, FloatValue.class); // FIXME do not assume FloatValue type
				mainSelector.selectItem(mts, req);
			}
			
		};
		
		this.createScheduleSelector = new Dropdown(page, "createScheduleSelector");
		List<DropdownOption> opts = new ArrayList<>();
		opts.add(new DropdownOption("memory", "Memory time series", true));
		opts.add(new DropdownOption("schedule", "Schedule", false));
		opts.add(new DropdownOption("recordedData", "SlotsDb timeseries", false));
		createScheduleSelector.setDefaultOptions(opts);
		
		this.createMemorySchedule = new Button(page, "createMemorySchedule", "Create") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				String selected = createScheduleSelector.getSelectedValue(req);
				setWidgetVisibility("memory".equals(selected), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				TreeTimeSeries timeSeries = new FloatTreeTimeSeries();
				mainSelector.selectItem(timeSeries, req);
			}
		};
		
		this.openScheduleCreationPopup = new Button(page, "openScheduleCreationPopup", "Create") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				String selected = createScheduleSelector.getSelectedValue(req);
				setWidgetVisibility("schedule".equals(selected), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				parentTypeSelector.selectSingleOption(DropdownData.EMPTY_OPT_ID, req); // important to avoid excessive options loading
			}
		};
		this.openSlotsCreationButton = new Button(page, "openSlotsCreationPopup", "Create") {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				String selected = createScheduleSelector.getSelectedValue(req);
				setWidgetVisibility("recordedData".equals(selected), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				slotsSelector.selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
			}
			
		};
		creatorTriggers = new PageSnippet(page, "createTriggers", true);
		creatorTriggers.append(createMemorySchedule, null).append(openScheduleCreationPopup, null).append(openSlotsCreationButton, null);
		
		// FIXME expensive resource operation on start
		this.parentTypeSelector = new ResourceTypeDropdown(page, "parentTypeSelector", (List) dataSources.logData.getHighLevelOptions());
		parentTypeSelector.setDefaultAddEmptyOption(true);
		this.parentResourceSelector = new ResourceDropdown<SingleValueResource>(page, "parentResourceSelector", false, SingleValueResource.class, 
					UpdateMode.AUTO_ON_GET, am.getResourceAccess()) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				Class<? extends SingleValueResource> type = (Class<? extends SingleValueResource>) parentTypeSelector.getSelectedType(req);
				setType(type, req); 
			}
			
		};
		
		this.scheduleCreationNameField = new TextField(page, "scheduleCreationTextField");
		this.scheduleCreationSubmit = new Button(page, "scheduleCreationSubmit", "Create") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				SingleValueResource svr = parentResourceSelector.getSelectedItem(req);
				if (svr == null) {
					alert.showAlert("Please select a parent resource first", false, req);
					return;
				}
				String name = scheduleCreationNameField.getValue(req);
				if (!Utils.isValidResourceName(name)) {
					alert.showAlert("Please enter a valid resource name. Got: " + name, false, req);
					return;
				}
				Resource r = svr.getSubResource(name);
				Schedule schedule;
				if (r != null) { // either virtual or existing, doesn't matter
					if (!(r instanceof Schedule)) {
						alert.showAlert("Subresource " + name + " exists, possibly as virtual resource, and is of incompatible type", false, req);
						return;
					}
					schedule = (Schedule) r;
				}
				else 
					schedule = svr.getSubResource(name, AbsoluteSchedule.class);
				schedule.create();
				mainSelector.selectItem(schedule, req);
				alert.showAlert("Schedule " + schedule.getPath() + " created.", true, req);
			}
			
		};
		PageSnippet creationPopSnippet = new PageSnippet(page, "creationPopupSnippet",true);
		StaticTable creationTable = new StaticTable(3, 2);
		creationTable.setContent(0, 0, "Select parent type").setContent(0, 1, parentTypeSelector)
			.setContent(1, 0, "Select parent resource").setContent(1, 1, parentResourceSelector)
			.setContent(2, 0, "Select schedule name").setContent(2, 1, scheduleCreationNameField);
		creationPopSnippet.append(creationTable, null);
		this.scheduleCreationPopup = new Popup(page, "scheduleCreationPopup",true);
		scheduleCreationPopup.setDefaultTitle("Create new schedule");
		scheduleCreationPopup.setDefaultHeaderHTML("New schedule");
		scheduleCreationPopup.setBody(creationPopSnippet,null);
		scheduleCreationPopup.setFooter(scheduleCreationSubmit, null);
		
		this.slotsSelector = new TemplateDropdown<DataRecorder>(page, "slotsSelector") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
			final Collection<DataRecorder> instances = AccessController.doPrivileged(new PrivilegedAction<Collection<DataRecorder>>() {

					@Override
					public Collection<DataRecorder> run() {
						for (OgemaTimeSeriesPersistence<?, ?> target : dataSources.getTargets()) {
							// need to use reflections to avoid direct dependency to SlotsDbStandalone
							Method method;
							try {
								method = target.getClass().getMethod("getAllInstances");
								return (Collection<DataRecorder>) method.invoke(target);
							} catch (NoSuchMethodException | ClassCastException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								continue;
							}
						}
						return Collections.emptyList();
					}
				});
				update(instances, req);
			}
			
		};
		slotsSelector.setTemplate(new DisplayTemplate<DataRecorder>() {
			
			// think CloseableDataRecorder
			private String getPath(DataRecorder slots) {
				return AccessController.doPrivileged(new PrivilegedAction<String>() {

					@Override
					public String run() {
						try {
							Method m = slots.getClass().getMethod("getPath");
							return (String) m.invoke(slots);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
							e.printStackTrace();
							return null;
						}
					}
				});
			}
			
			@Override
			public String getId(DataRecorder slots) {
				return getPath(slots);
			}

			@Override
			public String getLabel(DataRecorder slots, OgemaLocale arg1) {
				return getPath(slots);
			}
		});
		slotsSelector.setDefaultAddEmptyOption(true);
		this.newSlotsId = new TextField(page, "newSlotsId");
		this.slotsCreationSubmit = new Button(page, "slotsCreationSubmit", "Create SlotsDb timeseries") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				DataRecorder instance = slotsSelector.getSelectedItem(req);
				if (instance == null) {
					alert.showAlert("Please select a SlotsDb instance first", false, req);
					return;
				}
				String name = newSlotsId.getValue(req);
				if (name == null || name.trim().isEmpty()) {
					alert.showAlert("Please select a name for the new SlotsDb instance", false, req);
					return;
				}
				try {
					RecordedDataStorage rds = instance.createRecordedDataStorage(name, null);
					mainSelector.selectItem(rds, req);
					alert.showAlert("New SlotsDb timeseries: " + rds.getPath(), true, req);
				} catch (Exception e) {
					alert.showAlert("Could not create SlotsDb timeseries, an error occured: " + e, false, req);
				}
			}
		};
		this.slotsCreationPopup = new Popup(page, "slotsCreationPopup", true);
		creationPopSnippet = new PageSnippet(page, "slotsCreationSnippet", true);
		creationTable = new StaticTable(2, 2);
		creationTable.setContent(0, 0, "Select SlotsDb instance").setContent(0, 1, slotsSelector)
			.setContent(1, 0, "Select time series id").setContent(1, 1, newSlotsId);
		creationPopSnippet.append(creationTable, null);
		slotsCreationPopup.setDefaultTitle("SlotsDb timeseries creation");
		slotsCreationPopup.setDefaultHeaderHTML("New timeseries");
		slotsCreationPopup.setBody(creationPopSnippet, null);
		slotsCreationPopup.setFooter(slotsCreationSubmit, null);
		
		// load data
		
		this.importSelector = new TemplateDropdown<LabelledItem>(page, "importSelector") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				List<LabelledItem> generators = new ArrayList<>(dataSources.getSources());
				generators.addAll((Set) dataSources2);
				generators.addAll(fileSources.getSources());
				update(generators, req);
			}
		};  
		importSelector.setTemplate((DisplayTemplate) DataGenerator.TEMPLATE);
		this.openFileImportButton = new Button(page, "openFileImportButton") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				LabelledItem generator = importSelector.getSelectedItem(req);
				if (!(generator instanceof FileBasedDataGenerator)) {
					disable(req);
					setText("", req);
					setWidgetVisibility(false, req);
				}
				else {
					enable(req);
					setText("Import from file", req);
					setWidgetVisibility(true, req);
				}
			}
			
		};
		this.openScheduleImportButton = new Button(page, "openScheduleImportButton") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				LabelledItem generator = importSelector.getSelectedItem(req);
				if (!(generator instanceof OgemaDataSource) && (!(generator instanceof DataProvider<?>))) {
					disable(req);
					setText("", req);
					setWidgetVisibility(false, req);
				}
				else {
					enable(req);
					if (generator instanceof OgemaDataSource)
						setText("Import from OGEMA time series", req);
					else
						setText("Import from " + generator.label(req.getLocale()), req);
					setWidgetVisibility(true, req);
				}
			}
			
		};
		this.importTriggers = new PageSnippet(page, "importTriggers",true);
		importTriggers.append(openFileImportButton, null).append(openScheduleImportButton, null);
		
		this.loadScheduleDataSelector = new DataSourceWidget(page, "loadScheduleDataSelector", dataSources, 
				dataSources2, importSelector, true, false);
		this.loadScheduleReplaceCheckbox = new ImportOptionsCheckbox(page, "loadScheduleReplaceCheckbox");
		this.loadScheduleRepeatTimes = new ValueInputField<>(page, "loadScheduleRepeatTimes", Integer.class);
		loadScheduleRepeatTimes.setDefaultNumericalValue(1);
		loadScheduleRepeatTimes.setDefaultLowerBound(1);
		
//		this.loadScheduleReplaceCheckbox = new Checkbox(page, "loadScheduleReplaceCheckbox");
//		loadScheduleReplaceCheckbox.setDefaultList(Collections.singletonMap("Replace existing values in time range?", false));
		
		this.loadScheduleDataSubmit = new Button(page, "loadScheduleDataSubmit", "Import data") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = loadScheduleDataSelector.getSelectedSchedule(req);
				if (rots == null)
					disable(req);
				else
					enable(req);
					
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = loadScheduleDataSelector.getSelectedSchedule(req);
				if (rots == null) {
					alert.showAlert("Data import failed. Select a time series first.", false, req);
					return;
				}
				ReadOnlyTimeSeries main = mainSelector.getSelectedItem(req);
				if (main != null && !(main instanceof TimeSeries)) {
					alert.showAlert("Cannot import data... selected schedule " + main + " is not writable", false, req);
					return;
				}
				TimeSeries mainTs = (TimeSeries) main;
				boolean newCreated = mainTs == null;
				StringBuilder msg = new StringBuilder();
				if (newCreated) {
					mainTs = new FloatTreeTimeSeries();
					mainSelector.selectItem(mainTs, req);
					msg.append("New memory schedule created. ");
				}
				final boolean replace = loadScheduleReplaceCheckbox.doReplaceExisting(req);
				final boolean startMover = loadScheduleReplaceCheckbox.moveStartHere(req);
				final boolean endMover = loadScheduleReplaceCheckbox.moveEndHere(req);
				final int repeat = loadScheduleRepeatTimes.getNumericalValue(req);
				final List<SampledValue> values = Utils.getValues(rots, am.getFrameworkTime(), startMover, endMover, repeat);
				if (replace) {
					if (rots.getNextValue(Long.MIN_VALUE) != null) { // not empty 
						long min = rots.getNextValue(Long.MIN_VALUE).getTimestamp();
						long max = rots.getPreviousValue(Long.MAX_VALUE).getTimestamp();
						mainTs.replaceValues(min, max, values);
						msg.append("Values replaced in interval [" + min + ", " + max + "]. ");
					}
					else {
						msg.append("Selected schedule did not contain any values. ");
					}
				}
				else {
					mainTs.addValues(values);
					msg.append("Values added");
				}
				alert.showAlert(msg.toString(), true, req);
			}
			
		};
		
		this.scheduleImportPopup = new Popup(page, "scheduleImportPopup",true);
		scheduleImportPopup.setDefaultTitle("Import time series data");
		scheduleImportPopup.setDefaultHeaderHTML("OGEMA time series");
		PageSnippet scheduleImportSnippet = new PageSnippet(page, "scheduleImportSnippet", true);
		scheduleImportSnippet.append(loadScheduleDataSelector, null).linebreak(null)
			.append(loadScheduleReplaceCheckbox, null)
			.linebreak(null).append("Repeat ", null).append(loadScheduleRepeatTimes, null).append(" times", null);
//		Flexbox scheduleImportRepeatFlex = new Flexbox(page, "scheduleImportRepeatFlex", true);
//		scheduleImportRepeatFlex.setDefaultJustifyContent(JustifyContent.FLEX_LEFT);
//		Label lab = new 
		
		
		scheduleImportPopup.setBody(scheduleImportSnippet, null);
		scheduleImportPopup.setFooter(loadScheduleDataSubmit, null);
		
		this.fileImportSupportedFilesLabel = new Label(page, "fileImportSupportedFilesLabel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final String value;
				LabelledItem generator = importSelector.getSelectedItem(req);
				if (!(generator instanceof FileBasedDataGenerator))
					value = "";
				else
					value = ((FileBasedDataGenerator) generator).supportedFileFormat();
				setText(value, req);
			}
			
		};
		this.fileImportOptionsLabel = new Label(page, "fileImportOptionsLabel") {
			

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final String value;
				LabelledItem generator = importSelector.getSelectedItem(req);
				if (!(generator instanceof FileBasedDataGenerator))
					value = "";
				else
					value = ((FileBasedDataGenerator) generator).optionDescription(req.getLocale());
				setText(value, req);
			}
			
		};
		this.fileImportDelimiterField = new TextField(page, "fileImportDelimiterField") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final String value;
				LabelledItem generator = importSelector.getSelectedItem(req);
				if (!(generator instanceof FileBasedDataGenerator))
					value = "";
				else
					value = ((FileBasedDataGenerator) generator).defaultOptionString();
				setValue(value, req);
			}
			
		};
		this.fileImportReplaceCheckbox = new ImportOptionsCheckbox(page, "fileImportReplaceCheckbox");
		this.fileImportRepeatTimes = new ValueInputField<>(page, "fileImportRepeatTimes", Integer.class);
		fileImportRepeatTimes.setDefaultNumericalValue(1);
		fileImportRepeatTimes.setDefaultLowerBound(1);
		
//		this.fileImportReplaceCheckbox = new Checkbox(page, "fileImportReplaceCheckbox");
//		fileImportReplaceCheckbox.setDefaultList(Collections.singletonMap("Replace existing values in time range?", false));
		this.fileImportUpload = new FileUpload(page, "fileImportUpload", am) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				long cnt = fileImportWaiter.getCount(req);
				if (cnt > 0) {
					alert.showAlert("Upload running", false, req);
					disable(req);
					return;
				}
				final LabelledItem generator = importSelector.getSelectedItem(req);
				final ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				if (!(generator instanceof FileBasedDataGenerator) || (!(rots instanceof TimeSeries))) {
					disable(req); // TODO check if this works and prevents the upload
					return;
				}
				final FileBasedDataGenerator fgen = (FileBasedDataGenerator) generator; 
				final String option = fileImportDelimiterField.getValue(req);
				final String issues = fgen.checkOptionsString(option, req.getLocale());
				if (issues != null) {
					alert.showAlert(issues, false, req);
					disable(req);
					return;
				}
				enable(req);
				final boolean replace =  fileImportReplaceCheckbox.doReplaceExisting(req);
				final boolean startMover =  fileImportReplaceCheckbox.moveStartHere(req);
				final boolean endMover = fileImportReplaceCheckbox.moveEndHere(req);
				final int repeat = fileImportRepeatTimes.getNumericalValue(req);
//				boolean replace = fileImportReplaceCheckbox.getCheckboxList(req).values().iterator().next();
				registerListener(new FileImportListener(fileImportWaiter), new FileImportContext(fgen, (TimeSeries) rots, replace, option, 
						startMover, endMover, am.getFrameworkTime(), repeat), req);
				fileImportWaiter.reset(1, req);
			}
			
		};
		
		fileImportWaiter = new FileUploadLatch(page, "fileImportLatch") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				try {
					boolean success = this.await(30, TimeUnit.SECONDS, req);
					if (!success) {
						alert.showAlert("File upload not completed yet", false, req);
						return;
					}
					String issues = getIssues(req);
					if (issues == null)
						alert.showAlert("Upload successful", true, req);
					else
						alert.showAlert(issues, false, req);
				} catch (InterruptedException ignore) {
				}
			}
			
		};
		
		this.loadFileDataSubmit = new Button(page, "loadFileDataSubmit","Upload");
		this.fileImportDescription = new Label(page, "fileImportDescription") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				LabelledItem generator = importSelector.getSelectedItem(req);
				if (!(generator instanceof FileBasedDataGenerator)) {
					setText("", req);
					return;
				}
				final FileBasedDataGenerator fgen = (FileBasedDataGenerator) generator;
				setText(fgen.description(req.getLocale()), req);
			}
			
		};
		
		this.fileImportPopup = new Popup(page, "fileImportPopup", true);
		fileImportPopup.setDefaultTitle("Import data from file");
		fileImportPopup.setHeader(fileImportDescription, null);
		fileImportPopup.setDefaultHeaderHTML("Timeseries");
		PageSnippet fileImportSnippet = new PageSnippet(page, "fileImportSnippet", true);
		StaticTable fileImportTable = new StaticTable(3, 2);
		fileImportTable.setContent(0, 0, "Supported files").setContent(0, 1, fileImportSupportedFilesLabel)
			.setContent(1, 0, fileImportOptionsLabel).setContent(1, 1, fileImportDelimiterField)
			.setContent(2, 0, "Select a file").setContent(2, 1, fileImportUpload);
		fileImportSnippet.append(fileImportTable, null).linebreak(null).append(fileImportReplaceCheckbox, null)
			.linebreak(null).append("Repeat ", null).append(fileImportRepeatTimes, null).append(" times", null);
		fileImportPopup.setBody(fileImportSnippet, null);
		fileImportPopup.setFooter(loadFileDataSubmit, null);
		
		// plot data
		
		this.plotStartTime = new Datepicker(page, "plotStartTime") {

			private static final long serialVersionUID = 1L;

			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				Long start = null;
				if (rots != null) {
					SampledValue sv = rots.getNextValue(Long.MIN_VALUE);
					if (sv != null)
						start = sv.getTimestamp();
				}
				if (start == null)
					start = am.getFrameworkTime();
				setDate(start, req);
			}
			
		};
		this.plotEndTime = new Datepicker(page, "plotEndTime") {

			private static final long serialVersionUID = 1L;

			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				Long end = null;
				if (rots != null) {
					SampledValue sv = rots.getPreviousValue(Long.MAX_VALUE);
					if (sv != null)
						end = sv.getTimestamp();
				}
				if (end == null)
					end = am.getFrameworkTime();
				setDate(end, req);
			}
			
		};
		this.plotInterpolationMode = new TemplateDropdown<PlotType>(page, "plotInterpolationMode") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				if (rots == null)
					selectItem(PlotType.POINTS, req);
				else
					selectItem(mapMode(rots.getInterpolationMode()), req);
			}
			
		};
		plotInterpolationMode.setTemplate(new DisplayTemplate<PlotType>() {

			@Override
			public String getId(PlotType type) {
				return type.getId();
			}

			@Override
			public String getLabel(PlotType type, OgemaLocale locale) {
				return type.toString();
			}
			
		});
		plotInterpolationMode.setDefaultItems(Arrays.asList(PlotType.LINE_WITH_POINTS, PlotType.LINE, PlotType.STEPS, PlotType.POINTS, PlotType.BAR));
		this.plot = new SchedulePlotFlot(page, "plot", false, 24*60*60*1000L) {

			private static final long serialVersionUID = 1L;
			
			// should only be triggered by special button
			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				ScheduleDataFlot data = getScheduleData(req);
				if (rots == null)
					data.setSchedules(Collections.emptyMap());
				else {
					PlotType type = plotInterpolationMode.getSelectedItem(req);
					String label = getLabel(rots);
					SchedulePresentationData presiData = new DefaultSchedulePresentationData(rots, Float.class, label);
					data.setSchedules(Collections.singletonMap(label, presiData));
					data.setStartTime(plotStartTime.getDateLong(req));
					data.setEndTime(plotEndTime.getDateLong(req));
					getConfiguration(req).enableOverviewPlot(true);
					getConfiguration(req).setPlotType(type);
					
				}
			}
			
		};
		this.plotTrigger = new Button(page, "plotTrigger","Show plot",true);
		
// data manipulation
		
		this.scheduleManipulator = new ScheduleManipulator(page, "scheduleManipulator") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				ReadOnlyTimeSeries schedule = mainSelector.getSelectedItem(req);
				if (!(schedule instanceof TimeSeries))  // 
					setSchedule(null, req);
				else 
					setSchedule((TimeSeries) schedule,req);
			}
			
		};
		
		this.dataDeletion = new DataDeletor(page, "dataDeletion", mainSelector, alert);
		mainSelector.addTimeSeriesDependentWidget(dataDeletion);
// persistence
		
		this.persistenceSelector = new TemplateDropdown<TimeSeriesPersistence>(page, "persistenceSelector") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				List<TimeSeriesPersistence> targets = new ArrayList<>();
				targets.addAll(dataSources.getTargets());
				targets.addAll(fileSources.getTargets());
				update(targets, req);
			}
			
		};
		persistenceSelector.setTemplate((DisplayTemplate) DataGenerator.TEMPLATE);
		this.ogemaPersistenceSelector = new DataSourceWidget(page, "ogemaPersistenceSelector", dataSources, 
					dataSources2, (TemplateDropdown) persistenceSelector, true, true);
//		this.ogemaPersistenceReplaceCheckbox = new ImportOptionsCheckbox(page, "ogemaPersistenceReplaceCheckbox");
		this.ogemaPersistenceReplaceCheckbox = new Checkbox(page, "ogemaPersistenceReplaceCheckbox");
		ogemaPersistenceReplaceCheckbox.setDefaultList(Collections.singletonMap("Replace existing values in time range?", false));
		this.ogemaPersistenceSave = new Button(page, "ogemaPersistenceSave") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				TimeSeriesPersistence p = persistenceSelector.getSelectedItem(req);
				if (!(p instanceof OgemaTimeSeriesPersistence))
					return;
				ReadOnlyTimeSeries dat = mainSelector.getSelectedItem(req);
				if (dat == null)
					return;
				ReadOnlyTimeSeries target = ogemaPersistenceSelector.scheduleSelector.getSelectedSchedule(req);
				if (target == null)
					return;
				boolean replace = ogemaPersistenceReplaceCheckbox.getCheckboxList(req).values().iterator().next();
				try {
					((OgemaTimeSeriesPersistence) p).store(dat, target, replace);
					alert.showAlert("Saved", true, req);
				} catch (IllegalArgumentException | IOException e) {
					alert.showAlert("Saving data failed", false, req);
				}
			}
			
		};
		this.ogemaPersistencePopup = new Popup(page, "ogemaPersistencePopup", true);
		this.openOgemaPersistencePopup = new Button(page, "openOgemaPersistencePopup", "Save as OGEMA time series") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				TimeSeriesPersistence persistence = persistenceSelector.getSelectedItem(req);
				if (!(persistence instanceof OgemaTimeSeriesPersistence)) {
					disable(req);
					setWidgetVisibility(false, req);
					return;
				}
				enable(req);
				setWidgetVisibility(true, req);
//				OgemaTimeSeriesPersistence<?, ?> p = (OgemaTimeSeriesPersistence<?, ?>) persistence;
			}
			
		};
		this.saveTriggers = new PageSnippet(page, "saveTriggers", true);
		
		this.openFilePersistencePopup = new Button(page, "openFilePersistencePopup", "Export to file") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				TimeSeriesPersistence persistence = persistenceSelector.getSelectedItem(req);
				if (!(persistence instanceof FileBasedPersistence)) {
					disable(req);
					setWidgetVisibility(false, req);
					return;
				}
				enable(req);
				setWidgetVisibility(true, req);
			}
			
		};
		
		saveTriggers.append(openOgemaPersistencePopup, null).append(openFilePersistencePopup, null);
		
		PageSnippet ogemaPersistenceSnippet = new PageSnippet(page, "ogemaPersistenceSnippet", true);
		StaticTable persistenceTable = new StaticTable(2, 2);
		persistenceTable.setContent(0,0, "Options").setContent(0, 1, ogemaPersistenceSelector.highLevelOptions)
			.setContent(1, 0, "Time series").setContent(1, 1, ogemaPersistenceSelector.scheduleSelector);
		ogemaPersistenceSnippet.append(persistenceTable, null).linebreak(null).append(ogemaPersistenceReplaceCheckbox, null);
		ogemaPersistencePopup.setDefaultTitle("Store time series data");
		ogemaPersistencePopup.setDefaultHeaderHTML("Store in OGEMA time series");
		ogemaPersistencePopup.setBody(ogemaPersistenceSnippet, null);
		ogemaPersistencePopup.setFooter(ogemaPersistenceSave, null);
		
		// file based persistence
		this.filePersistenceDelimiterField  = new TextField(page, "filePersistenceDelimiterField") {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				TimeSeriesPersistence persistence = persistenceSelector.getSelectedItem(req);
				if (!(persistence instanceof FileBasedPersistence)) {
					return;
				}
				setValue(((FileBasedPersistence) persistence).defaultOptionString(),req);
			}
		};
		final File tempFolder = FrameworkUtil.getBundle(getClass()).getBundleContext().getDataFile("temp");
		if (tempFolder.exists() && tempFolder.isDirectory()) {
			try {
				FileUtils.deleteDirectory(tempFolder);
			} catch (IOException ignore) {
			}
		}
		this.filePersistenceDownload = new FileDownload(page, "filePersistenceDownload", am.getWebAccessManager());
		
		
		this.generateDownloadFile = new Button(page, "generateDownloadFile", "Generate") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ReadOnlyTimeSeries rots = mainSelector.getSelectedItem(req);
				if (rots == null) {
					return;
				}
				TimeSeriesPersistence persistence = persistenceSelector.getSelectedItem(req);
				if (!(persistence instanceof FileBasedPersistence)) {
					return;
				}
				FileBasedPersistence fp = (FileBasedPersistence) persistence;
				String options = filePersistenceDelimiterField.getValue(req);
				String issues = fp.checkOptionsString(options, req.getLocale());
				if (issues != null) {
					alert.showAlert(issues, false, req);
					return;
				}
				try {
					if (!tempFolder.exists())
						tempFolder.mkdirs();
					String prefix = getFileNamePrefix(rots);
					String flEnding = fp.getFileEnding(options);
					File fl = File.createTempFile(prefix, "." + flEnding, tempFolder);
					try (Writer writer = new FileWriter(fl)) {
						// XXX Float.class
						fp.generate(rots, options, Float.class, writer);
					}
					filePersistenceDownload.setFile(fl, req);
					alert.showAlert("Saved", true, req);
				} catch (IllegalArgumentException | IOException e) {
					alert.showAlert("Saving failed " + e, false, req);
				}
			}
			
		};
		this.filePersistentOptionLabel = new Label(page, "filePersistentOptionLabel") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				TimeSeriesPersistence persistence = persistenceSelector.getSelectedItem(req);
				if (!(persistence instanceof FileBasedPersistence)) {
					return;
				}
				FileBasedPersistence fp = (FileBasedPersistence) persistence;
				setText(fp.optionDescription(req.getLocale()), req);
			}
			
		};
		this.filePersistenceDownloadStart = new Button(page, "filePersistenceDownloadStart", "Download") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				// FIXME 
//				filePersistenceDownload.getFile(req)  missing!
			}
			
		};

		this.filePersistencePopup = new Popup(page, "filePersistencePopup", true); 
		PageSnippet filePersistenceSnippet = new PageSnippet(page, "filePersistenceSnippet", true);
		StaticTable filePersistenceTable = new StaticTable(4, 2);
		filePersistenceTable.setContent(0, 0, filePersistentOptionLabel).setContent(0, 1, filePersistenceDelimiterField)
							.setContent(1, 0, "Generate file").setContent(1, 1, generateDownloadFile)
							.setContent(2, 0, "Start download").setContent(2, 1, filePersistenceDownloadStart)
												.setContent(3, 1, filePersistenceDownload);
		
		filePersistenceSnippet.append(filePersistenceTable, null);
		filePersistencePopup.setDefaultTitle("Export date");
		filePersistencePopup.setDefaultHeaderHTML("Store timeseries in file");
		filePersistencePopup.setBody(filePersistenceSnippet, null);
		
		
		buildPage();
		setDependencies();
	}

	private final void buildPage() {
		StaticTable table = new StaticTable(4, 2, new int[]{2,3});
		table.setContent(0, 0, "Active schedule").setContent(0, 1, mainSelector)
			.setContent(1, 0, "First timestamp").setContent(1, 1, firstTimestamp)
			.setContent(2, 0, "Last timestamp").setContent(2, 1, lastTimestamp)
			.setContent(3, 0, nrPointsTrigger).setContent(3, 1, nrPoints);
		
		page.append(header).linebreak().append(alert).linebreak().append(table);
		
		// accordion
			// select schedule
		PageSnippet snippet = new PageSnippet(page, "selectScheduleSnippetTab", true);
		table = new StaticTable(2, 2, new int[] {2,3});
		table								   .setContent(0, 1, selectExistingSchedule)
											   .setContent(1, 1, copyExistingSchedule);
		snippet.append(scheduleSelector, null).linebreak(null).append(table,null);
		accordion.addItem("Select existing schedule", snippet,null);
		
			// create schedule
		snippet = new PageSnippet(page, "createScheduleSnippetTab", true);
		table = new StaticTable(2, 2, new int[]{2,3});
		table.setContent(0, 0, "Select schedule type").setContent(0,1, createScheduleSelector)
				.setContent(1, 1, creatorTriggers);
		snippet.append(table, null);
		accordion.addItem("Create new schedule", snippet, null);

			// load data
		snippet = new PageSnippet(page, "importScheduleSnippetTab", true);
		table = new StaticTable(2, 2, new int[]{2,3});
		table.setContent(0, 0, "Select data source").setContent(0,1, importSelector)
				.setContent(1, 1, importTriggers);
		snippet.append(table, null).linebreak(null).append(loadScheduleDataSelector.selectionTree, null);
		accordion.addItem("Import data", snippet, null);
		
			// plot data
		snippet = new PageSnippet(page, "plotSnippetTab", true);
		table = new StaticTable(4, 2, new int[]{2,3});
		table.setContent(0, 0, "Select start time").setContent(0,1, plotStartTime)
				.setContent(1, 0, "Select end time").setContent(1,1, plotEndTime)
				.setContent(2, 0, "Select interpolation mode").setContent(2,1, plotInterpolationMode)
													.setContent(3, 1, plotTrigger);
		snippet.append(table, null).linebreak(null).append(plot, null);
		accordion.addItem("Plot data", snippet, null);
		
			// manipulate data
		snippet = new PageSnippet(page, "manipulateSnippetTab", true);
		snippet.append(scheduleManipulator, null).linebreak(null).append(dataDeletion, null);
		accordion.addItem("Manipulate data", snippet, null);
		
			// save data
		snippet = new PageSnippet(page, "persistenceSnippetTab", true);
		table = new StaticTable(3, 2, new int[]{3,2});
		table.setContent(0, 0, "Select persistence target").setContent(0, 1, persistenceSelector)
														.setContent(1, 0, saveTriggers);
		snippet.append(table, null);
		accordion.addItem("Save schedule", snippet, null);
		
		page.append(accordion).linebreak();
		page.append(scheduleCreationPopup).linebreak().append(slotsCreationPopup).linebreak().append(scheduleImportPopup).linebreak()
			.append(fileImportPopup).linebreak().append(fileImportWaiter).linebreak().append(ogemaPersistencePopup)
			.linebreak().append(filePersistencePopup);
	}
	
	private final void setDependencies() {
		mainSelector.triggerAction(firstTimestamp, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		mainSelector.triggerAction(lastTimestamp, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		nrPointsTrigger.triggerAction(nrPoints, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		// schedule selection
		scheduleSelector.sourceTypeDropdown.triggerAction(selectExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,2);
		scheduleSelector.sourceTypeDropdown.triggerAction(copyExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,2);
		// TODO check if this works...
		scheduleSelector.selectionTree.triggerAction(selectExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		scheduleSelector.selectionTree.triggerAction(copyExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		
		scheduleSelector.highLevelOptions.triggerAction(selectExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		scheduleSelector.highLevelOptions.triggerAction(copyExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		scheduleSelector.scheduleSelector.triggerAction(selectExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		scheduleSelector.scheduleSelector.triggerAction(copyExistingSchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectExistingSchedule.triggerAction(mainSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		copyExistingSchedule.triggerAction(mainSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		// timeseries creation
		createScheduleSelector.triggerAction(createMemorySchedule, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		createScheduleSelector.triggerAction(openScheduleCreationPopup, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		createScheduleSelector.triggerAction(openSlotsCreationButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		createMemorySchedule.triggerAction(mainSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		// new schedule
		openScheduleCreationPopup.triggerAction(scheduleCreationPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET); 
		openScheduleCreationPopup.triggerAction(parentTypeSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST); // select empty option
		openScheduleCreationPopup.triggerAction(parentResourceSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		parentTypeSelector.triggerAction(parentResourceSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		scheduleCreationSubmit.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		scheduleCreationSubmit.triggerAction(mainSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		scheduleCreationSubmit.triggerAction(scheduleCreationPopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET); 
		// new slots
		openSlotsCreationButton.triggerAction(slotsCreationPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		openSlotsCreationButton.triggerAction(slotsSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		slotsCreationSubmit.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		slotsCreationSubmit.triggerAction(mainSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		slotsCreationSubmit.triggerAction(slotsCreationPopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET); 
		// load data from schedule
		importSelector.triggerAction(openScheduleImportButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openScheduleImportButton.triggerAction(scheduleImportPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		// TODO add unselected option to loadScheduleDataSelector options
		openScheduleImportButton.triggerAction(loadScheduleDataSelector.sourceTypeDropdown, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openScheduleImportButton.triggerAction(loadScheduleDataSelector.highLevelOptions, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		openScheduleImportButton.triggerAction(loadScheduleDataSelector.scheduleSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,2);
		
		loadScheduleDataSelector.selectionTree.triggerAction(loadScheduleDataSubmit, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		loadScheduleDataSelector.scheduleSelector.triggerAction(loadScheduleDataSubmit, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		loadScheduleDataSubmit.triggerAction(mainSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST); // reload everything
		loadScheduleDataSubmit.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		loadScheduleDataSubmit.triggerAction(scheduleImportPopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		// load data from file
		importSelector.triggerAction(openFileImportButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openFileImportButton.triggerAction(fileImportOptionsLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openFileImportButton.triggerAction(fileImportSupportedFilesLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openFileImportButton.triggerAction(fileImportDescription, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openFileImportButton.triggerAction(fileImportDelimiterField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openFileImportButton.triggerAction(fileImportPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		loadFileDataSubmit.triggerAction(fileImportUpload, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);
		loadFileDataSubmit.triggerAction(fileImportPopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		loadFileDataSubmit.triggerAction(mainSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST); // reload everything
		fileImportUpload.triggerAction(fileImportWaiter, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);
		fileImportWaiter.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		fileImportWaiter.triggerAction(firstTimestamp, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		fileImportWaiter.triggerAction(lastTimestamp, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		// plot data
		mainSelector.triggerAction(plotStartTime, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		mainSelector.triggerAction(plotEndTime, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		mainSelector.triggerAction(plotInterpolationMode, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		plotTrigger.triggerAction(plot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		// manipulate data
		mainSelector.triggerAction(scheduleManipulator, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		// save data
		persistenceSelector.triggerAction(openOgemaPersistencePopup, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		persistenceSelector.triggerAction(openFilePersistencePopup, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		 	// ogema persistence
		openOgemaPersistencePopup.triggerAction(ogemaPersistenceSelector.sourceTypeDropdown, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openOgemaPersistencePopup.triggerAction(ogemaPersistenceSelector.highLevelOptions, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openOgemaPersistencePopup.triggerAction(ogemaPersistenceSelector.scheduleSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openOgemaPersistencePopup.triggerAction(ogemaPersistencePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		ogemaPersistenceSave.triggerAction(ogemaPersistencePopup, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		ogemaPersistenceSave.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			// file persistence		
		openFilePersistencePopup.triggerAction(filePersistenceDelimiterField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openFilePersistencePopup.triggerAction(filePersistentOptionLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		generateDownloadFile.triggerAction(filePersistenceDownload, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		openFilePersistencePopup.triggerAction(filePersistencePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		filePersistenceDownloadStart.triggerAction(filePersistenceDownload, TriggeringAction.POST_REQUEST, FileDownloadData.STARTDOWNLOAD);
		filePersistenceDownloadStart.triggerAction(filePersistencePopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);

	}
	
	private static final PlotType mapMode(InterpolationMode mode) {
		switch (mode) {
		case LINEAR:
			return PlotType.LINE_WITH_POINTS;
		case NEAREST:
		case STEPS:
			return PlotType.STEPS;
		case NONE:
		default: // null
			return PlotType.POINTS;
		}
	}
	
	private static class TimeSeriesSelector extends Label implements SelectorTemplate<ReadOnlyTimeSeries> {

		private static final long serialVersionUID = 1L;
		private final List<TimeSeriesDependentWidget> dependentWidgets = new ArrayList<>(); 

		public TimeSeriesSelector(WidgetPage<?> page, String id) {
			super(page, id);
		}
		
		@Override
		public TimeSeriesSelectorData createNewSession() {
			return new TimeSeriesSelectorData(this);
		}
		
		@Override
		public TimeSeriesSelectorData getData(OgemaHttpRequest req) {
			return (TimeSeriesSelectorData) super.getData(req);
		}
		
		public ReadOnlyTimeSeries getSelectedItem(OgemaHttpRequest req) {
			return getData(req).schedule;
		}
		
		public void selectItem(ReadOnlyTimeSeries schedule, OgemaHttpRequest req) {
			getData(req).schedule = schedule;
		}

		@Override
		public void selectDefaultItem(ReadOnlyTimeSeries arg0) {
			throw new UnsupportedOperationException();
		}
		
		void updateDependentWidgets(ReadOnlyTimeSeries schedule, OgemaHttpRequest req) {
			synchronized (dependentWidgets) {
				for (TimeSeriesDependentWidget w: dependentWidgets) {
					try {
						w.update(schedule, req);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		void addTimeSeriesDependentWidget(TimeSeriesDependentWidget widget) {
			synchronized (dependentWidgets) {
				dependentWidgets.add(widget);
			}
		}
		
	}
	
	private static class TimeSeriesSelectorData extends LabelData {
		
		private ReadOnlyTimeSeries schedule;

		public TimeSeriesSelectorData(TimeSeriesSelector label) {
			super(label);
		}
		
		@Override
		public JSONObject retrieveGETData(OgemaHttpRequest req) {
			setText(getLabel(schedule));
			// we need this for some widgets which cannot determine their state themselves
			((TimeSeriesSelector) widget).updateDependentWidgets(schedule, req);
			return super.retrieveGETData(req);
		}
		
	}
	
	static interface TimeSeriesDependentWidget {
		
		void update(ReadOnlyTimeSeries newSchedule, OgemaHttpRequest req);
		
	}
	
	private static String getLabel(ReadOnlyTimeSeries schedule) {
		if (schedule == null)
			return "";
		if (schedule instanceof Schedule)
			return "Schedule: " + schedule;
		if (schedule instanceof RecordedData)
			return "Recorded data (SlotsDb): " + ((RecordedData) schedule).getPath();
		if (schedule instanceof OnlineTimeSeries)
			return "Online time series: " + ((OnlineTimeSeries) schedule).getResource().getPath();
		if (schedule instanceof MemoryTimeSeries)
			return "Memory schedule";
		return "Unknown time series type";
	}
	
	private static String getFileNamePrefix(ReadOnlyTimeSeries schedule) {
		if (schedule instanceof Schedule)
			return ((Schedule) schedule).getPath().replace('/', '_');
		if (schedule instanceof RecordedData)
			return ((RecordedData) schedule).getPath();
		return "memorySchedule";
	}
	
	private static class FileImportContext {
		
		private final FileBasedDataGenerator generator;
		private final TimeSeries timeSeries;
		private final String option;
		private final boolean replaceValues;
		private final boolean moveStart;
		private final boolean moveEnd;
		private final long now;
		private final int repeat;
		
		public FileImportContext(FileBasedDataGenerator generator, TimeSeries timeSeries, boolean replaceValues, String option, 
				boolean moveStart, boolean moveEnd, long now, int repeat) {
			Objects.requireNonNull(generator);
			Objects.requireNonNull(timeSeries);
			this.generator = generator;
			this.timeSeries = timeSeries;
			this.replaceValues = replaceValues; 
			this.option = (option != null ? option.trim() : option);
			this.moveEnd = moveEnd;
			this.moveStart = moveStart;
			this.now = now;
			this.repeat = repeat;
		}
		
	}
	
	private static class FileUploadLatch extends LatchWidget {

		private static final long serialVersionUID = 1L;

		public FileUploadLatch(WidgetPage<?> page, String id) {
			super(page, id);
		}

		@Override
		public FileUploadLatchData createNewSession() {
			return new FileUploadLatchData(this);
		}
		
		@Override
		public FileUploadLatchData getData(OgemaHttpRequest req) {
			return (FileUploadLatchData) super.getData(req);
		}
		
		public void setIssues(String issues, OgemaHttpRequest req) {
			getData(req).issues = issues;
		}
		
		public String getIssues(OgemaHttpRequest req) {
			return getData(req).issues;
		}
		
	}
	
	private static class FileUploadLatchData extends LatchWidgetData {
		
		private String issues = null;

		public FileUploadLatchData(LatchWidget empty) {
			super(empty);
		}
		
	}
	
	private static class FileImportListener implements FileUploadListener<FileImportContext> {

		private final FileUploadLatch latch;
		
		public FileImportListener(FileUploadLatch latch) {
			this.latch = latch;
		}

		@Override
		public void fileUploaded(FileItem fileItem, FileImportContext context, OgemaHttpRequest req) {
			try {
				List<SampledValue> values = context.generator.parseFile(fileItem, Float.class, context.option);
				if (context.moveStart || context.moveEnd || context.repeat != 1)
					values = Utils.getValues(values, context.now, context.moveStart, context.moveEnd, context.repeat);
				if (context.replaceValues) {
					if (!values.isEmpty())
						context.timeSeries.replaceValues(values.get(0).getTimestamp(), values.get(values.size()-1).getTimestamp(), values);
				}
				else 
					context.timeSeries.addValues(values);
				latch.setIssues(null, req);
			} catch (Exception e) {
				latch.setIssues("Upload failed: " + e, req);
			} finally {
				latch.countDown(req);
			}
			
		}
		
	}
	
}

