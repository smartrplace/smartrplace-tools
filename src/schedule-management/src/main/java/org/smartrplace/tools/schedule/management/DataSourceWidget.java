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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.ogema.core.model.Resource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.schedule.management.imports.DataGenerator;
import org.smartrplace.tools.schedule.management.imports.OgemaDataSource;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.html.selectiontree.LinkingOption;
import de.iwes.widgets.html.selectiontree.SelectionItem;
import de.iwes.widgets.html.selectiontree.SelectionTree;
import de.iwes.widgets.html.selectiontree.TerminalOption;
import de.iwes.widgets.template.DisplayTemplate;
import de.iwes.widgets.template.LabelledItem;

class DataSourceWidget extends PageSnippet {

	private static final long serialVersionUID = 1L;
	final TemplateDropdown<? extends LabelledItem> sourceTypeDropdown; // FIXME why nothign selected initially?
	final TemplateDropdown<Object> highLevelOptions; // only active if sourceTypeDropdown has a DataGenerator selected
	final ScheduleSelector scheduleSelector; // only active if sourceTypeDropdown has a DataGenerator selected
	final SelectionTree selectionTree; // only active if sourceTypeDropdown has a DataProvider selected
	final boolean appendSourceTypeDropdown;
	final boolean fullSize;
	
	DataSourceWidget(WidgetPage<?> page, String id, DataSourceFactory dataSources, Set<DataProvider<?>> dataSources2) {
		this(page, id, dataSources, dataSources2, null, false, false);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	DataSourceWidget(WidgetPage<?> page, String id, DataSourceFactory dataSources, final Set<DataProvider<?>> dataSources2,
				TemplateDropdown<? extends LabelledItem> sourceTypeDropdown, boolean fullSize, boolean loadTargets) {
		super(page, id, true);
		this.fullSize = fullSize;
		this.appendSourceTypeDropdown = sourceTypeDropdown == null;
		if (appendSourceTypeDropdown) {
			sourceTypeDropdown = new TemplateDropdown<LabelledItem>(page, "sourceTypeDd_" + getId()) {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public void onGET(OgemaHttpRequest req) {
					final List<LabelledItem> items = new ArrayList<LabelledItem>((Set) dataSources2);
					if (!loadTargets)
						items.addAll(dataSources.getSources());
					else
						items.addAll(dataSources.getTargets());
					update(items, req);
				}
				
			};
			sourceTypeDropdown.setTemplate((DisplayTemplate) DataGenerator.TEMPLATE);
		}
		this.sourceTypeDropdown = sourceTypeDropdown;
		
		this.highLevelOptions = new TemplateDropdown<Object>(page, "highLevelOptions_" + getId()) {

			private static final long serialVersionUID = 1L;
		
			@SuppressWarnings({ "rawtypes" })
			@Override
			public void onGET(OgemaHttpRequest req) {
//				DataGenerator source = DataSourceWidget.this.sourceTypeDropdown.getSelectedItem(req);
				final LabelledItem source0 = DataSourceWidget.this.sourceTypeDropdown.getSelectedItem(req);
				if (!(source0 instanceof OgemaDataSource)) {
					update(Collections.emptyList(), req);
					disable(req);
				}
				else {
					enable(req);
					List<?> options = ((OgemaDataSource) source0).getHighLevelOptions();
					if (options == null) 
						options = Collections.emptyList();
					update(options, req);
				}
			}
			
		};
		highLevelOptions.setTemplate(new DisplayTemplate<Object>() {

			@SuppressWarnings("rawtypes")
			@Override
			public String getId(Object object) {
				if (object instanceof Class)
					return ((Class) object).getName();
				if (object instanceof Resource)
					return ((Resource) object).getPath();
				return object.toString();
			}

			@SuppressWarnings("rawtypes")
			@Override
			public String getLabel(Object object, OgemaLocale arg1) {
				if (object instanceof Class)
					return ((Class) object).getSimpleName();
				if (object instanceof Resource)
					return ((Resource) object).getPath();
				return object.toString();
			}
		});
		this.scheduleSelector = new ScheduleSelector(page, "scheduleSelector_" + getId());
		
		this.selectionTree = loadTargets ? null : new SelectionTree(page, "selectionTree_" + getId()) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final LabelledItem source0 = DataSourceWidget.this.sourceTypeDropdown.getSelectedItem(req);
				if (!(source0 instanceof DataProvider<?>)) {
					setWidgetVisibility(false, req);
					setSelectionOptions(Collections.emptyList(), req);
					return;
				} 
				setWidgetVisibility(true, req);
				final DataProvider<?> provider = (DataProvider<?>) source0;
				final List<LinkingOption> options = Arrays.asList(provider.selectionOptions());
				setSelectionOptions(options, req);
				final OgemaWidget terminal = getTerminalSelectWidget(req);
			}
			
		};
		
