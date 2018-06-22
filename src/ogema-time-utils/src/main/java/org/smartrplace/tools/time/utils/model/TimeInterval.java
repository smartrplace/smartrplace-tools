package org.smartrplace.tools.time.utils.model;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;

/**
 * A persistent time interval, based on a {@link TemporalUnit} (of type {@link ChronoUnit})
 * and a factor.
 */
public interface TimeInterval extends Resource { 

	/**
	 * Must correspond to an enum of {@link ChronoUnit}.
	 * @return
	 */
	StringResource chronoUnit();
	IntegerResource factor();
	
	/**
	 * Note: this activates this resource and its subresources.
	 * @param factor
	 * @param unit
	 */
	default void setValue(int factor, ChronoUnit unit) {
		setValue(factor, unit, true);
	}
	
	/**
	 * @param factor
	 * @param unit
	 * @param activate
	 */
	default void setValue(int factor, ChronoUnit unit, boolean activate) {
		Objects.requireNonNull(unit);
		this.chronoUnit().setValue(unit.name());
		this.factor().setValue(factor);
		if (activate)
			this.activate(true);
	}
	
	/**
	 * @return
	 * @throws IllegalArgumentException if the value of the {@link #chronoUnit()} subresource does not
	 * 		correspond to one of the {@link ChronoUnit} enums. 
	 */
	default TemporalAmount getTemporalAmount() {
		if (!chronoUnit().isActive() || !factor().isActive())
			return null;
		final ChronoUnit unit = ChronoUnit.valueOf(chronoUnit().getValue());
		final int factor = factor().getValue();
		if (!unit.isDateBased())
			return Duration.of(factor, unit);
		else { // XXX no standard method for this?
			switch (unit) {
			case DAYS:
				return Period.ofDays(factor);
			case WEEKS:
				return Period.ofWeeks(factor);
			case MONTHS:
				return Period.ofMonths(factor);
			case YEARS:
				return Period.ofYears(factor);
			case DECADES:
				return Period.ofYears(10 * factor);
			case CENTURIES:
				return Period.ofYears(100 * factor);
			default:
				throw new IllegalArgumentException("Unit " + unit + " not supported");
			}
		}
	}
	
}
