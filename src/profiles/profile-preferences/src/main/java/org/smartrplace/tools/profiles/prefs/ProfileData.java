package org.smartrplace.tools.profiles.prefs;

import java.util.Collections;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.model.actors.OnOffSwitch;
import org.smartrplace.tools.profiles.DataPoint;

public class ProfileData {

	private final Map<DataPoint, Resource> dataPoints;
	private final OnOffSwitch onOffSwitch;
	private final String endStateId;
	
	public ProfileData(Map<DataPoint, Resource> dataPoints, OnOffSwitch onOffSwitch, String endStateId) {
		this.dataPoints = dataPoints == null || dataPoints.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(dataPoints);
		this.onOffSwitch = onOffSwitch;
		this.endStateId = endStateId == null || endStateId.isEmpty() ? null : endStateId;
	}
	
	public Map<DataPoint, Resource> getDataPoints() {
		return dataPoints;
	}
	
	public OnOffSwitch getOnOffSwitch() {
		return onOffSwitch;
	}
	
	public String getEndStateId() {
		return endStateId;
	}
	
}
