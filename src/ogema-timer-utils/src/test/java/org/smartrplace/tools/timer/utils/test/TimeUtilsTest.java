package org.smartrplace.tools.timer.utils.test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import org.junit.Assert;
import org.junit.Test;
import org.smartrplace.tools.timer.utils.TimeUtils;

public class TimeUtilsTest {
	
	private static final ZoneId zone = ZoneId.systemDefault();
	
	private static void testTruncation(final LocalDateTime in, final LocalDateTime expected, 
				final long factor, final ChronoUnit unit, final boolean previousOrNext) {
		final ZonedDateTime inZdt = ZonedDateTime.of(in, zone);
		final Instant result;
		if (previousOrNext)
			result = TimeUtils.getLastAlignedIntervalStart(inZdt.toInstant(), factor, unit, zone);
		else
			result = TimeUtils.getNextAlignedIntervalStart(inZdt.toInstant(), factor, unit, zone);
		final LocalDateTime intervalStart = ZonedDateTime.ofInstant(result, zone).toLocalDateTime();
		Assert.assertEquals("Unexpected aligned interval start for base time " + in + " and period " + factor + " " + unit, expected, intervalStart);
	}

	// test 1 minute alignment
	@Test
	public void previousAlignedIntervalWorksWithDuration0() {
		final LocalDateTime dateTimeIn = LocalDateTime.of(2018, 6, 22, 11, 25, 51);
		final LocalDateTime dateTimeExpected = LocalDateTime.of(2018, 6, 22, 11, 25);
	    testTruncation(dateTimeIn, dateTimeExpected, 1, ChronoUnit.MINUTES, true);
	}
	
	// test 15 minutes alignment
	@Test
	public void previousAlignedIntervalWorksWithDuration1() {
	    testTruncation(LocalDateTime.of(2018, 6, 22, 11, 25, 51), LocalDateTime.of(2018, 6, 22, 11, 15), 
	    		15, ChronoUnit.MINUTES, true);
	}
	
	// test 17 minutes alignment
	@Test
	public void previousAlignedIntervalWorksWithDuration2() {
	    testTruncation(LocalDateTime.of(2018, 6, 22, 11, 25, 51), LocalDateTime.of(2018, 6, 22, 11, 25), 
	    		17, ChronoUnit.MINUTES, true);
	}
	
	// test 1 day alignment
	@Test
	public void previousAlignedIntervalWorksWithPeriod0() {
	    testTruncation(LocalDateTime.of(2018, 8, 21, 23, 05, 21), LocalDateTime.of(2018, 8, 21, 0, 0), 
	    		1, ChronoUnit.DAYS, true);
	}
	
	// test 1 week alignment
	@Test
	public void previousAlignedIntervalWorksWithPeriod4() {
		final LocalDateTime monday = LocalDateTime.of(2018, 8, 21, 0, 0).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		final LocalDateTime wednesday = monday.plus(2, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS).plus(5, ChronoUnit.MINUTES);
	    testTruncation(wednesday, monday, 1, ChronoUnit.WEEKS, true);
	}
	
	// test 1 month alignment
	@Test
	public void previousAlignedIntervalWorksWithPeriod1() {
	    testTruncation(LocalDateTime.of(2018, 8, 21, 23, 05, 21), LocalDateTime.of(2018, 8, 1, 0, 0), 
	    		1, ChronoUnit.MONTHS, true);
	}
	
	// test 3 months alignment
	@Test
	public void previousAlignedIntervalWorksWithPeriod2() {
	    testTruncation(LocalDateTime.of(2018, 8, 21, 23, 05, 21), LocalDateTime.of(2018, 7, 1, 0, 0),
	    		3, ChronoUnit.MONTHS, true);
	}
	
	@Test
	public void previousEqualWorksDuration() {
	    testTruncation(LocalDateTime.of(2018, 6, 22, 11, 0, 0), LocalDateTime.of(2018, 6, 22, 11, 0), 
	    		13, ChronoUnit.HOURS, true);		
	}
	
	@Test
	public void previousEqualWorksPeriod() {
	    testTruncation(LocalDateTime.of(2018, 6, 1, 0, 0, 0), LocalDateTime.of(2018, 6, 1, 0, 0), 
	    		7, ChronoUnit.MONTHS, true);		
	}
	
	// test 15 minutes alignment for next
	@Test
	public void nextAlignedIntervalWorksWithDuration0() {
	    testTruncation(LocalDateTime.of(2018, 6, 22, 11, 25, 51), LocalDateTime.of(2018, 6, 22, 11, 30), 
	    		15, ChronoUnit.MINUTES, false);
	}
	
	// test 3 months alignment for next
	@Test
	public void nextAlignedIntervalWorksWithPeriod0() {
	    testTruncation(LocalDateTime.of(2018, 8, 21, 23, 05, 21), LocalDateTime.of(2018, 10, 1, 0, 0), 
	    		3, ChronoUnit.MONTHS, false);
	}
}
