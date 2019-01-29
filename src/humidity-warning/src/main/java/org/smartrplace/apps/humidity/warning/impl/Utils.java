/**
 * ﻿Copyright 2019 Smartrplace UG
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
package org.smartrplace.apps.humidity.warning.impl;

import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.PhysicalElement;

import de.iwes.widgets.api.services.NameService;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class Utils {
	
	private final static double a = 7.5;
	private final static double b = 237.3;

	public final static String getName(final PhysicalElement device, final NameService nameService, final OgemaLocale locale) {
		String name0 = null;
		if (nameService != null) {
			name0 = nameService.getName(device, locale, false, true);
			if (name0 == null) {
				Resource parent = getParentSecure(device);
				while (parent != null) {
					if (parent instanceof PhysicalElement) {
						name0 = nameService.getName(parent, locale, false, true);
						if (name0 != null)
							break;
					}
					parent = getParentSecure(parent);
				}
			}
		}
		if (name0 == null)
			name0 = device.name().isActive() ? device.name().getValue() : device.getLocation();
		return name0.length() < 50 ? name0 : name0.substring(0, 50); 
	}	
	
	/**
	 * @param t temperature in C
	 * @param h relative humidity (0..1)
	 * @return dew point in C
	 */
	public static float calculateDewPoint(final float t, final float h) {
		if (!Float.isFinite(t) || !Float.isFinite(h))
			return Float.NaN;
		final double log = Math.log10(h);
		final double tempFactor = b + t;
		return (float) (b * (a*t + tempFactor * log) / (a*b - tempFactor * log));
	}
	
	private static Resource getParentSecure(final Resource r) {
		try {
			return r.getParent();
		} catch (SecurityException e) {
			return null;
		}
	}
	
}
