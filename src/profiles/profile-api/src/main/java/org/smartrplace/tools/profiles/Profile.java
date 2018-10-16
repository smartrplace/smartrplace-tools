package org.smartrplace.tools.profiles;

import java.util.Map;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.tools.profiles.DataPoint.DataType;

import de.iwes.widgets.template.LabelledItem;

public interface Profile extends LabelledItem {
	
	/**
	 * 
	 * Allowed return values are
	 * <ul>
	 *   <li>{@link ReadOnlyTimeSeries} if dp is of {@link DataPoint#dataType() type} {@link DataType#TIME_SERIES}
	 *   <li>String if dp is of {@link DataPoint#dataType() type} {@link DataType#STRING}
	 *   <li>Number if dp is of {@link DataPoint#dataType() type} {@link DataType#SINGLE_VALUE}
	 * </ul>
	 * @param dp
	 * @return
	 */
	Object getPrimaryData(DataPoint dp);
	Object getContextData(DataPoint dp);
	Map<DataPoint, Object> getPrimaryData();
	Map<DataPoint, Object> getContextData();	
	Map<Long, State> stateEndTimes();
 	 
	String templateId();

}
