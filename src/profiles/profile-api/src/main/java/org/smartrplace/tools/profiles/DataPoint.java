package org.smartrplace.tools.profiles;

import de.iwes.widgets.template.LabelledItem;

public interface DataPoint extends LabelledItem {

	boolean optional();
	DataType dataType();
	Class<?> typeInfo();
	
	enum DataType {
		
		STRING, SINGLE_VALUE, TIME_SERIES
		
	}
	
}
