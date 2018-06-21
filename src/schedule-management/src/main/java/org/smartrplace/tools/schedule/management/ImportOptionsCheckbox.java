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

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.checkbox.Checkbox;
import de.iwes.widgets.html.form.checkbox.CheckboxData;

class ImportOptionsCheckbox extends Checkbox {

	private static final long serialVersionUID = 1L;
	private static final Map<String,Boolean> importOptions;
	private static final String REPLACE_EXISTING = "Replace existing values in time range";
	private static final String MOVE_START_HERE = "Move time series start to now";
	private static final String MOVE_END_HERE = "Move time series end to now";
	private static final String MOVE_START_TO_0 = "Move time series start to 0";
	
	static {
		importOptions = new LinkedHashMap<>();
		importOptions.put(REPLACE_EXISTING, false);
		importOptions.put(MOVE_START_HERE, false);
		importOptions.put(MOVE_END_HERE, false);
		importOptions.put(MOVE_START_TO_0, false);
	}

	ImportOptionsCheckbox(WidgetPage<?> page, String id) {
		super(page, id);
		setDefaultList(new LinkedHashMap<>(importOptions));
		this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST); 
	}
	
	/*
	 * internal methods
	 */

	@Override
	public CheckboxData createNewSession() {
		return new ImportOptionsCheckboxData(this);
	}
	
	/*
	 * public methods
	 */

	boolean doReplaceExisting(OgemaHttpRequest req) {
		return getCheckboxList(req).get(REPLACE_EXISTING);
	}
	
	boolean moveStartHere(OgemaHttpRequest req) {
		return getCheckboxList(req).get(MOVE_START_HERE);
	}
	
	boolean moveEndHere(OgemaHttpRequest req) {
		return getCheckboxList(req).get(MOVE_END_HERE);
	}

	boolean moveStartTo0(OgemaHttpRequest req) {
		return getCheckboxList(req).get(MOVE_START_TO_0);
	}
	
	private static class ImportOptionsCheckboxData extends CheckboxData {

		ImportOptionsCheckboxData(ImportOptionsCheckbox checkbox) {
			super(checkbox);
		}
		
		// copied from CheckboxData
		@Override
		public JSONObject onPOST(String data, OgemaHttpRequest req) {
			final boolean wasStartMover = checkboxList.get(MOVE_START_HERE);
			final boolean wasEndMover = checkboxList.get(MOVE_END_HERE);
			final boolean wasStart0 = checkboxList.get(MOVE_START_TO_0);
	        JSONObject request = new JSONObject(data);
	        data = request.getString("data");
	        try {
	            String[] map = data.split("&");
	            checkboxList.clear();
	            for (String entry : map) {
	                String key = entry.split("=")[0];
	                String value = entry.split("=")[1];
	                checkboxList.put(key, Boolean.valueOf(value));
//	                req.widetValue = value;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
			final boolean isStartMover = checkboxList.get(MOVE_START_HERE);
			final boolean isEndMover = checkboxList.get(MOVE_END_HERE);
			final boolean isStart0Mover = checkboxList.get(MOVE_START_TO_0);
	        if (isStartMover && isEndMover || isStartMover && isStart0Mover || isStart0Mover && isEndMover) { // must not both be active at the same time
	        	boolean fixed = wasStartMover || wasEndMover || wasStart0;
	        	if (wasStartMover || !fixed) 
	        		checkboxList.put(MOVE_START_HERE, false);
	        	if (wasEndMover || !fixed) 
	        		checkboxList.put(MOVE_END_HERE, false);
	        	if (wasStart0 || !fixed)
	        		checkboxList.put(MOVE_START_TO_0, false);
	        }
	        return request;
		}
		
	}
	
}
