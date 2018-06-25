package org.smartrplace.tools.timer.utils.model;

import java.time.DayOfWeek;
import java.time.ZoneId;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.slf4j.LoggerFactory;

public interface DayTemplateProviderData extends Resource {

	/**
	 * Value must be convertible to {@link ZoneId} via {@link ZoneId#of(String)}.
	 * @return
	 */
	StringResource zoneId();
	DayTemplateData defaultDay();

	default void setZoneId(ZoneId zone) {
		if (zone == null)
			zoneId().delete();
		zoneId().<StringResource> create().setValue(zone.getId());
	}
	
	default ZoneId getZoneId() {
		if (!zoneId().isActive())
			return ZoneId.systemDefault();
		try {
			return ZoneId.of(zoneId().getValue());
		} catch (Exception e) {
			LoggerFactory.getLogger(DayTemplateProviderData.class).warn("Failed to read zone id from resource",e);
			return ZoneId.systemDefault();
		}
	}
	
	default DayTemplateData getDayTemplateData(DayOfWeek day) {
		if (day == null)
			return defaultDay();
		return getSubResource(day.name(), DayTemplateData.class);
	}
	
}
