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
package org.smartrplace.tools.schedule.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.smartrplace.tools.schedule.management.imports.DataGenerator;
import org.smartrplace.tools.schedule.management.imports.FileBasedDataGenerator;
import org.smartrplace.tools.schedule.management.imports.OgemaDataSource;
import org.smartrplace.tools.schedule.management.persistence.FileBasedPersistence;
import org.smartrplace.tools.schedule.management.persistence.OgemaTimeSeriesPersistence;
import org.smartrplace.tools.schedule.management.persistence.TimeSeriesPersistence;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

/**
 * This is an extensible application. Register services 
 * {@link DataGenerator} and/or {@link TimeSeriesPersistence} 
 * for extensions. See for instance the SlotsDbStandalone app.
 */
@Component
@Service(Application.class)
@References({
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, 
			policy=ReferencePolicy.DYNAMIC, 
			referenceInterface=DataGenerator.class,
			bind="addSource",
			unbind="removeSource",
			name="source"),
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, 
			policy=ReferencePolicy.DYNAMIC, 
			referenceInterface=DataProvider.class,
			bind="addGenericSource",
			unbind="removeGenericSource",
			name="provider"),
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, 
			policy=ReferencePolicy.DYNAMIC, 
			referenceInterface=TimeSeriesPersistence.class,
			bind="addTarget",
			unbind="removeTarget",
			name="target")
})
public class ScheduleManagementApp implements Application {

	private DataSourceFactory dataSources;
	private FileBasedDataGeneratorFactory fileBasedDataSources;
	// TODO migrate to OSGi 6 annotations, use ComponentServiceObjects -> do not enforce class loading
	private final Set<DataProvider<?>> dataSources2 = Collections.synchronizedSet(new HashSet<>(8));
	private final Set<DataGenerator> sourceQueue = new HashSet<>(8);
	private final Set<DataProvider<?>> sourceQueue2 = new HashSet<>(8);
	private final Set<TimeSeriesPersistence> targetQueue = new HashSet<>(8);
	private WidgetApp wApp;
	ApplicationManager am;
	private ServiceRegistration<?> adminAppRegistration;
	private ServiceRegistration<?> scheduleAppRegistration; 
	
	@Reference
	private OgemaGuiService widgetService;

	@Override
	public synchronized void start(ApplicationManager appManager) {
		this.am = appManager;
		wApp = widgetService.createWidgetApp("/org/smartrplace/tools/schedule/management", appManager);
		final WidgetPage<?> page = wApp.createStartPage();
		dataSources = new DataSourceFactory(this);
		fileBasedDataSources = new FileBasedDataGeneratorFactory();
		drainQueue();
		new ScheduleMgmtPage(page, dataSources, dataSources2, fileBasedDataSources, appManager);
		boolean test = Boolean.getBoolean("org.ogema.apps.createtestresources");
		if (test)
			createTestSchedule();
		registerFrameworkAdminApp();
	}
	
	private final void drainQueue() {
		Iterator<DataGenerator> it = sourceQueue.iterator();
		while (it.hasNext()) {
			addSource(it.next());
			it.remove();
		}
		Iterator<TimeSeriesPersistence> it2 = targetQueue.iterator();
		while (it2.hasNext()) {
			addTarget(it2.next());
			it2.remove();
		}
		Iterator<DataProvider<?>> it3 = sourceQueue2.iterator();
		while (it3.hasNext()) {
			addGenericSource(it3.next());
			it3.remove();
		}
	}
	
	private final void fillQueue() {
		if (dataSources != null) {
			sourceQueue.addAll(dataSources.drainSources());
			targetQueue.addAll(dataSources.drainTargets());
		}
		if (fileBasedDataSources != null) {
			sourceQueue.addAll(fileBasedDataSources.drainSources());
			targetQueue.addAll(fileBasedDataSources.drainTargets());
		}
		if (dataSources2 != null) {
			sourceQueue2.addAll(dataSources2);
			dataSources2.clear();
		}
	}
	
	@Override
	public synchronized void stop(AppStopReason reason) {
		if (wApp != null)
			wApp.close();
		fillQueue();
		wApp = null;
		am = null;
		dataSources = null;
		fileBasedDataSources = null;;
		unregisterFrameworkAdminApp();
	}
	
	protected synchronized void addGenericSource(DataProvider<?> provider) {
		if (am == null) {
			sourceQueue2.add(provider);
			return;
		}
		dataSources2.add(provider);
	}
	
