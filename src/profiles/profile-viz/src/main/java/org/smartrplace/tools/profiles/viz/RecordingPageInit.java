package org.smartrplace.tools.profiles.viz;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.actors.OnOffSwitch;
import org.osgi.service.component.ComponentServiceObjects;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.Profile;
import org.smartrplace.tools.profiles.ProfileGeneration;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;
import org.smartrplace.tools.profiles.prefs.ProfileData;
import org.smartrplace.tools.profiles.prefs.ProfilePreferences;
import org.smartrplace.tools.profiles.prefs.StateDuration;
import org.smartrplace.tools.profiles.utils.StandardDataPoints;
import org.smartrplace.tools.profiles.utils.StateImpl;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.EnumDropdown;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.form.textfield.ValueInputField;
import de.iwes.widgets.html.html5.AbstractGrid;
import de.iwes.widgets.html.html5.SimpleGrid;
import de.iwes.widgets.html.html5.TemplateGrid;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.template.DisplayTemplate;
import de.iwes.widgets.template.LabelledItem;

// TODO: duration grid ordering not working
class RecordingPageInit {

	private final WidgetPage<?> page;
	private final Header header;
	private final TaskAlert alert;
	private final TemplateDropdown<ComponentServiceObjects<ProfileTemplate>> templateSelector;
	private final TemplateDropdown<String> preferencesSelector;
	private final TextField storeConfigAsPreferenceId;
	private final Button storeConfigAsPreference;
	private final TemplateDropdown<State> endStateSelector;
	private final TemplateDropdown<Resource> switchSelector;
	private final Header primaryHeader;
	private final TemplateGrid<DataPoint> primaryPointsGrid; 
	private final Header contextHeader;
	private final TemplateGrid<DataPoint> contextPointsGrid;
	private final Header durationsHeader;
	private final TemplateGrid<OrderedState> durations;
	private final Button start;
	private final ButtonConfirm cancel;
	
