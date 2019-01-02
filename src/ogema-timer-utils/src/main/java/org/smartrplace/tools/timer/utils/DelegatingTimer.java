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
package org.smartrplace.tools.timer.utils;

import java.util.List;
import java.util.Objects;

import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;

public abstract class DelegatingTimer implements Timer {
	
	protected final Timer baseTimer;

	protected DelegatingTimer(Timer timer) {
		this.baseTimer = Objects.requireNonNull(timer);
		baseTimer.stop();
	}

	@Override
	public void stop() {
		baseTimer.stop();
	}

	@Override
	public void resume() {
		baseTimer.resume();
	}

	@Override
	public boolean isRunning() {
		return baseTimer.isRunning();
	}

	/**
	 * @throws UnsupportedOperationException not supported
	 */
	@Override
	public void setTimingInterval(long millis) {
		throw new UnsupportedOperationException("Not supported by " + getClass().getSimpleName());
	}

	@Override
	public long getTimingInterval() {
		return baseTimer.getTimingInterval();
	}

	@Override
	public void destroy() {
		baseTimer.destroy();
	}

	@Override
	public void addListener(TimerListener listener) {
		baseTimer.addListener(listener);
	}

	@Override
	public boolean removeListener(TimerListener listener) {
		return baseTimer.removeListener(listener);
	}

	// may be necessary to override this in derived class
	@Override
	public List<TimerListener> getListeners() {
		return baseTimer.getListeners();
	}

	@Override
	public long getExecutionTime() {
		return baseTimer.getExecutionTime();
	}

	@Override
	public long getNextRunTime() {
		return baseTimer.getNextRunTime();
	}
	
}
