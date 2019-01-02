/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.tools.timer.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Time utils, in particular for finding alinged interval start times, such as the start time
 * of the current week, the next full 15 minutes, etc.
 * 
 * @author cnoelle
 */
public class TimeUtils {

	private static final Map<TemporalUnit, Long> DEFAULT_ALIGNMENT_FACTORS = Stream.<TemporalUnit> builder()
			.add(ChronoUnit.MONTHS).add(ChronoUnit.HOURS).add(ChronoUnit.MINUTES).add(ChronoUnit.SECONDS)
			.build()
			.collect(Collectors.toMap(Function.identity(), unit -> 
				unit == ChronoUnit.MONTHS ? 12L : 
				unit == ChronoUnit.HOURS ? 24L : 
				60L));

	private static final Map<TemporalUnit, TemporalUnit> DEFAULT_ALIGNMENT_UNITS = Stream.<TemporalUnit> builder()
			.add(ChronoUnit.MONTHS).add(ChronoUnit.HOURS).add(ChronoUnit.MINUTES).add(ChronoUnit.SECONDS)
			.build()
			.collect(Collectors.toMap(Function.identity(), unit -> 
				unit == ChronoUnit.MONTHS ? ChronoUnit.YEARS : 
				unit == ChronoUnit.HOURS ? ChronoUnit.DAYS : 
				unit == ChronoUnit.MINUTES ? ChronoUnit.HOURS :
				ChronoUnit.MINUTES));

	
	// no need to instantiate this
	private TimeUtils() {}

	/**
	 * Like {@link #getLastAlignedIntervalStart(Instant, TemporalAmount, ZoneId)}, except that 
	 * the time zone is taken to be the system default zone (which is only relevant if the period
	 * is date-based).
	 * @param base
	 * @param period
	 * @return
	 * 		an instant before or equal to the passed instant, corresponding to an aligned interval start time as described above.
	 * @throws IllegalArgumentException if {@link TemporalAmount#getUnits() period.getUnits} returns more than one unit, or zero units.
	 * @throws NullPointerException if {@link Base} or period are null
	 * @throws java.time.temporal.UnsupportedTemporalTypeException for "exotic" units
	 */
	public static Instant getLastAlignedIntervalStart(final Instant base, final long factor, final TemporalUnit unit) {
		return getLastAlignedIntervalStart(base, factor, unit, null);	
	}
	
	/**
	 * Get the start time of the current aligned interval for a specific {@link TemporalAmount}, such as a {@link java.time.Duration}
	 * or a {@link java.time.Period}. The temporal amount must be defined in terms of a single {@link TemporalUnit},
	 * otherwise an {@link IllegalArgumentException} is thrown.<br>
	 * 
	 * The interval start time is determined as follows. First, the passed instant is truncated to the unit of time 
	 * specified by period. If the factor multiplying the unit in the temporal amount is 1, then this instant is returned.
	 * If the factor is greater than one, then it is checked whether the temporal amount can be aligned within a period
	 * corresponding to the next higher temporal unit (for instance, an hour can be splits into four quarters of 15 minutes duration each,
	 * or into 3 parts of 20 minutes each; a year can be split into 2 half-years of 6-months duration, into 3 parts of 4-months duration,
	 * into 4 quarters of 3-months duration, or into 6 periods of 2-months duration). If no alignment is possible,
	 * the truncated instant is returned. If alignment is possible, the previous aligned start time is returned.<br>
	 * 
	 * Examples:
	 * <ul>
	 * 		<li>if the period is 1 minute, then a time stamp 2018-06-22T11:25:51 will be truncated to 2018-06-22T11:25:00
	 * 			(the full hour is split into four quarters)
	 * 		<li>if the period is 15 minutes, then a time stamp 2018-06-22T11:25:51 will be truncated to 2018-06-22T11:15:00
	 * 			(the full hour is split into four quarters)
	 *      <li>if the period is 17 minutes, then a time stamp 2018-06-22T11:25:51 will be truncated to 2018-06-22T11:25:00 
	 *          (17 minutes cannot be aligned within one hour, hence only the unit <code>MINUTES</code> is taken into account and 
	 *          	the factor 17 is ignored)
	 *      <li>if the period is 1 month, then a time stamp 2018-08-21T23:05:21 will be truncated to 2018-08-01T00:00:00
	 *      <li>if the period is 3 months, then a time stamp 2018-08-21T23:05:21 will be truncated to 2018-07-01T00:00:00
	 *          (the year is split into four quarters, starting in January, April, July and October, respectively)
	 * </ul>
	 *  
	 * @param base
	 * @param period
	 * @param timeZone
	 * 		This is only relevant if the temporal unit of the passed temporal amount is {@link TemporalUnit#isDateBased() date-based}.
	 * 		If the passed time zone is null, then the system default zone is used. 
	 * @return
	 * 		an instant before or equal to the passed instant, corresponding to an aligned interval start time as described above.
	 * @throws IllegalArgumentException if {@link TemporalAmount#getUnits() period.getUnits} returns more than one unit, or zero units.
	 * @throws NullPointerException if {@link Base} or period are null
	 * @throws java.time.temporal.UnsupportedTemporalTypeException for "exotic" units
	 */
	public static Instant getLastAlignedIntervalStart(final Instant base, final long factor, final TemporalUnit unit, ZoneId timeZone) {
		return previousOrNextAlignedIntervalStart(base, factor, unit, timeZone, true);	
	}

