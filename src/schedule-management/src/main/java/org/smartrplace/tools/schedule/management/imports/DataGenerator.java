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
