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
/**
 * Copyright 2018 Smartrplace UG
 *
 * FendoDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FendoDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartrplace.rest.timeseries;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ogema.core.administration.FrameworkClock;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.core.timeseries.TimeSeries;
import org.ogema.recordeddata.RecordedDataStorage;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.logging.fendodb.stats.StatisticsService;
import org.smartrplace.logging.fendodb.tools.FendoDbTools;
import org.smartrplace.logging.fendodb.tools.config.FendodbSerializationFormat;
import org.smartrplace.logging.fendodb.tools.config.SerializationConfiguration;
import org.smartrplace.logging.fendodb.tools.config.SerializationConfigurationBuilder;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.widgets.html.selectiontree.LinkingOption;
import de.iwes.widgets.html.selectiontree.SelectionItem;
import de.iwes.widgets.html.selectiontree.TerminalOption;

@SuppressWarnings("deprecation")
@Component(
		service=Servlet.class,
		property=HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=" + TimeseriesServlet.ALIAS
)
public class TimeseriesServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(TimeseriesServlet.class);
	private static final String[] EMPTY_OPTS = new String[0];
    private static final long serialVersionUID = 1L;
    static final String ALIAS = "/rest/timeseries";

    private final static String[] JSON_FORMATS = {
    	"application/json",
    	"application/x-ldjson",
    	"application/x-json-stream",
    	"application/ld-json",
    	"application/x-ndjson"
    };

    private final static String[] XML_FORMATS = {
    	"application/xml",
    	"text/xml"
    };

    @Reference(
    		cardinality=ReferenceCardinality.OPTIONAL,
    		policy=ReferencePolicy.DYNAMIC,
    		policyOption=ReferencePolicyOption.GREEDY
    )
    private volatile FrameworkClock clock;
    
    private final ConcurrentMap<String, ComponentServiceObjects<DataProvider<?>>> dataProviders = new ConcurrentHashMap<>();
 
    @Reference(
    		service=DataProvider.class,
    		cardinality=ReferenceCardinality.MULTIPLE,
    		policy=ReferencePolicy.DYNAMIC,
    		bind="addProvider",
    		unbind="removeProvider"
	)
    protected void addProvider(final ComponentServiceObjects<DataProvider<?>> service) {
    	final String providerId = getProviderId(service);
    	if (providerId == null) {
    		final DataProvider<?> provider = service.getService();
    		logger.warn("Data provider without id: {}", provider);
    		service.ungetService(provider);
    		return;
    	}
    	final ComponentServiceObjects<DataProvider<?>> existing = dataProviders.putIfAbsent(providerId, service);
    	if (existing != null) {
    		final DataProvider<?> provider = service.getService();
    		logger.warn("Duplicate data provider id {}: {}", providerId, provider);
    		service.ungetService(provider);
    		return;
    	}
    }
    
    protected void removeProvider(final ComponentServiceObjects<DataProvider<?>> service) {
    	final String providerId = getProviderId(service);
    	if (providerId != null)
    		dataProviders.remove(providerId, service);
    }
    
    @Reference
    private ComponentServiceObjects<StatisticsService> statistics;

    
    private static String getProviderId(final ComponentServiceObjects<DataProvider<?>> service) {
    	final Object prop = service.getServiceReference().getProperty("provider-id");
    	if (prop instanceof String)
    		return (String) prop;
    	return useService(service, provider -> provider.id());
    }
    
    private static <R> R useService(final ComponentServiceObjects<DataProvider<?>> service, final Function<DataProvider<?>, R> consumer) {
    	final DataProvider<?> provider = service.getService();
    	try {
    		return consumer.apply(provider);
    	} finally {
    		service.ungetService(provider);
    	}
    }
    
    /**
     * 
     * @param providerId
     * @param consumer
     * @return
     * @throws NoSuchElementException if data provider is not found
     */
    private <R> R useService(final String providerId, final Function<DataProvider<?>, R> consumer) {
    	final ComponentServiceObjects<DataProvider<?>> service= dataProviders.get(providerId);
    	if (service == null)
    		throw new NoSuchElementException();
    	return useService(service, consumer);
    }
    
    // 
    private static Map<ReadOnlyTimeSeries, String>  extractTimeSeries(final HttpServletRequest req, final HttpServletResponse resp, final DataProvider<?> provider) throws IOException {
		final LinkingOption[] opts = provider.selectionOptions();
		if (opts == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Provider does not provide selection options");
			return null;
		}
    	final String[] options0 = req.getParameterValues(Parameters.PARAM_SELECTION_OPTION);
    	final String[] options = options0 != null ? options0  :EMPTY_OPTS;
		final Map<LinkingOption, Collection<SelectionItem>> items = new HashMap<>(opts.length); 
		final List<SelectionItem> lastItems = Arrays.stream(opts)
			.map(opt -> {
				final LinkingOption[] deps = opt.dependencies();
				final List<Collection<SelectionItem>> relevantItems = deps == null ? null : Arrays.stream(deps)
						.map(items::get)
						.map(collection -> collection != null ? collection : Collections.<SelectionItem> emptyList())
						.collect(Collectors.toList());
				final List<SelectionItem> selItems = opt.getOptions(relevantItems);
				final String id = opt.id();
				List<SelectionItem> selectedItems = Arrays.stream(options)
					.filter(o -> o.startsWith(id + ":"))
					.map(o -> selItems.stream().filter(item -> item.id().equalsIgnoreCase(o.substring(id.length() + 1))).findAny().orElse(null))
					.filter(it -> it != null)
					.collect(Collectors.toList());
				if (selectedItems.isEmpty() && opt instanceof TerminalOption<?>) {
					selectedItems = selItems;
				}
				items.put(opt, selectedItems);
				return selectedItems;
			}).reduce((a,b) -> b).orElse(null);
		if (lastItems == null)
			return null;
		final TerminalOption<? extends ReadOnlyTimeSeries> terminal = provider.getTerminalOption();
		return lastItems.stream()
				.collect(Collectors.toMap(terminal::getElement, SelectionItem::id));
    }
    
    // entry time series -> id
    private Map.Entry<ReadOnlyTimeSeries, String> extractSingleTimeseries(final HttpServletRequest req, final HttpServletResponse resp, final DataProvider<?> provider) throws IOException {
    	final Map<ReadOnlyTimeSeries, String> list = extractTimeSeries(req, resp, provider);
    	if (list == null)
    		return null;
    	if (list.isEmpty()) {
    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Timeseries not found");
    		return null;
    	}
    	return list.entrySet().iterator().next();
    }
    
    private Map<ReadOnlyTimeSeries, String> getMultipleTimeseries(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    	final String providerId = req.getParameter(Parameters.PARAM_PROVIDER);
    	if (providerId == null) {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Provider id missing");
    		return null;
    	}
    	ComponentServiceObjects<DataProvider<?>> service = dataProviders.get(providerId);
    	if (service == null) {
    		service = dataProviders.entrySet().stream()
    				.filter(entry -> entry.getKey().equalsIgnoreCase(providerId))
    				.map(entry -> entry.getValue())
    				.findAny().orElse(null);
    	}
    	if (service == null) {
    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Provider " + providerId + " not found");
    		return null;
    	}
    	final DataProvider<?> provider = service.getService();
    	try {
    		return extractTimeSeries(req, resp, provider);
    	} finally {
    		service.ungetService(provider);
    	}
    }

    // TODO support multipart?
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	resp.setCharacterEncoding("UTF-8");
    	final String target = req.getParameter(Parameters.PARAM_TARGET);
    	if (target == null || target.trim().isEmpty()) {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Target missing");
    		return;
    	}
    	final FendodbSerializationFormat format = getFormat(req, false);
    	final String providerId = req.getParameter(Parameters.PARAM_PROVIDER);
    	final ComponentServiceObjects<DataProvider<?>> service = dataProviders.get(providerId);
    	if (service == null) {
    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Data provider " + providerId + " not found");
    		return;
		}
    	final Map.Entry<ReadOnlyTimeSeries,String> entry;
    	final DataProvider<?> provider = service.getService();
    	try {
	    	entry = extractSingleTimeseries(req, resp, provider);
    	} finally {
    		service.ungetService(provider);
    	}
    	if (entry == null) {
    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Time series not found");
    		return;
		}
    	final ReadOnlyTimeSeries timeSeries = entry.getKey();
    	if (!(timeSeries instanceof TimeSeries) && !(timeSeries instanceof RecordedDataStorage)) {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot write to time series " + timeSeries + ": read only");
    		return;
    	}
		switch (target.trim().toLowerCase()) {
		case Parameters.TARGET_VALUE:
			// {"value":12.3,"time":34}
			// <entry><value>32.3</value><time>34</time></entry>
			Deserialization.deserializeValue(req.getReader(), timeSeries, format, clock, resp);
			break;
		case Parameters.TARGET_VALUES:
			Deserialization.deserializeValues(req.getReader(), timeSeries, format, resp);
			break;
        default:
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown target " + target);
        	return;
		}
    }

     @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    	   /*

    	 resp.setCharacterEncoding("UTF-8");
    	final String databasePath = req.getParameter(Parameters.PARAM_DB);
    	if (databasePath == null || databasePath.trim().isEmpty()) {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Database id missing");
    		return;
    	}
    	final String target = req.getParameter(Parameters.PARAM_TARGET);
    	if (target == null || target.trim().isEmpty()) {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Target missing");
    		return;
    	}
    	if (target.equalsIgnoreCase("database")) {
    		try (final CloseableDataRecorder recorder = factory.getInstance(Paths.get(databasePath))) {
        		if (recorder == null) {
    	    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Database could not be created: " + databasePath);
    	    		return;
        		}
        	}
    		resp.setStatus(HttpServletResponse.SC_OK);
    		return;
    	}
    	final String id = req.getParameter(Parameters.PARAM_ID);
    	if (id == null)  {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Timeseries id missing");
    		return;
    	}
    	try (final CloseableDataRecorder recorder = factory.getExistingInstance(Paths.get(databasePath))) {
    		if (recorder == null) {
	    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Database not found: " + databasePath);
	    		return;
    		}
    		if (recorder.getConfiguration().isReadOnlyMode()) {
    			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Database opened in read-only mode: " + databasePath);
	    		return;
    		}
    		switch (target.toLowerCase()) {
        	case Parameters.TARGET_TIMESERIES: //create or update timeseries
        		final String updateMode = req.getParameter(Parameters.PARAM_UPDATE_MODE);
            	if (updateMode == null) {
            		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Update mode missing");
            		return;
            	}
            	final StorageType storageType = StorageType.valueOf(updateMode.trim().toUpperCase());
        		final RecordedDataConfiguration config = new RecordedDataConfiguration();
        		config.setStorageType(storageType);
        		if (storageType == StorageType.FIXED_INTERVAL) {
        			final String itv = req.getParameter(Parameters.PARAM_INTERVAL);
        			long interval = 60 * 1000;
        			if (itv != null) {
        				try {
        					interval = Long.parseLong(itv);
        					if (interval <= 0)
        						throw new NumberFormatException();
        				} catch (NumberFormatException e) {
        					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid update interval: " + itv);
        					return;
        				}
        			}
        			config.setFixedInterval(interval);
        		}
            	try {
            		final FendoTimeSeries ts0 = recorder.getRecordedDataStorage(id);
            		if (ts0 != null) {
            			ts0.update(config);
            		} else {
            			recorder.createRecordedDataStorage(id, config);
            		}
            	} catch (DataRecorderException e) {
            		resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            		return;
        		}
            	break;
        	case Parameters.TARGET_PROPERTIES:
        		final String[] properties = req.getParameterValues(Parameters.PARAM_PROPERTIES);
        		if (properties == null || properties.length == 0) {
        			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Properties missing");
        			return;
        		}
        		final FendoTimeSeries slots = recorder.getRecordedDataStorage(id);
        		if (slots == null) {
        			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Timeseries " + id + " not found");
        			return;
        		}
        		Arrays.stream(properties)
					.map(string -> string.split("="))
					.filter(prop -> prop.length == 2)
					.forEach(prop -> {
						final String key = prop[0].trim();
						final String value = prop[1].trim();
						if (key.isEmpty() || value.isEmpty())
							return;
						slots.addProperty(key, value);
					});
        		break;
        	default:
        		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown target: " + target);
        		return;
        	}
    	}
    	resp.setStatus(HttpServletResponse.SC_OK);
    	    */

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /*

   	resp.setCharacterEncoding("UTF-8");
    	final String databasePath = req.getParameter(Parameters.PARAM_DB);
    	if (databasePath == null || databasePath.trim().isEmpty()) {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Database id missing");
    		return;
    	}
    	final String target = req.getParameter(Parameters.PARAM_TARGET);
    	if (target == null || target.trim().isEmpty()) {
    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Target missing");
    		return;
    	}
    	try (final CloseableDataRecorder recorder = factory.getExistingInstance(Paths.get(databasePath))) {
    		if (recorder == null) {
	    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Database not found: " + databasePath);
	    		return;
    		}
        	final String id = req.getParameter(Parameters.PARAM_ID);
        	switch (target.toLowerCase()) {
        	case Parameters.TARGET_PROPERTIES:
        	case Parameters.TARGET_TAG:
        	case Parameters.TARGET_TIMESERIES:
            	if (id == null)  {
            		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Timeseries id missing");
            		return;
            	}
	    		final FendoTimeSeries timeSeries = recorder.getRecordedDataStorage(id.trim());
	    		if (timeSeries == null) {
	    			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Timeseries not found: " + id);
		    		return;
	    		}
	    		if (target.equalsIgnoreCase("timeseries")) {
		    		if (!recorder.deleteRecordedDataStorage(id)) {
		    			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Delete operation failed for " + id);
		    			return;
		    		}
	    		} else if (target.equalsIgnoreCase("tag")) {
	    			final String tag = req.getParameter(Parameters.PARAM_TAGS);
	    			if (tag == null) {
	    				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tag missing");
			    		return;
	    			}
	    			if (!timeSeries.removeProperty(tag)) {
	    				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Tag " + tag + " not found on timeseries " + id);
	    				return;
	    			}
	    		} else if (target.equalsIgnoreCase("properties")) {
	    			final String[] properties = req.getParameterValues(Parameters.PARAM_PROPERTIES);
	        		if (properties == null || properties.length == 0) {
	        			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Properties missing");
	        			return;
	        		}
	        		final AtomicBoolean anyFound = new AtomicBoolean(false);
	        		Arrays.stream(properties)
						.map(string -> string.split("="))
						.filter(prop -> prop.length == 2)
						.forEach(prop -> {
							final String key = prop[0].trim();
							final String value = prop[1].trim();
							if (key.isEmpty() || value.isEmpty())
								return;
							if (timeSeries.removeProperty(key, value))
								anyFound.set(true);
						});
	        		if (!anyFound.get()) {
	        			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "None of the specified properties found on timeseries " + id);
	    				return;
	        		}
	    		}
	    		break;
    		case Parameters.TARGET_DATA:
	    		final Long start = Utils.parseTimeString(req.getParameter(Parameters.PARAM_START), null);
	    		final Long end = Utils.parseTimeString(req.getParameter(Parameters.PARAM_END), null);
	    		if (start == null && end == null) {
	    			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No start or end time specified");
	    			return;
	    		}
	    		boolean result2 = true;
    			if (start != null)
    				result2 = recorder.deleteDataBefore(Instant.ofEpochMilli(start));
    			if (end != null)
    				result2 = result2 && recorder.deleteDataAfter(Instant.ofEpochMilli(end));
    			if (!result2) {
	    			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Delete operation failed");
	    			return;
    			}
    			break;
        	default:
        		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown target: " + target);
        		return;
        	}
			resp.setStatus(HttpServletResponse.SC_OK);
    	}
    	    */

    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    	final String providerId = req.getParameter(Parameters.PARAM_PROVIDER);
    	resp.setCharacterEncoding("UTF-8");
    	final FendodbSerializationFormat format = getFormat(req, true);
    	if (providerId == null || providerId.trim().isEmpty()) {
    		outputDatabaseInstances(resp, getFormat(req, true));
    		setContent(resp, format);
    		resp.setStatus(HttpServletResponse.SC_OK);
        	return;
    	}
    	int idt = 4;
    	final String indent = req.getParameter(Parameters.PARAM_INDENT);
        if (indent != null) {
         	try {
         		idt = Integer.parseInt(indent);
         	} catch (NumberFormatException e) {
         		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid indentation: " + indent);
         		return;
         	}
        }
        final char[] indentation = idt >= 0 ? new char[idt] : new char[0];
        Arrays.fill(indentation, ' ');
        final char[] lineBreak = idt >= 0 ? new char[0] : new char[] {'\n'};
        final DateTimeFormatter formatter;
        final String dtFormatter = req.getParameter(Parameters.PARAM_DT_FORMATTER);
        if (dtFormatter != null)
        	formatter = DateTimeFormatter.ofPattern(dtFormatter);
        else
        	formatter = null;
    	String target = req.getParameter(Parameters.PARAM_TARGET);
    	if (target == null)
    		target = Parameters.TARGET_FIND;
    	else
    		target = target.trim().toLowerCase();
    	final ComponentServiceObjects<DataProvider<?>> service = dataProviders.get(providerId);
    	if (service == null) {
    		resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Data provider " + providerId + " not found");
    		return;
		}
    	final DataProvider<?> provider = service.getService();
    	try {
            switch (target) {
            case Parameters.TARGET_DATA:
            	printTimeseriesData(req, resp, provider, format, formatter);
            	break;
            case Parameters.TARGET_NEXT: // fallthrough
            case Parameters.TARGET_PREVIOUS:
            	final boolean nextOrPrevious = target.equals("nextvalue");
            	final Map.Entry<ReadOnlyTimeSeries, String> entry = extractSingleTimeseries(req, resp, provider);
            	if (entry == null)
            		return;
            	final ReadOnlyTimeSeries ts = entry.getKey();
            	final String timestamp = req.getParameter(Parameters.PARAM_TIMESTAMP);
            	if (timestamp == null) {
            		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Timestamp missing");
            		return;
            	}
            	final Long t = Utils.parseTimeString(timestamp, null);
            	if (t == null) {
             		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Timestamp missing");
            		return;
            	}
            	final SampledValue sv = nextOrPrevious ? ts.getNextValue(t) : ts.getPreviousValue(t);
            	final String result = Utils.serializeValue(sv, format, formatter, lineBreak, indentation);
            	resp.getWriter().write(result);
            	break;
            case Parameters.TARGET_SIZE:
            	final Map.Entry<ReadOnlyTimeSeries, String> entry2 = extractSingleTimeseries(req, resp, provider);
            	if (entry2 == null)
            		return;
            	final ReadOnlyTimeSeries tsb = entry2.getKey();
            	final long start = Utils.parseTimeString(req.getParameter(Parameters.PARAM_START), Long.MIN_VALUE);
                final long end = Utils.parseTimeString(req.getParameter(Parameters.PARAM_END), Long.MAX_VALUE);
                final int size = tsb.size(start, end);
                printSize(resp.getWriter(), entry2.getValue(), lineBreak, indentation, size, format);
                break;
            case Parameters.TARGET_FIND:
            case Parameters.TARGET_STATISTICS:
            	findTimeseries(target, req, resp, provider, format);
            	break;
            default:
            	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown target " + target);
            	return;
            }
            setContent(resp, format);
    	} finally {
    		service.ungetService(provider);
    	}
    	resp.setStatus(HttpServletResponse.SC_OK);
    }

    private static void setContent(final HttpServletResponse resp, final FendodbSerializationFormat format) {
    	resp.setContentType(format == FendodbSerializationFormat.XML ? "application/xml" :
        	format == FendodbSerializationFormat.JSON ? "application/json" : "text/csv");
    }

    /**
     * @param target
     * 		either "find" or "stats"
     * @param req
     * @param resp
     * @throws IOException
     */
    private final void findTimeseries(final String target, final HttpServletRequest req, final HttpServletResponse resp,
    		final DataProvider<?> provider, final FendodbSerializationFormat format) throws IOException {
    	switch (target) {
    	case Parameters.TARGET_FIND:
    		final Collection<String> timeSeriesIds = extractTimeSeries(req, resp, provider).values();
    		serializeStrings(resp, format, timeSeriesIds, "timeSeries");
    		return;
    	case Parameters.TARGET_STATISTICS:
    		final String[] providers0 = req.getParameterValues(Parameters.PARAM_PROVIDERS);
    		if (providers0 == null || providers0.length == 0) {
    			serializeStrings(resp, format, extractTimeSeries(req, resp, provider).values(), "timeSeries");
    			return;
    		}
       		final List<ReadOnlyTimeSeries> matches = extractTimeSeries(req, resp, provider).keySet().stream()
       				.collect(Collectors.toList());
    		final List<String> providerIds = Arrays.asList(providers0);
    		final Long start = Utils.parseTimeString(req.getParameter(Parameters.PARAM_START), null);
    		final Long end = Utils.parseTimeString(req.getParameter(Parameters.PARAM_END), null);
    		final Map<String,?> results;
    		final StatisticsService stats = statistics.getService();
    		try {
	    		if (start == null || end == null)
	    			results = stats.evaluateByIds(matches, providerIds);
	    		else
	    			results = stats.evaluateByIds(matches, providerIds, start, end);
    		} finally {
    			statistics.ungetService(stats);
    		}
	    	serializeMap(resp, format, results, "statistics");
    	}
    }

    private static void printSize(final Writer writer, final String id, final char[] lineBreak, final char[] indentation, final int size,
    		final FendodbSerializationFormat format) throws IOException {
    	switch (format) {
        case XML:
        	writer.write("<entry>");
        	writer.write(lineBreak);
        	writer.write(indentation);
        	writer.write("<id>");
        	writer.write(id);
        	writer.write("</id>");
        	writer.write(lineBreak);
        	writer.write(indentation);
        	writer.write("<size>");
        	writer.write(size + "");
        	writer.write("</size>");
        	writer.write(lineBreak);
        	writer.write(indentation);
        	writer.write("</entry>");
        	break;
        case JSON:
        	writer.write('{');
        	writer.write(lineBreak);
        	writer.write(indentation);
        	writer.write("\"id\":\"");
        	writer.write(id);
        	writer.write('\"');
        	writer.write(',');
        	writer.write(lineBreak);
        	writer.write(indentation);
        	writer.write("\"size\":");
        	writer.write(size+ "");
        	writer.write(lineBreak);
        	writer.write('}');
        	break;
        case CSV:
        	writer.write("id:");
        	writer.write(id);
        	writer.write('\n');
        	writer.write("size:");
        	writer.write(size+ "");
    	}
    }

    private static void printTimeseriesData(final HttpServletRequest req, final HttpServletResponse resp,
    		final DataProvider<?> provider, final FendodbSerializationFormat format,
    		final DateTimeFormatter formatter) throws IOException, ServletException {
    	// TODO use find instead
//   		String id = req.getParameter(Parameters.PARAM_ID);
//    	if (id == null || id.trim().isEmpty()) {
//        	outputTimeseriesIds(req, resp, provider, format);
//        	return;
//        }
        final Map<ReadOnlyTimeSeries,String> list = extractTimeSeries(req, resp, provider);
        if (list == null)
        	return;
        if (list.isEmpty()) {
        	resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Timeseries not found");
        	return;
        }
        final ReadOnlyTimeSeries ts = list.keySet().iterator().next();
    	final long start = Utils.parseTimeString(req.getParameter(Parameters.PARAM_START), Long.MIN_VALUE);
        final long end = Utils.parseTimeString(req.getParameter(Parameters.PARAM_END), Long.MAX_VALUE);
        final String samplingIntervalStr = req.getParameter(Parameters.PARAM_INTERVAL);
        final Long samplingInterval;
        try {
        	samplingInterval = samplingIntervalStr == null? null : Long.parseLong(samplingIntervalStr);
        } catch (NumberFormatException e) {
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Interval " + samplingIntervalStr + " is not a valid number");
        	return;
        }
        final String maxValuesStr = req.getParameter(Parameters.PARAM_MAX);
        final int maxValues;
        try {
        	maxValues = maxValuesStr == null? 10000 : Integer.parseInt(maxValuesStr);
        } catch (NumberFormatException e) {
        	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid maximum nr argument " + maxValuesStr);
        	return;
        }
        final SerializationConfigurationBuilder builder = SerializationConfigurationBuilder.getInstance()
        		.setInterval(start, end)
        		.setFormat(format)
        		.setFormatter(formatter)
        		.setSamplingInterval(samplingInterval)
        		.setMaxNrValues(maxValues);
        final String indent = req.getParameter(Parameters.PARAM_INDENT);
        if (indent != null) {
        	try {
        		final int i = Integer.parseInt(indent);
        		if (i < 0)
        			builder.setPrettyPrint(false);
        		else
        			builder.setIndentation(i);
        	} catch (NumberFormatException e) {
        		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid indentation: " + indent);
        		return;
        	}
        }
        final SerializationConfiguration config = builder.build();
        final int nrDataPoints = FendoDbTools.serialize(resp.getWriter(), ts, config);
        resp.setHeader("nrdatapoints", nrDataPoints + "");
    }

    private void outputDatabaseInstances(final HttpServletResponse resp, final FendodbSerializationFormat format) throws IOException {
    	serializeStrings(resp, format, dataProviders.keySet(), "dataProviders");
    }

    private static void outputTimeseriesIds(final HttpServletRequest req, final HttpServletResponse resp, DataProvider<?> provider,
    		final FendodbSerializationFormat format) throws IOException {
    	serializeStrings(resp, format,extractTimeSeries(req, resp, provider).values(), "timeSeries");
    }

    private static void serializeStrings(final HttpServletResponse resp, final FendodbSerializationFormat format,
    		final Collection<String> strings, final String entryTag) throws IOException {
    	final PrintWriter writer = resp.getWriter();
    	switch (format) {
    	case XML:
    		writer.println("<entries>");
    		break;
    	case JSON:
    		writer.write("{\"entries\":\n");
    		writer.println('[');
    		break;
    	default:
    	}
    	boolean first = true;
        for (String id : strings) {
        	switch (format) {
        	case XML:
        		writer.write('<');
        		writer.write(entryTag);
        		writer.write('>');
        		writer.write(id);
        		writer.write('<');
        		writer.write('/');
        		writer.write(entryTag);
        		writer.write('>');
        		writer.println();
        		break;
        	case JSON:
        		if (!first) {
        			writer.write(',');
        			writer.println();
        		} else
        			first = false;
        		writer.write('\"');
        		writer.write(id);
        		writer.write('\"');
        		break;
        	default:
        		writer.println(id);
        	}

        }
        switch (format) {
    	case XML:
    		writer.write("</entries>");
    		break;
    	case JSON:
    		writer.println();
    		writer.write(']');
    		writer.write('}');
    		break;
    	default:
    	}
    }

    private static void serializeMap(final HttpServletResponse resp, final FendodbSerializationFormat format,
    		final Map<String, ?> map, final String entryTag) throws IOException {
    	final PrintWriter writer = resp.getWriter();
    	switch (format) {
    	case XML:
    		writer.println("<entries>");
    		break;
    	case JSON:
    		writer.write("{\"entries\":\n");
    		writer.println('{');
    		break;
    	default:
    	}
    	boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
        	final String id = entry.getKey();
        	final Object value = entry.getValue();
        	switch (format) {
        	case XML:
        		writer.write('<');
        		writer.write(entryTag);
        		writer.write('>');
	        		writer.write('<');
	        		writer.write("id");
	        		writer.write('>');
        				writer.write(id);
    				writer.write('<');
	        		writer.write('/');
	        		writer.write("id");
	        		writer.write('>');
	        		writer.write('<');
	        		writer.write("value");
	        		writer.write('>');
	        			writer.write(value.toString());
	        		writer.write('<');
	        		writer.write('/');
	        		writer.write("value");
	        		writer.write('>');
        		writer.write('<');
        		writer.write('/');
        		writer.write(entryTag);
        		writer.write('>');
        		writer.write('\n');
        		break;
        	case JSON:
        		if (!first) {
        			writer.write(',');
        			writer.write('\n');
        		} else
        			first = false;
        		writer.write('\"');
        		writer.write(id);
        		writer.write('\"');
        		writer.write(':');
        		final boolean isNumber = value instanceof Number; 
        		if (!isNumber)
        				writer.write('\"');
        		writer.write((isNumber && Double.isNaN(((Number) value).doubleValue())) ? "null" : value.toString());
        		if (!isNumber)
    				writer.write('\"');
        		break;
        	default:
        		writer.write(id);
        		writer.write(':');
        		writer.write(value.toString());
        		writer.write('\n');
        	}

        }
        switch (format) {
    	case XML:
    		writer.write("</entries>");
    		break;
    	case JSON:
    		writer.println();
    		writer.write('}');
    		writer.write('}');
    		break;
    	default:
    	}
    }

    private static int getJsonIndex(final String header) {
    	return Arrays.stream(JSON_FORMATS)
    		.map(format -> header.indexOf(format))
    		.filter(idx -> idx >= 0)
    		.sorted()
    		.findFirst().orElse(-1);
    }

    private static int getXmlIndex(final String header) {
    	return Arrays.stream(XML_FORMATS)
    		.map(format -> header.indexOf(format))
    		.filter(idx -> idx >= 0)
    		.sorted()
    		.findFirst().orElse(-1);
    }

    private static FendodbSerializationFormat getFormat(final HttpServletRequest req, final boolean acceptOrContentType) {
    	final String format = req.getParameter(Parameters.PARAM_FORMAT);
    	if (format != null) {
   			return FendodbSerializationFormat.valueOf(format.trim().toUpperCase());
    	}
    	final String header = req.getHeader(acceptOrContentType ? "Accept" : "Content-Type");
    	if (header == null)
    		return FendodbSerializationFormat.CSV;
    	final String accept = header.toLowerCase();
        final int returnXML = getXmlIndex(accept);
        final int returnJSON = getJsonIndex(accept);
        final boolean isXml = returnXML != -1 && (returnJSON == -1 || returnXML < returnJSON);
        final boolean isJson = !isXml && returnJSON != -1;
        return isXml ? FendodbSerializationFormat.XML :
         	isJson ? FendodbSerializationFormat.JSON : FendodbSerializationFormat.CSV;
    }

}
