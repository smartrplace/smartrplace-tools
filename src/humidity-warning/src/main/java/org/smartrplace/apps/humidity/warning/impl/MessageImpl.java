/**
 * ﻿Copyright 2019 Smartrplace UG
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
package org.smartrplace.apps.humidity.warning.impl;

import de.iwes.widgets.api.messaging.Message;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

class MessageImpl implements Message{

	private final String title;
	private final String msg;
	private final String link = null;
	private final MessagePriority prio;
	
	MessageImpl(String title,String msg,MessagePriority prio) {
		this.title = title;
		this.msg = msg;
		this.prio = prio;
	}

	@Override
	public String title(OgemaLocale locale) {
		return title;
	}

	@Override
	public String message(OgemaLocale locale) {
		return msg;
	}

	@Override
	public String link() {
		return link;
	}

	@Override
	public MessagePriority priority() {
		return prio;
	}
	
}
