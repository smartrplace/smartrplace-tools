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
package org.smartrplace.tools.exec.impl;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class TaskWrapper implements Runnable {

	private final Runnable task;
	private final AtomicLong executionMillis = new AtomicLong(0);
	private final AtomicBoolean isRunning = new AtomicBoolean(false);
	
	TaskWrapper(Runnable task) {
		this.task = task;
	}
	
	@Override
	public void run() {
		final long nanos = System.nanoTime();
		isRunning.set(true);
		try {
			task.run();
		} finally {
			this.executionMillis.getAndAdd((System.nanoTime() - nanos) / 1000000);
			isRunning.set(false);
		}
	}
	
	public Runnable getTask() {
		return task;
	}
	
	public long getExecutionTimeMillis() {
		return executionMillis.get();
	}
	
	public boolean isRunning() {
		return isRunning.get();
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
