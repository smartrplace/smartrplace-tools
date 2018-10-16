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
