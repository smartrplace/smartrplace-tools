package org.smartrplace.tools.profiles.utils;

import org.smartrplace.tools.profiles.DataPoint;

public class DataPointImpl extends LabelledItemImpl implements DataPoint {
	
	private final boolean optional;
	private final DataType dataType;
	private final Class<?> type;

	public DataPointImpl(String id, Class<?> type, DataType dataType, boolean optional, String label_de, String label_en) {
		super(id, label_de, label_en);
		this.optional = optional;
		this.dataType = dataType;
		this.type = type;
	}

	@Override
	public boolean optional() {
		return optional;
	}

	@Override
	public DataType dataType() {
		return dataType; 
	}
	
	@Override
	public Class<?> typeInfo() {
		return type;
	}
	
	@Override
	public String toString() {
		return "DataPointImpl[" + id() + "]";
	}
	
	
}
