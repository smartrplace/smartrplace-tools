package org.smartrplace.tools.profiles.prefs;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.model.actors.OnOffSwitch;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.State;

public class ProfileData {

	private final Map<DataPoint, Resource> dataPoints;
	private final OnOffSwitch onOffSwitch;
	private final State endState;
	private final List<StateDuration> durations;
	
	public ProfileData(Map<DataPoint, Resource> dataPoints, OnOffSwitch onOffSwitch, State endState, List<StateDuration> durations) {
		this.dataPoints = dataPoints == null || dataPoints.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(dataPoints);
		this.onOffSwitch = onOffSwitch;
		this.endState = endState;
		this.durations = durations == null ? null : Collections.unmodifiableList(durations);
	}
	
	public Map<DataPoint, Resource> getDataPoints() {
		return dataPoints;
	}
	
	public OnOffSwitch getOnOffSwitch() {
		return onOffSwitch;
	}
	
	public State getEndState() {
		return endState;
	}
	
	public List<StateDuration> getDurations() {
		return durations;
	}
	
}
