/**
 * Copyright 2018 Smartrplace UG
 *
 * Schedule management is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Schedule management is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartrplace.tools.schedule.management.imports;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.DisplayTemplate;
import de.iwes.widgets.template.LabelledItem;

public interface DataGenerator extends LabelledItem {

	public static final DisplayTemplate<LabelledItem> TEMPLATE = new DisplayTemplate<LabelledItem>() {

		@Override
		public String getId(LabelledItem generator) {
			return generator.id();
		}

		@Override
		public String getLabel(LabelledItem generator, OgemaLocale locale) {
			return generator.label(locale);
		}
	};
	
}
