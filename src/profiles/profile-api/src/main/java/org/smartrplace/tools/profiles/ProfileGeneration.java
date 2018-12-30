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
package org.smartrplace.tools.profiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Consumer;

public interface ProfileGeneration {

	default Profile run(ProfileTemplate template, Consumer<State> switchFunction, NavigableMap<Long, State> stateDurations, 
				Map<DataPoint, Object> data) throws InterruptedException {
		return run(template, switchFunction, stateDurations, data, null);
	}
	
	Profile run(ProfileTemplate template, Consumer<State> switchFunction, NavigableMap<Long, State> stateDurations, 
			Map<DataPoint, Object> data, State endState) throws InterruptedException;	
	
	// serialization // generic
	void store(Profile profile, Writer out) throws IOException;

	default void store(Profile profile, OutputStream out) throws IOException {
		store(profile, new OutputStreamWriter(out, StandardCharsets.UTF_8));
	}

	Profile read(Reader in) throws IOException;
	
	default Profile read(InputStream in) throws IOException {
		return read(new InputStreamReader(in, StandardCharsets.UTF_8));
	};

	// serialization // global store
	void storeProfile(Profile profile) throws IOException;
	boolean removeStoredProfile(String profileId) throws IOException;
	Profile getStoredProfile(String profileId) throws IOException;
//	Collection<Profile> getStoredProfiles(String templateId) throws IOException;
	Collection<String> getStoredProfileIds(String templateId) throws IOException;
	
}