	@SuppressWarnings("serial")
	public RecordingPageInit(final WidgetPage<?> page, final ApplicationManager appMan, 
			final ComponentServiceObjects<ProfileGeneration> generator, 
			final ConcurrentMap<String, ComponentServiceObjects<ProfileTemplate>> templates, 
			final ComponentServiceObjects<ProfilePreferences> preferences) {
		this.page = page;
		page.setTitle("Profile recording");
		this.header = new Header(page, "header", "Profile generation");
		header.setDefaultColor("blue");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		this.primaryHeader = new Header(page, "primaryHeader", "Select primary data");
		primaryHeader.setDefaultHeaderType(3);
		primaryHeader.setDefaultColor("blue");
		primaryHeader.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		this.contextHeader = new Header(page, "contextHeader", "Select context data");
		contextHeader.setDefaultHeaderType(3);
		contextHeader.setDefaultColor("blue");
		contextHeader.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		this.durationsHeader = new Header(page, "durationsHeader", "Select state durations");
		durationsHeader.setDefaultHeaderType(3);
		durationsHeader.setDefaultColor("blue");
		durationsHeader.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		alert = new TaskAlert(page, "alert", "");
		alert.setDefaultVisibility(false);
		this.templateSelector = new TemplateDropdown<ComponentServiceObjects<ProfileTemplate>>(page, "templateSelector") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				update(templates.values(), req);
			}
			
		};
		templateSelector.setTemplate(new DisplayTemplate<ComponentServiceObjects<ProfileTemplate>>() {
			
			@Override
			public String getLabel(ComponentServiceObjects<ProfileTemplate> service, OgemaLocale locale) {
				return propertyOrValue(ProfileTemplate.LABEL_PROPERTY, template -> template.label(locale), service);
			}
			
			@Override
			public String getId(ComponentServiceObjects<ProfileTemplate> service) {
				return propertyOrValue(ProfileTemplate.ID_PROPERTY, LabelledItem::id, service);
			}
		});
		this.preferencesSelector = new TemplateDropdown<String>(page, "preferencesSelector") {
			
			public void onGET(OgemaHttpRequest req) {
				final ComponentServiceObjects<ProfileTemplate> template = templateSelector.getSelectedItem(req);
				if (template == null) {
					update(Collections.emptyList(), req);
					return;
				}
				final String templateId = propertyOrValue(ProfileTemplate.ID_PROPERTY, LabelledItem::id, template);
				final ProfilePreferences prefs = preferences.getService();
				try {
					final Future<Collection<String>> future = prefs.getProfileIds(templateId);
					final Collection<String> result = future.get(10, TimeUnit.SECONDS);
					update(result, req);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (Exception e) {
					update(Collections.emptyList(), req);
					appMan.getLogger().error("Failed to update preference selector",e);
				} finally {
					preferences.ungetService(prefs);
				}
			}
			
			public void onPOSTComplete(String id, OgemaHttpRequest req) {
				final String selected = getSelectedItem(req);
				if (selected == null)
					return;
				final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
				if (service == null) 
					return;
				final ProfileTemplate template = service.getService();
				try {
					final ProfilePreferences prefs = preferences.getService();
					try {
						final Future<ProfileData> future = prefs.loadProfileConfiguration(template, selected);
						final ProfileData pd = future.get(30, TimeUnit.SECONDS);
						final Map<DataPoint, Resource> data = pd.getDataPoints();
						selectData(data, primaryPointsGrid, req);
						selectData(data, contextPointsGrid, req);
						final OnOffSwitch oo = pd.getOnOffSwitch();
						if (oo != null)
							switchSelector.selectItem(oo.getLocationResource(), req);
						final State endState = pd.getEndState();
						if (endState != null)
							endStateSelector.selectItem(endState, req);
						final List<StateDuration> durations = pd.getDurations();
						if (durations != null)
							setDurations(RecordingPageInit.this.durations, durations, req);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					} catch (Exception e) {
						alert.showAlert("Failed to load configuration: " + e, false, req);
						appMan.getLogger().warn("Error",e);
					} finally {
						preferences.ungetService(prefs);
					}
				} finally {
					service.ungetService(template);
				}
			}
			
		};
		preferencesSelector.setDefaultAddEmptyOption(true);
		preferencesSelector.selectDefaultItem(null);
		this.storeConfigAsPreferenceId = new TextField(page, "storeConfigAsPreferenceId");
		this.storeConfigAsPreference = new Button(page, "storeConfigAsPref", "Store configuration") {
			
			public void onGET(OgemaHttpRequest req) {
				final String id = storeConfigAsPreferenceId.getValue(req);
				if (id == null || id.trim().isEmpty())
					disable(req);
				else 
					enable(req);
			}
			
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final String id = storeConfigAsPreferenceId.getValue(req);
				if (id == null || id.trim().isEmpty()) {
					alert.showAlert("Please enter a configuration id", false, req);
					return;
				}
				final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
				if (service == null) 
					return;
				final ProfileTemplate template = service.getService();
				try {
					final Map<DataPoint, Resource> resourceSettings = new HashMap<>();
					getInputData(resourceSettings, contextPointsGrid, template.contextData(), req);
					getInputData(resourceSettings, primaryPointsGrid, template.primaryData(), req);
					final OnOffSwitch swtch = (OnOffSwitch) switchSelector.getSelectedItem(req);
					final State endState = endStateSelector.getSelectedItem(req);
					final ProfilePreferences prefs = preferences.getService();
					try {
						prefs.storeProfileConfiguration(template, id, resourceSettings, 
								getDurationObjects(durations, template.states(), req), endState, swtch);
						alert.showAlert("Configuration stored: " + id, true, req);
					} finally {
						preferences.ungetService(prefs);
					}
				} finally {
					service.ungetService(template);
				}
			}
			
		};
		this.switchSelector = new ResourceDropdown<Resource>(page, "switchSelector") {
			
			public void onGET(OgemaHttpRequest req) {
				final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
				if (service == null) {
					update(Collections.emptyList(), req);
					return;
				}
				final ProfileTemplate template = service.getService();
				try {
					final List<State> states = template.states();
					if (states.size() <= 1) {
						update(Collections.emptyList(), req);
						return;
					}
					// TODO
//					if (states.size() == 2) {
						update(appMan.getResourceAccess().getResources(OnOffSwitch.class), req);
//					} else {
						// ??
						
//					}
				} finally {
					service.ungetService(template);
				}
				
			}
			
		};
		switchSelector.setDefaultAddEmptyOption(true);
		switchSelector.selectDefaultItem(null);
		this.endStateSelector = new TemplateDropdown<State>(page, "endStateSelector") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
				if (service == null) {
					update(Collections.emptyList(), req);
					return;
				}
				final ProfileTemplate template = service.getService();
				try {
					final List<State> states = template.states();
					update(states.size() <= 1 ? Collections.emptyList() : states, req);
				} finally {
					service.ungetService(template);
				}
			}
			
		};
		endStateSelector.setDefaultAddEmptyOption(true);
		endStateSelector.selectDefaultItem(null);
		this.primaryPointsGrid = new DataGrid(page, "primaryPointsGrid", true, appMan, preferencesSelector);
		this.contextPointsGrid = new DataGrid(page, "contextPointsGrid", false, appMan, preferencesSelector, 
					Arrays.asList(StandardDataPoints.profileStartTime(true).id()));
		final RowTemplate<OrderedState> durationsTemplate = new RowTemplate<OrderedState>() {
			
			private final Map<String, Object> header;
			
			{
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("state", "State");
				header.put("duration", "Duration");
				header.put("unit", "Unit");
				this.header = Collections.unmodifiableMap(header);
			}

			@Override
			public Row addRow(final OrderedState state, final OgemaHttpRequest req) {
				final String id = getLineId(state);
				final Row row = new Row();
				row.addCell("state", state.label(req.getLocale()));
				final ValueInputField<Long> duration = new ValueInputField<>(durations, "durartion_" + id, Long.class, req);
				duration.setDefaultNumericalValue(5L);
				duration.setDefaultLowerBound(0);
				preferencesSelector.triggerAction(duration, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("duration", duration);
				final EnumDropdown<ChronoUnit> unit = new EnumDropdown<>(durations, "unit_" + id, req, ChronoUnit.class);
				unit.selectDefaultItem(ChronoUnit.MINUTES);
				preferencesSelector.triggerAction(unit, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("unit", unit);
				return row;
			}

			@Override
			public Map<String, Object> getHeader() {
				return header;
			}

			@Override
			public String getLineId(OrderedState state) {
				return "_" + state.getIdx() + "_" + state.getState().id();
			}
			
			
		};
		this.durations = new TemplateGrid<OrderedState>(page, "durations", false, durationsTemplate) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
				if (service == null) {
					update(Collections.emptyList(), req);
					return;
				}
				final ProfileTemplate profile = service.getService();
				try {
					final List<State> states = profile.states();
					final List<OrderedState> ordered = new ArrayList<>(states.size());
					for (int i=0; i<states.size(); i++) {
						ordered.add(new OrderedState(states.get(i), i));
					}
					update(ordered, req);
				} finally {
					service.ungetService(profile);
				}
			}
			
		};
		setGridStyle(durations);
		durations.setComparator(OrderedState::compare);
		this.start = new Button(page, "startRecording", "Start recording") {
			
			public void onGET(OgemaHttpRequest req) {
				final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
				if (service == null || alert.isTaskActive()) {
					disable(req);
					setPollingInterval(5000, req);
				}
				else {
					enable(req);
					setPollingInterval(-1, req);
				}
			}
			
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
				if (service == null) 
					return;
				final Resource swtch = switchSelector.getSelectedItem(req);
				final Consumer<State> switchFunction;
				if (swtch instanceof OnOffSwitch) {
					switchFunction = state -> ((OnOffSwitch) swtch).stateControl().setValue(!StateImpl.OFF.id().equals(state.id())); 
				} else {
					switchFunction = state -> {}; // ?
				}
				final ProfileTemplate template = service.getService();
				try {
					final Map<DataPoint, Resource> input = new HashMap<>();
					getInputData(input, contextPointsGrid, template.contextData(), req);
					getInputData(input, primaryPointsGrid, template.primaryData(), req);
					final RecordingTask task = new RecordingTask(generator, service, switchFunction, 
								input, getDurations(durations, template.states(), req), endStateSelector.getSelectedItem(req));
					final Future<Profile> future = Execs.getExecutor().submit(task);
					try { // useful to keep the template service around until the task starts, to avoid bouncing
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					alert.setText("Recording active for profile " + template.label(req.getLocale()), req);
					alert.setStyle(AlertData.BOOTSTRAP_INFO, req);
					alert.start(future);
					alert.setWidgetVisibility(true, req);
				} catch (Exception e) {
					alert.setText("Recording failed: " + e, req);
					alert.setStyle(AlertData.BOOTSTRAP_DANGER, req);
					alert.setWidgetVisibility(true, req);
					LoggerFactory.getLogger(getClass()).info("Recording failed",e);
				} finally {
					service.ungetService(template);
				}
			}
			
		};
		this.cancel = new ButtonConfirm(page, "cancelBtn", "Cancel") {
			
			public void onGET(OgemaHttpRequest req) {
				if (!alert.isTaskActive()) {
					disable(req);
					setPollingInterval(-1, req);
				}
				else {
					setPollingInterval(5000, req);
					enable(req);
				}
			}
			
			public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
				alert.cancel(req);
			}
			
		};
		cancel.setDefaultConfirmMsg("Do you really want to interrupt the recording?");
		cancel.setDefaultConfirmBtnMsg("Cancel");
		cancel.setDefaultConfirmPopupTitle("Confirm interruption");
		buildPage();
		setDependencies();
	}
	
	private final void buildPage() {
		final SimpleGrid selectorGrid = new SimpleGrid(page, "selectorGrid", true);
		setGridStyle(selectorGrid);
		selectorGrid.addItem("Start recording", false, null).addItem(start, false, null)
			.addItem("Cancel recording", true, null).addItem(cancel, false, null)
			.addItem("Select template", true, null).addItem(templateSelector, false, null)
			.addItem("Load configuration", true, null).addItem(preferencesSelector, false, null)
			.addItem("Store configuration", true, null).addItem(storeConfigAsPreferenceId, false, null)
			.addItem("", true, null).addItem(storeConfigAsPreference, false, null); // TODO in row above
		final SimpleGrid switchGrid = new SimpleGrid(page, "switchGRid", true);
		setGridStyle(switchGrid);
		switchGrid.addItem("Select end state", false, null).addItem(endStateSelector, false, null)
			.addItem("Select switch", true, null).addItem(switchSelector, false, null);
		page.append(header).linebreak().append(alert)
			.append(selectorGrid)
			.append(primaryHeader).linebreak()
			.append(primaryPointsGrid)
			.append(contextHeader).linebreak()
			.append(contextPointsGrid)
			.append(durationsHeader).linebreak()
			.append(durations).linebreak()
			.append(switchGrid);
	}
	
	private final void setDependencies() {
		templateSelector.triggerAction(primaryPointsGrid, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		templateSelector.triggerAction(contextPointsGrid, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		templateSelector.triggerAction(durations, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		templateSelector.triggerAction(endStateSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		templateSelector.triggerAction(preferencesSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		storeConfigAsPreferenceId.triggerAction(storeConfigAsPreference, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		storeConfigAsPreference.triggerAction(preferencesSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		storeConfigAsPreference.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		start.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		cancel.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		start.triggerAction(start, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		start.triggerAction(cancel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		cancel.triggerAction(start, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		cancel.triggerAction(cancel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		preferencesSelector.triggerAction(switchSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		preferencesSelector.triggerAction(endStateSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		primaryPointsGrid.triggerAction(preferencesSelector, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		contextPointsGrid.triggerAction(preferencesSelector, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		durations.triggerAction(preferencesSelector, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);

	}
	
	private final class DataTemplate extends RowTemplate<DataPoint> {
		
		private final boolean primary;
		private final ApplicationManager appMan;
		private final OgemaWidget preferenceSelector;
		private final Map<String, Object> header;
		
		public DataTemplate(boolean primary, ApplicationManager appMan, OgemaWidget preferenceSelector) {
			this.primary = primary;
			this.appMan = appMan;
			this.preferenceSelector = preferenceSelector;
		}
		
		{
			final Map<String, Object> headerLocal = new LinkedHashMap<>();
			headerLocal.put("dp", "Data point");
			headerLocal.put("select", "Select");
			header = Collections.unmodifiableMap(headerLocal);
		}

		@SuppressWarnings("serial")
		@Override
		public Row addRow(final DataPoint dataPoint, final OgemaHttpRequest req) {
			final String id = getLineId(dataPoint);
			final Row row = new Row();
			row.addCell("dp", dataPoint.label(req.getLocale()));
			final ResourceDropdown<?> resourceDrop = new ResourceDropdown<Resource>(primary ? primaryPointsGrid : contextPointsGrid, "select_" + id, req) {

				public void onGET(OgemaHttpRequest req) {
					Class<?> type = dataPoint.typeInfo();
					if (type != null && !Resource.class.isAssignableFrom(type)) {
						if (type == Float.class || type == float.class)
							type = FloatResource.class;
						else if (type == Integer.class )
							type = IntegerResource.class;
						else if (type == Boolean.class || type == boolean.class)
							type = BooleanResource.class;
						else if (type == Long.class || type == long.class)
							type = TimeResource.class;
						else if (type == String.class)
							type = StringResource.class;
						else 
							type = null;
					}
					@SuppressWarnings({ "unchecked", "rawtypes" })
					final List<Resource> resources = appMan.getResourceAccess().getResources((Class) type);
					update(resources, req);
				};
				
			};
			resourceDrop.setDefaultAddEmptyOption(true);
			preferenceSelector.triggerAction(resourceDrop, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			row.addCell("select", resourceDrop);
			return row;
		}

		@Override
		public Map<String, Object> getHeader() {
			return header;
		}

		@Override
		public String getLineId(DataPoint dataPoint) {
			return dataPoint.id();
		}
		
	}
	
	@SuppressWarnings("serial")
	private final class DataGrid extends TemplateGrid<DataPoint> {
		
		private final boolean primary;
		private final Collection<String> filteredIds;
		
		public DataGrid(WidgetPage<?> page, String id, boolean primary, ApplicationManager appMan, OgemaWidget preferenceSelector) {
			this(page, id, primary, appMan, preferenceSelector, null);
		}
		
		public DataGrid(WidgetPage<?> page, String id, boolean primary, ApplicationManager appMan, OgemaWidget preferenceSelector, Collection<String> filteredIds) {
			super(page, id, false, new DataTemplate(primary, appMan, preferenceSelector));
			this.primary = primary;
			RecordingPageInit.setGridStyle(this);
			this.filteredIds = filteredIds == null || filteredIds.isEmpty() ? null : new ArrayList<>(filteredIds);
		}


		public void onGET(OgemaHttpRequest req) {
			final ComponentServiceObjects<ProfileTemplate> service = templateSelector.getSelectedItem(req);
			if (service == null) {
				update(Collections.emptyList(), req);
				return;
			}
			final ProfileTemplate profile = service.getService();
			try {
				List<DataPoint> points = primary ? profile.primaryData() : profile.contextData();
				if (filteredIds != null) {
					points = new ArrayList<>(points);
					points.removeIf(dp -> filteredIds.contains(dp.id()));
				}
				update(points, req);
			} finally {
				service.ungetService(profile);
			}
		}
		
	};
	
	private static String propertyOrValue(final String propertyKey, final Function<ProfileTemplate, String> function,
			final ComponentServiceObjects<ProfileTemplate> service) {
		final Object value = service.getServiceReference().getProperty(propertyKey);
		if (value != null)
			return value.toString();
		final ProfileTemplate profile = service.getService();
		try {
			return function.apply(profile);
		} finally {
			service.ungetService(profile);
		}
		
	}
	
	private static void getInputData(final Map<DataPoint, Resource> input, final TemplateGrid<DataPoint> grid, 
			final List<DataPoint> points, final OgemaHttpRequest req) {
		final Collection<Row> rows = grid.getRows(req);
		for (Row row: rows) {
			final Map<String,Object> cols = row.cells;
			final String dpLabel = (String) cols.get("dp");
			final Optional<DataPoint> opt = points.stream()
					.filter(dp -> dpLabel.equals(dp.label(req.getLocale())))
					.findAny();
				if (!opt.isPresent())
					continue;
				final Object r = cols.get("select");
				if (!(r instanceof TemplateDropdown<?>))
					continue;
				final Resource res = ((TemplateDropdown<Resource>) r).getSelectedItem(req);
				if (res != null)
					input.put(opt.get(), res);
		}
	}
	
	private static void selectData(final Map<DataPoint, Resource> input, final TemplateGrid<DataPoint> grid, 
			final OgemaHttpRequest req) {
		final Collection<Row> rows = grid.getRows(req);
		for (Row row: rows) {
			final Map<String,Object> cols = row.cells;
			final String dpLabel = (String) cols.get("dp");
			final Resource value = input.entrySet().stream()
				.filter(entry -> dpLabel.equals(entry.getKey().label(req.getLocale())))
				.map(Map.Entry::getValue)
				.findAny().orElse(null);
			if (value == null)
				continue;
			final Object r = cols.get("select");
			if (!(r instanceof TemplateDropdown<?>))
				continue;
			((TemplateDropdown<Resource>) r).selectItem(value.getLocationResource(), req);
		}
	}
	
	// FIXME ordering of template grid does not work
	private static List<Long> getDurations(final TemplateGrid<OrderedState> grid, final List<State> states, final OgemaHttpRequest req) {
		final Collection<Row> rows = grid.getRows(req); 
		// TODO grid.getRows as Map<OrderedState, Row> and grid.getRow(OrderedState)
		final List<Long> durations = new ArrayList<>(states.size());
		int idx = -1;
		for (State state:states) {
			idx++;
			final String label = new OrderedState(state, idx).label(req.getLocale());
			final Optional<Row> opt = rows.stream()
				.filter(row -> label.equals(row.cells.get("state")))
				.findAny();
			if (!opt.isPresent()) {
				durations.add(null);
				continue;
			}
			final Object d = opt.get().cells.get("duration");
			final Long dur = ((ValueInputField<Long>) d).getNumericalValue(req);
			final ChronoUnit unit = ((EnumDropdown<ChronoUnit>) opt.get().cells.get("unit")).getSelectedItem(req);
			final long millis = unit.getDuration().toMillis() * dur;
			durations.add(millis);
		}
		return durations;
	}
	
	private static List<StateDuration> getDurationObjects(final TemplateGrid<OrderedState> grid, final Collection<State> states, final OgemaHttpRequest req) {
		final Collection<Row> rows = grid.getRows(req);
		final List<StateDuration> durations = new ArrayList<>(rows.size());
		final Iterator<Row> rowIt = rows.iterator();
		final Iterator<State> statesIt = states.iterator();
		while (rowIt.hasNext()) {
			final Row row = rowIt.next();
			final Object d = row.cells.get("duration");
			if (!(d instanceof ValueInputField<?>))
				continue;
			if (!statesIt.hasNext())
				break;
			final State state =statesIt.next();
			final Long dur = ((ValueInputField<Long>) d).getNumericalValue(req);
			if (dur == null)
				continue;
			final ChronoUnit unit = ((EnumDropdown<ChronoUnit>) row.cells.get("unit")).getSelectedItem(req);
			durations.add(new StateDuration(state, dur.intValue(), unit));
		}
		return durations;
	}
	
	private static void setDurations(final TemplateGrid<OrderedState> grid, final List<StateDuration> durations, final OgemaHttpRequest req) {
		final Collection<Row> rows = grid.getRows(req);
		final Iterator<Row> rowIt = rows.iterator();
		final Iterator<StateDuration> durIt = durations.iterator();
		while (rowIt.hasNext()) {
			final Row row = rowIt.next();
			final Map<String, Object> cells = row.cells;
			final Object d = cells.get("duration");
			if (!(d instanceof ValueInputField<?>))
				continue;
			if (!durIt.hasNext())
				break;
			final StateDuration sd = durIt.next(); // TODO check for correct label
			final EnumDropdown<ChronoUnit> ud = (EnumDropdown<ChronoUnit>) cells.get("unit");
			final int dur = sd.getDuration();
			final ChronoUnit unit = sd.getUnit();
			((ValueInputField<Long>) d).setNumericalValue((long) dur, req);
			ud.selectItem(unit, req);
		}
	}
	
	@SuppressWarnings("serial")
	private static class TaskAlert extends Alert {
		
		private final ThreadLocal<Future<Profile>> currentTask  = new ThreadLocal<>();

		public TaskAlert(WidgetPage<?> page, String id, String text) {
			super(page, id, text);
		}
		
		public void start(final Future<Profile> task) {
			currentTask.set(task);
		}
		
		public boolean isTaskActive() {
			final Future<Profile> future = currentTask.get();
			return future != null && !future.isDone();
		}
		
		public boolean cancel(OgemaHttpRequest req) {
			final Future<Profile> future = currentTask.get();
			if (future == null || future.isDone())
				return false;
			final boolean cancelled = future.cancel(false);
			currentTask.set(null);
			if (!cancelled)
				return false;
			setText("Recording has been cancelled", req);
			setStyle(AlertData.BOOTSTRAP_WARNING, req);
			return true;
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final Future<Profile> future = currentTask.get();
			if (future == null)
				return;
			setWidgetVisibility(true, req);
			if (future.isDone()) {
				try {
					final Profile task = future.get();
					setText("Task finished: " + task.label(req.getLocale()) + ": " + task.description(req.getLocale()), req);
					setStyle(AlertData.BOOTSTRAP_SUCCESS, req);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (ExecutionException | RuntimeException e) {
					setText("Recording failed: " + (e instanceof ExecutionException ? e.getCause() : e), req);
					setStyle(AlertData.BOOTSTRAP_DANGER, req);
					LoggerFactory.getLogger(getClass()).warn("Recording failed",e);
				}
				currentTask.set(null);
				setPollingInterval(-1, req);
			} else {
				setPollingInterval(5000, req);
			}
		}
		
	}
	
	private static void setGridStyle(final AbstractGrid grid) {
		grid.setDefaultAppendFillColumn(true);
		grid.setDefaultPrependFillColumn(true);
		grid.setDefaultColumnGap("1em");
		grid.setDefaultRowGap("1em");
	}
	
	private static class OrderedState {
		
		private final State state;
		private final int idx;
		
		public OrderedState(State state, int idx) {
			this.state = state;
			this.idx = idx;
		}
		
		public int getIdx() {
			return idx;
		}
		
		public State getState() {
			return state;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof OrderedState))
				return false;
			final OrderedState other = (OrderedState) obj;
			return Objects.equals(this.getState(), other.getState()) && this.getIdx() == other.getIdx();
		}
		
		@Override
		public int hashCode() {
			return state.hashCode() + idx * 7;
		}
		
		@Override
		public String toString() {
			return "OrderedState [state = " + state.id() + ", idx = " + idx + "]";
		}
		
		public String label(OgemaLocale  locale) {
			return state.label(locale) + " (" + idx + ")";
		}
		
		public static int compare(final OrderedState state1, final OrderedState state2) {
			return Integer.compare(state1.getIdx(), state2.getIdx());
		}
		
	}
	
}