	/**
	 * Like {@link #getNextAlignedIntervalStart(Instant, TemporalAmount, ZoneId)}, except that 
	 * the time zone is taken to be the system default zone (which is only relevant if the period
	 * is date-based).
	 * @param base
	 * @param period
	 * @return
	 * 	 	an instant after or equal to the passed instant, corresponding to an aligned interval start time.
	 * @throws IllegalArgumentException if {@link TemporalAmount#getUnits() period.getUnits} returns more than one unit, or zero units.
	 * @throws NullPointerException if {@link Base} or period are null
 	 * @throws java.time.temporal.UnsupportedTemporalTypeException for "exotic" units
	 */
	public static Instant getNextAlignedIntervalStart(final Instant base, final long factor, final TemporalUnit unit) {
		return getNextAlignedIntervalStart(base, factor, unit, null);
	}
	
	/**
	 * See {@link #getLastAlignedIntervalStart(Instant, TemporalAmount, ZoneId)}
	 * @param base
	 * @param period
	 * @param timeZone
	 * @return
	 * 	 	an instant after or equal to the passed instant, corresponding to an aligned interval start time.
	 * @throws IllegalArgumentException if {@link TemporalAmount#getUnits() period.getUnits} returns more than one unit, or zero units.
	 * @throws NullPointerException if {@link Base} or period are null
 	 * @throws java.time.temporal.UnsupportedTemporalTypeException for "exotic" units
	 */
	public static Instant getNextAlignedIntervalStart(final Instant base, final long factor, final TemporalUnit unit, final ZoneId timeZone) {
		return previousOrNextAlignedIntervalStart(base, factor, unit, timeZone, false);
	}
	
	/**
	 * Add the specified amount of time to an instant, using the system default time zone, if required.
	 * @param base
	 * @param factor
	 * @param unit
	 * @return
	 */
	public static Instant add(final Instant base, final long factor, final TemporalUnit unit) {
		return add(base, factor, unit, null);
	}

	/**
	 * Add the specified amount of time to an instant.
	 * @param base
	 * @param factor
	 * @param unit
	 * @param timeZone
	 * @return
	 */
	public static Instant add(final Instant base, final long factor, final TemporalUnit unit, ZoneId timeZone) {
		if (!unit.isDateBased())
			return base.plus(factor, unit);
		if (timeZone == null)
			timeZone = ZoneId.systemDefault();
		return ZonedDateTime.ofInstant(base, timeZone)
			.plus(factor, unit)
			.toInstant();
	}
	
	/**
 	 * Subtract the specified amount of time from an instant, using the system default time zone, if required.
	 * @param base
	 * @param factor
	 * @param unit
	 * @return
	 */
	public static Instant subtract(final Instant base, final long factor, final TemporalUnit unit) {
		return subtract(base, factor, unit, null);
	}
	
