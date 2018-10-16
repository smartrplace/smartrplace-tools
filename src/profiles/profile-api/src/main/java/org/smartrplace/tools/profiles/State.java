package org.smartrplace.tools.profiles;

import de.iwes.widgets.template.LabelledItem;

public interface State extends LabelledItem {

	Class<?> typeInfo();
	
	default long minDuration() {
		return -1;
	} 
	
	default long maxDuration() {
		return -1;
	}
	
	
}
