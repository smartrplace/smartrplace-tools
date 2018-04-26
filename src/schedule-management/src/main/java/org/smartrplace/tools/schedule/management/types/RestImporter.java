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
package org.smartrplace.tools.schedule.management.types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.fileupload.FileItem;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.serialization.JaxbResource;
import org.ogema.serialization.jaxb.ScheduleResource;
import org.ogema.tools.impl.ResourceSerializer;
import org.smartrplace.tools.schedule.management.persistence.FileBasedPersistence;
import org.smartrplace.tools.schedule.management.serialization.JaxbSchedule;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

// FIXME serialization not working yet
public class RestImporter implements FileBasedPersistence {
	
	private static SoftReference<JAXBContext> unmarshallingContext = new SoftReference<JAXBContext>(null);
	private static SoftReference<ObjectMapper> jsonMapper = new SoftReference<ObjectMapper>(null);
	private static SoftReference<JAXBContext> marshallingContext = new SoftReference<JAXBContext>(null);
	
	static synchronized JAXBContext getUnmarshallingContext() {
		JAXBContext context = unmarshallingContext.get();
		if (context == null) {
			try {
				// XXX?
				context = JAXBContext.newInstance("org.ogema.serialization.jaxb",
						org.ogema.serialization.jaxb.Resource.class.getClassLoader());
			} catch (JAXBException e) {
				throw new RuntimeException(e);
			}
			unmarshallingContext = new SoftReference<JAXBContext>(context);
		}
		return context;
	}
	
	static synchronized JAXBContext getMarshallingContext() {
		JAXBContext context = marshallingContext.get();
		if (context == null) {
			try {
				// XXX ?
				context = JAXBContext.newInstance("org.ogema.serialization", JaxbResource.class.getClassLoader());
			} catch (JAXBException ex) {
				throw new RuntimeException(ex);
			}
			marshallingContext = new SoftReference<JAXBContext>(context);
		}
		return context;
	}	
	
	static synchronized ObjectMapper getJsonMapper() {
		ObjectMapper mapper = jsonMapper.get();
		if (mapper == null) {
			AnnotationIntrospector spec = AnnotationIntrospector.pair(new JacksonAnnotationIntrospector(),
					new com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
			mapper = new ObjectMapper();
			SimpleModule testModule = new SimpleModule("ScheduleManagement", new com.fasterxml.jackson.core.Version(1, 0, 0, null,null,null));
			testModule.addSerializer(new ResourceSerializer(mapper));
			mapper.registerModule(testModule);
			mapper.setAnnotationIntrospector(spec);
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true); // TODO configurable
			jsonMapper = new SoftReference<ObjectMapper>(mapper);
		}
		return mapper;
	}
	
	@Override
	public String id() {
		return "restimporter";
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Serialized schedule";
	}

	@Override
	public String description(OgemaLocale locale) {
		return "Load an xml or json representation of an OGEMA timeseries";
	}
	
	@Override
	public String supportedFileFormat() {
		return "json, xml, ogx";
	}
	
	@Override
	public String defaultOptionString() {
		return "xml";
	}
	
	@Override
	public String optionDescription(OgemaLocale locale) {
		return "Format (\"json\" or \"xml\")";
	}

	@Override
	public String checkOptionsString(String options, OgemaLocale locale) {
		if (options == null || (!options.trim().toLowerCase().equals("json") && !options.trim().toLowerCase().equals("xml"))) 
			return "Options string must be either \"json\" or \"xml\".";
		return null;
	}
	
	@Override
	public List<SampledValue> parseFile(FileItem file, Class<?> type, String separator) throws IOException {
		return parseFile(file, type, separator, Long.MIN_VALUE, Long.MAX_VALUE);
	}
	
	// TODO evaluate start and end time, and type
	@SuppressWarnings("rawtypes") 
	@Override
	public List<SampledValue> parseFile(FileItem file, Class<?> type, String options, long start, long end) throws IOException {
		options = options.trim().toLowerCase();
		final List<SampledValue> values = new ArrayList<>();
		final org.ogema.serialization.jaxb.Resource result;
		try (final Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
			switch (options) {
			case "json":
				result = getJsonMapper().readValue(reader, org.ogema.serialization.jaxb.Resource.class);
				break;
			case "xml":
				result = (org.ogema.serialization.jaxb.Resource) ((JAXBElement) getUnmarshallingContext().createUnmarshaller().unmarshal(reader)).getValue();
				break;
			default:
				throw new IllegalArgumentException("Invalid option " + options + ", must be either \"json\" or \"xml\".");
			}
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		if (!(result instanceof ScheduleResource)) 
			throw new IllegalArgumentException("Uploaded XML/JSON file does not contain a schedule; got instead " + result.getClass().getSimpleName());
		ScheduleResource schedule = (ScheduleResource) result;
		for (org.ogema.serialization.jaxb.SampledValue svSer: schedule.getEntry()) {
			values.add(new SampledValue(svSer.createOgemaValue(), svSer.getTime(), svSer.getQuality()));
		}
		return values;
	}
	
	@Override
	public String getFileEnding(String options) {
		return options;
	}
	
	@Override
	public void generate(ReadOnlyTimeSeries timeSeries, String options, Class<?> type, Writer writer) throws IOException, IllegalArgumentException {
		options = options.trim().toLowerCase();
		org.smartrplace.tools.schedule.management.serialization.JaxbSchedule<?> schedule = TypeUtils.getSchedule(timeSeries, type);
		switch (options) {
		case "xml":
			try {
				JAXBElement<?> el = new JAXBElement<>(new QName(JaxbResource.NS_OGEMA_REST, "resource", "og"), JaxbSchedule.class, schedule);
				getMarshallingContext().createMarshaller().marshal(el, writer);
			} catch (JAXBException e) {
				throw new RuntimeException(e);
			}
			break;
		case "json":
			getJsonMapper().writeValue(writer, schedule);
			break;
		default:
			throw new IllegalArgumentException("invalid type " + options + ", must be \"xml\" or \"json\"");
		}
		writer.flush();
	}
	
}
