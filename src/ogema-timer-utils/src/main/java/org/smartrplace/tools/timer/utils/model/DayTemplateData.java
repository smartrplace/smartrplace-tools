package org.smartrplace.tools.timer.utils.model;

import java.time.LocalTime;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.tools.resource.util.ValueResourceUtils;

public interface DayTemplateData extends Resource {

	ResourceList<TemplateData> data();
	
	default NavigableMap<LocalTime, Object> getValuesAsMap() {
		final NavigableMap<LocalTime, Object> map = new TreeMap<>();
		data().getAllElements().forEach(data -> {
			final LocalTime time = data.getLocalTime();
			if (time == null || !data.value().isActive())
				return;
			final Object o = ValueResourceUtils.getValue(data.value());
			if (o != null)
				map.put(time, o);
		});
		return map;
	}
	
}
