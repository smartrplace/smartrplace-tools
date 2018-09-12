package org.smartrplace.tools.exec.impl;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

class TaskWrapper implements Runnable {

	private final Runnable task;
	private final AtomicLong executionMillis = new AtomicLong(0);
	
	TaskWrapper(Runnable task) {
		this.task = task;
	}
	
	@Override
	public void run() {
		final long nanos = System.nanoTime();
		try {
			task.run();
		} finally {
			this.executionMillis.getAndAdd((System.nanoTime() - nanos) / 1000000);
		}
	}
	
	public Runnable getTask() {
		return task;
	}
	
	public long getExecutionTimeMillis() {
		return executionMillis.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TaskWrapper))
			return false;
		return Objects.equals(this.task, ((TaskWrapper) obj).task);
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(task);
	}
	
}
