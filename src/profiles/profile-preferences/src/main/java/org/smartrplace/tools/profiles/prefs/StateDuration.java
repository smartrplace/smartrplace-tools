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
package org.smartrplace.tools.profiles.prefs;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;

public class StateDuration {

	private final State state;
	private final int duration;
	private final ChronoUnit unit;
	
	public StateDuration(State state, int duration, ChronoUnit unit) {
		this.state = Objects.requireNonNull(state);
		this.duration = Objects.requireNonNull(duration);
		this.unit = Objects.requireNonNull(unit);
	}

	public int getDuration() {
		return duration;
	}
	
	public ChronoUnit getUnit() {
		return unit;
	}
	
	public State getState() {
		return state;
	}

	public static StateDuration deserialize(org.smartrplace.tools.profiles.prefs.model.StateDuration resource, final ProfileTemplate template) {
		final State state = template.states().stream()
				.filter(st -> resource.stateId().getValue().equals(st.id()))
				.findAny().orElse(null);
		if (state == null)
			return null;
		final int dur = resource.duration().getValue();
		if (dur <= 0)
			return null;
		try {
			final ChronoUnit unit = ChronoUnit.valueOf(resource.timeUnit().getValue());
			return new StateDuration(state, dur, unit);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public static void serialize(final List<StateDuration> durations, 
			final ResourceList<org.smartrplace.tools.profiles.prefs.model.StateDuration> baseResource, final ResourceTransaction trans) {
		trans.create(baseResource);
		final String baseName = baseResource.getName();
		for (int i = 0; i<durations.size(); i++) {
			final StateDuration obj = durations.get(i);
			final org.smartrplace.tools.profiles.prefs.model.StateDuration sub = baseResource.getSubResource(baseName + "_" + i, org.smartrplace.tools.profiles.prefs.model.StateDuration.class);
			trans.create(sub);
			trans.setInteger(sub.duration(), obj.getDuration());
			trans.setString(sub.stateId(), obj.state.id());
			trans.setString(sub.timeUnit(), obj.unit.name());
		}
	}
	
}
