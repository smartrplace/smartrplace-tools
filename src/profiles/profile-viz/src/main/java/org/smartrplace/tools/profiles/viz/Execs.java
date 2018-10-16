package org.smartrplace.tools.profiles.viz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class Execs {

	private final static AtomicInteger cnt = new AtomicInteger(0);
	private static ExecutorService exec;
	
	static ExecutorService getExecutor() {
		synchronized (Execs.class) {
			if (exec == null) 
				exec = Executors.newCachedThreadPool(r -> new Thread(r, "profile-recording-" + cnt.getAndIncrement()));
			return exec;
		}
		
	}
	
}
