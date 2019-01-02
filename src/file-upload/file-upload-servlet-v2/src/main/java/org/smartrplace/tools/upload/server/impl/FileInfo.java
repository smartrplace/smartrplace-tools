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
package org.smartrplace.tools.upload.server.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class FileInfo {

	final Path file;
	final long timestamp;
	final long size;
	
	FileInfo(Path path, long timestamp) throws IOException {
		this.file = path;
		this.timestamp = timestamp;
		this.size = Files.size(file);
	}
	
}
