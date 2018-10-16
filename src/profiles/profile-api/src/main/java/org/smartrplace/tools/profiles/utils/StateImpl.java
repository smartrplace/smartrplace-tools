package org.smartrplace.tools.profiles.utils;

import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.smartrplace.tools.profiles.State;

public class StateImpl extends LabelledItemImpl implements State {

	public static final StateImpl ON = new StateImpl("on", SingleSwitchBox.class, "an", "on");
	public static final StateImpl OFF = new StateImpl("off", SingleSwitchBox.class, "aus", "off");
	
	private final Class<?> type;
	
	public StateImpl(String id, Class<?> type, String label_de, String label_en) {
		super(id, label_de, label_en);
		this.type = type;
	}

	@Override
	public Class<?> typeInfo() {
		return type;
	}
	
	@Override
	public String toString() {
		return "StateImpl[" + id() + "]";
	}
	
}
