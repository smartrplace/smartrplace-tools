package org.smartrplace.tools.sampletask.impl;

import org.osgi.service.component.annotations.Component;
import org.smartrplace.tools.exec.ExecutorConstants;

// could be a factory component as well
@Component(
		service=Runnable.class,
		property= {
				ExecutorConstants.TASK_DELAY + ":Long=10000",
				ExecutorConstants.TASK_PERIOD + ":Long=5000",
		}
)
public class SampleTask implements Runnable {

	@Override
	public void run() {
		System.out.println("Sample task executing at " + new java.util.Date(System.currentTimeMillis()));
	}
	
	@Override
	public String toString() {
		return "Sample task for testing ";
	}

}