		buildWidget();
		setDependencies();
	}
	
	private final void buildWidget() {
		int rows = appendSourceTypeDropdown ? 3 : 2;
		final StaticTable table;
		if (fullSize)
			table = new StaticTable(rows, 2);
		else
			table = new StaticTable(rows, 2, new int[] {2,3});
		if (appendSourceTypeDropdown)
			table.setContent(0, 0, "Source").setContent(0, 1, sourceTypeDropdown);
		int offset = appendSourceTypeDropdown ? 1 : 0;
		table.setContent(offset, 0, "Options").setContent(offset, 1, highLevelOptions)
			.setContent(1+offset, 0, "Time series").setContent(1+offset, 1, scheduleSelector);
		append(table, null);
		if (selectionTree != null)
			linebreak(null).append(selectionTree, null);
	}
	
	private final void setDependencies() {
		sourceTypeDropdown.triggerAction(highLevelOptions, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		sourceTypeDropdown.triggerAction(scheduleSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		if (selectionTree != null)
			sourceTypeDropdown.triggerAction(selectionTree, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		highLevelOptions.triggerAction(scheduleSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	public ReadOnlyTimeSeries getSelectedSchedule(OgemaHttpRequest req) {
		final LabelledItem source = sourceTypeDropdown.getSelectedItem(req);
		if (source instanceof DataGenerator) {
			return scheduleSelector.getSelectedSchedule(req);
		} else if (source instanceof DataProvider<?>) {
			selectionTree.getTerminalSelectWidget(req);
			@SuppressWarnings("unchecked")
			final TemplateMultiselect<SelectionItem> selector = (TemplateMultiselect<SelectionItem>) selectionTree.getTerminalSelectWidget(req);
			if (selector == null)
				return null;
			@SuppressWarnings("unchecked")
			final TerminalOption<ReadOnlyTimeSeries> terminalOpt = (TerminalOption<ReadOnlyTimeSeries>) selectionTree.getTerminalOption(req);
			if (terminalOpt == null)
				return null;
			final List<SelectionItem> items = selector.getSelectedItems(req);
			if (items.size() != 1)
				return null;
			return terminalOpt.getElement(items.get(0));
		} else
			return null;
		 
		
	}
	
	
	class ScheduleSelector extends Dropdown  {

		public ScheduleSelector(WidgetPage<?> page, String id) {
			super(page, id);
		}
		
		private static final long serialVersionUID = 1L;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void onGET(OgemaHttpRequest req) {
//			DataGenerator source = sourceTypeDropdown.getSelectedItem(req);
			final LabelledItem source0 = sourceTypeDropdown.getSelectedItem(req);

			if (!(source0 instanceof OgemaDataSource)) {
				update(Collections.emptyMap(), req);
				disable(req);
				return;
			}
			enable(req);
			Object hlOption = null;
			try {
				hlOption = highLevelOptions.getSelectedItem(req); // may be null
				setOptions(((OgemaDataSource) source0).getAllTimeseries(hlOption),req);
			} catch (Exception e) {
				LoggerFactory.getLogger(ScheduleManagementApp.class).error("Failed to load time series for source {}",source0.id(),e);
				setOptions(Collections.emptyList(), req);
			}
			String selected = getSelectedValue(req);
			ReadOnlyTimeSeries schedule = null;
			if (selected !=  null)
				schedule = ((OgemaDataSource) source0).getTimeseries(hlOption, selected);
			getData(req).schedule = schedule;
		};
		
		@Override
		public ScheduleSelectorData createNewSession() {
			return new ScheduleSelectorData(this);
		}
		
		@Override
		public ScheduleSelectorData getData(OgemaHttpRequest req) {
			return (ScheduleSelectorData) super.getData(req);
		}
		
		public ReadOnlyTimeSeries getSelectedSchedule(OgemaHttpRequest req) {
			return getData(req).schedule;
		}
		
	}
	
	class ScheduleSelectorData extends DropdownData {

		private ReadOnlyTimeSeries schedule;
		
		public ScheduleSelectorData(ScheduleSelector dropdown) {
			super(dropdown);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public JSONObject onPOST(String arg0, OgemaHttpRequest req) {
			JSONObject obj = super.onPOST(arg0, req);
			String opt = getSelectedValue();
			if (opt == null || opt.equals(DropdownData.EMPTY_OPT_ID)) {
				schedule = null;
				return obj;
			}
//			DataGenerator source = sourceTypeDropdown.getSelectedItem(req);
			final LabelledItem source0 = sourceTypeDropdown.getSelectedItem(req);
			if (!(source0 instanceof OgemaDataSource)) {
				schedule = null;
				return obj;
			}
			Object instance = highLevelOptions.getSelectedItem(req);
			this.schedule = ((OgemaDataSource) source0).getTimeseries(instance, opt);
			return obj;
		}
		
	}

}
