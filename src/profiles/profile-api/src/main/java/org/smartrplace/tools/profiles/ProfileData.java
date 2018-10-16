package org.smartrplace.tools.profiles;

import org.ogema.core.model.simple.SingleValueResource;

public interface ProfileData {

	SingleValueResource getPrimaryData(DataPoint dp);
	SingleValueResource getContextData(DataPoint dp);
	
}
