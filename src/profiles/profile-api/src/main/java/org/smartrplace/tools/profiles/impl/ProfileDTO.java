package org.smartrplace.tools.profiles.impl;

import java.util.Map;

public class ProfileDTO {

	public String id;
	public String templateId;
	public Map<String, String> stringValues;
	public Map<String, Number> numericalValues;
	public Map<String, TimeseriesDTO> timeseriesValues;
	public Map<Long, String> stateEndTimes;
	
}