	/**
	 * Subtract the specified amount of time from an instant.
	 * @param base
	 * @param factor
	 * @param unit
	 * @param timeZone
	 * @return
	 */
	public static Instant subtract(final Instant base, final long factor, final TemporalUnit unit, ZoneId timeZone) {
		if (!unit.isDateBased())
			return base.minus(factor, unit);
		if (timeZone == null)
			timeZone = ZoneId.systemDefault();
		return ZonedDateTime.ofInstant(base, timeZone)
			.minus(factor, unit)
			.toInstant();
	}
	
	
	private static Instant previousOrNextAlignedIntervalStart(final Instant base, final long factor, final TemporalUnit unit, 
			ZoneId timeZone, final boolean previousOrNext) {
		Objects.requireNonNull(base);
		Objects.requireNonNull(unit);
		final long alignmentBase = getDefaultAlignmentFactor(unit);
		final boolean isAligned = factor > 1 && alignmentBase % factor == 0;
		if (!unit.isDateBased()) {
			final Instant result;
			if (!isAligned)
				result = base.truncatedTo(unit);
			else  {
				final TemporalUnit alignment = DEFAULT_ALIGNMENT_UNITS.get(unit);
				Instant lastCand = base.truncatedTo(alignment);
				Instant cand = lastCand;
				while (cand.compareTo(base) <= 0) {
					lastCand = cand;
					cand = cand.plus(factor, unit);
				}
				result = lastCand;
			}
			if (previousOrNext || result.compareTo(base) == 0)
				return result;
			return result.plus(factor, unit);
		}
		else {
			if (timeZone == null)
				timeZone = ZoneId.systemDefault();
			final ZonedDateTime baseZdt = ZonedDateTime.ofInstant(base, timeZone);
			final ZonedDateTime result;
			if (!isAligned) 
				result = truncateToDateBasedUnit(baseZdt, unit);
			else {
				final TemporalUnit alignment = DEFAULT_ALIGNMENT_UNITS.get(unit);
				ZonedDateTime cand = truncateToDateBasedUnit(baseZdt, alignment);
				ZonedDateTime lastCand = cand;
				while (cand.compareTo(baseZdt) <= 0) {
					lastCand = cand;
					cand = cand.plus(factor, unit);
				}
				result = lastCand;
			}
			if (previousOrNext || result.toInstant().compareTo(base) == 0)
				return result.toInstant();
			return result.plus(factor, unit).toInstant();
		}
	}
	
	private static long getDefaultAlignmentFactor(final TemporalUnit unit) {
		return DEFAULT_ALIGNMENT_FACTORS.getOrDefault(unit, 1L);
	}

	/**
	 * @param zdt
	 * @param unit
	 * @return
	 * @throws java.time.temporal.UnsupportedTemporalTypeException
	 */
	private static ZonedDateTime truncateToDateBasedUnit(ZonedDateTime zdt, TemporalUnit unit) {
		zdt = zdt.truncatedTo(ChronoUnit.DAYS);
		if (unit.equals(ChronoUnit.DAYS))
			return zdt;
		if (unit.equals(ChronoUnit.WEEKS))
			return zdt.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		if (unit.equals(ChronoUnit.MONTHS))
			return zdt.with(TemporalAdjusters.firstDayOfMonth());
		if (unit.equals(ChronoUnit.YEARS))
			return zdt.with(TemporalAdjusters.firstDayOfYear());
		if (unit.equals(ChronoUnit.DECADES)) {
			zdt = zdt.with(TemporalAdjusters.firstDayOfYear());
			final int years = zdt.getYear() % 10;
			return years == 0 ? zdt : zdt.minus(years, ChronoUnit.YEARS);
		}
		if (unit.equals(ChronoUnit.CENTURIES)) {
			zdt = zdt.with(TemporalAdjusters.firstDayOfYear());
			final int years = zdt.getYear() % 100;
			return years == 0 ? zdt : zdt.minus(years, ChronoUnit.YEARS);
		}
		return zdt.truncatedTo(unit); // likely throws exception
	}
	
}
