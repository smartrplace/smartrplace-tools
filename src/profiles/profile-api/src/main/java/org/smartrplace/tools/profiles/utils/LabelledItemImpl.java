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
package org.smartrplace.tools.profiles.utils;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

public class LabelledItemImpl implements LabelledItem {

	private final String id;
	private final String label_de;
	private final String label_en;
	
	public LabelledItemImpl(String id, String label_de, String label_en) {
		this.id = id;
		this.label_de = label_de;
		this.label_en = label_en;
	}
	
	@Override
	public String id() {
		return id;
	}

	@Override
	public String label(OgemaLocale locale) {
		return locale == OgemaLocale.GERMAN ? label_de : label_en;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		return id.equals(((LabelledItem) obj).id());
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
}
