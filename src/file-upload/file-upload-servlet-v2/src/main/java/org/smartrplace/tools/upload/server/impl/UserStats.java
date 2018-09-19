package org.smartrplace.tools.upload.server.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.servlet.http.HttpServletResponse;

class UserStats {

	private static final long DAY = Duration.ofDays(1).toMillis();
	private static final long HOUR = Duration.ofHours(1).toMillis();
	private static final long MINUTE = Duration.ofMinutes(1).toMillis();
	private final String user;
	// entries: access times
	private final NavigableSet<Long> accessed = new ConcurrentSkipListSet<>();
	
	UserStats(String user) {
		this.user = user;
	}
	
	private final void touched(final long now) {
		accessed.add(now);
		accessed.headSet(now - DAY, false).clear();
	}
	
	private final int getAccessCount(final long now, final long delta) {
		return accessed.tailSet(now-delta, true).size();
	}
	
	boolean accessGranted(final FileUploadConfiguration config, final HttpServletResponse resp) throws IOException {
		final long now = System.currentTimeMillis();
		touched(now);
		final int dailySize = getAccessCount(now, DAY);
		if (dailySize > config.maxUploadsPerDay() + 1) { // +1 because we already counted this request
			// 429 : too many requests by the user per time interval
			resp.sendError(429, "Too many requests per day; max allowed: " + config.maxUploadsPerDay());
			return false;
		}
		if (dailySize > config.maxUploadsPerMinute()) {
			final int hourlySize = getAccessCount(now, HOUR);
			if (hourlySize > config.maxUploadsPerHour() + 1) {
				resp.sendError(429, "Too many requests per hour; max allowed: " + config.maxUploadsPerHour());
				return false;
			}
			if (hourlySize > config.maxUploadsPerMinute()) {
				final int minuteSize = getAccessCount(now, MINUTE);
				if (minuteSize > config.maxUploadsPerMinute() + 1) {
					resp.sendError(429, "Too many requests per minute; max allowed: " + config.maxUploadsPerHour());
					return false;
				}
			}
		}
		return true;
	}
	
}