	protected synchronized void removeGenericSource(DataProvider<?> provider) {
		sourceQueue2.remove(provider);
		dataSources2.remove(provider);
	}

	protected synchronized void addSource(DataGenerator source) {
		if (am == null) {
			sourceQueue.add(source);
			return;
		}
		if (source instanceof FileBasedDataGenerator) 
			fileBasedDataSources.addSource((FileBasedDataGenerator) source);
		else if (source instanceof OgemaDataSource<?, ?>) 
			dataSources.addSource((OgemaDataSource<?, ?>) source);
	}
	
	protected synchronized void removeSource(DataGenerator source) {
		sourceQueue.remove(source);
		if (fileBasedDataSources != null && dataSources != null) {
			if (source instanceof FileBasedDataGenerator) 
				fileBasedDataSources.removeSource((FileBasedDataGenerator) source);
			else if (source instanceof OgemaDataSource<?, ?>) 
				dataSources.removeSource((OgemaDataSource<?, ?>) source);
		}
	}
	
	protected synchronized void addTarget(TimeSeriesPersistence target) {
		if (am == null) {
			targetQueue.add(target);
			return;
		}
		if (target instanceof FileBasedPersistence) 
			fileBasedDataSources.addTarget((FileBasedPersistence) target);
		else if (target instanceof OgemaTimeSeriesPersistence<?, ?>) 
			dataSources.addTarget((OgemaTimeSeriesPersistence<?, ?>) target);
	}
	
	protected synchronized void removeTarget(TimeSeriesPersistence target) {
		targetQueue.remove(target);
		if (dataSources != null && fileBasedDataSources != null) {
			if (target instanceof FileBasedPersistence) 
				fileBasedDataSources.removeTarget((FileBasedPersistence) target);
			else if (target instanceof OgemaTimeSeriesPersistence<?, ?>) 
				dataSources.removeTarget((OgemaTimeSeriesPersistence<?, ?>) target);
		}
	}
	
	private final void createTestSchedule() {
		FloatResource fl = am.getResourceManagement().createResource("scheduleTestFloat", FloatResource.class);
		AbsoluteSchedule schedule = fl.program().create();
		List<SampledValue> values = new ArrayList<>();
		long t0 = am.getFrameworkTime();
		for (int i=0;i<1000;i++) {
			values.add(new SampledValue(new FloatValue((float) Math.random()), t0 += 10000 + (long) ((Math.random()-0.5)*5000), Quality.GOOD));
		}
		schedule.addValues(values);
		fl.activate(true);
	}

	private void registerFrameworkAdminApp() {
		try {
			final String symbName = "org.smartrplace.tools.schedule-management";
			final String desc = "Manage schedules and other time series, visualize and edit them";
			
			de.iwes.tools.apps.collections.api.AdminApp app = new de.iwes.tools.apps.collections.api.AdminApp() {
				
				public String bundleSymbolicName() {
					return symbName;
				}
				
				public String description() {
					return desc;
				}
				
			};
			de.iwes.tools.apps.collections.api.ScheduleApp scheduleApp = new de.iwes.tools.apps.collections.api.ScheduleApp() {

				public String bundleSymbolicName() {
					return symbName;
				}
				
				public String description() {
					return desc;
				}

				@Override
				public Class<? extends ReadOnlyTimeSeries> supportedScheduleType() {
					return ReadOnlyTimeSeries.class;
				}
				
			};
			adminAppRegistration = FrameworkUtil.getBundle(getClass()).getBundleContext().registerService(
					de.iwes.tools.apps.collections.api.AdminApp.class, app, null);
			scheduleAppRegistration = FrameworkUtil.getBundle(getClass()).getBundleContext().registerService(
					de.iwes.tools.apps.collections.api.ScheduleApp.class, scheduleApp, null);
		} catch (NoClassDefFoundError ignore) {}
	}
	
	private void unregisterFrameworkAdminApp() {
		final ServiceRegistration<?> reg = adminAppRegistration;
		final ServiceRegistration<?> reg2 = scheduleAppRegistration;
		adminAppRegistration = null;
		scheduleAppRegistration = null;
		if (reg != null || reg2 != null) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						reg.unregister();
					} catch (Exception ignore) {}
					try {
						reg2.unregister();
					} catch (Exception ignore) {}
				}
			}).start();
		}
	}

}
