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
package org.smartrplace.tools.upload.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.smartrplace.tools.upload.api.FileConfiguration;

public interface FileUploadClient {
	
	/**
	 * PID of the default implementation
	 */
	public static final String CLIENT_PID = "org.smartrplace.tools.FileUploadClient";
	
	/**
	 * @param stream
	 * @param size
	 * 		size of the stream in bytes. Must match exactly.
	 * @param targetPath
	 * 		may be null, in which case the current user's base upload path is used
	 * @param targetFilename
	 * @param contentType
	 * 		may be null, in which case a default is used (application/octet-stream)
	 * @param config
	 * 		must not be null
	 * @return
	 * 		a future with the Http response
	 */
	Future<HttpResponse> upload(InputStream stream, long size, String targetPath, ContentType contentType,
			FileConfiguration config) throws URISyntaxException;
	
	/**
	 * 
	 * @param bytes
	 * @param targetPath
	 * @param contentType
	 * @param config
	 * @return
	 * @throws URISyntaxException
	 */
	default Future<HttpResponse> upload(byte[] bytes, String targetPath, ContentType contentType,
			FileConfiguration config) throws URISyntaxException {
		return upload(new ByteArrayInputStream(bytes), bytes.length, targetPath, contentType, config);
	}
	
	/**
	 * 
	 * @param file
	 * @param targetPath
	 * @param contentType
	 * @param config
	 * 		may be null, in which case a default is used
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	default Future<HttpResponse> upload(Path file, String targetPath, ContentType contentType, 
			FileConfiguration config) throws IOException, URISyntaxException {
		if (config == null)
			config = new FileConfiguration();
		final String filename = file.getFileName().toString();
		final int idx = filename.lastIndexOf('.');
		config.filePrefix = idx > 0 ? filename.substring(0, idx) : filename;
		config.fileEnding = idx > 0 ? filename.substring(idx+1) : null;
		final long size = Files.size(file); // FIXME this is probably not the correct size!
		try (final InputStream stream = new BufferedInputStream(Files.newInputStream(file))) {
			return upload(stream, size, targetPath, contentType, config);
		}
	}

	/**
	 * Upload all files/folders in a specific directory. Filenames in the folder
	 * are required to obey a specific naming convention; they must end with a 
	 * a string that can be parsed to a date or date-time. Files/folders are uploaded
	 * incrementally; only those that have not been previously uploaded will be uploaded.
	 * 
	 * @param folder
	 * 		upload the complete content of this folder
	 * @param targetPath
	 * @param config
	 * @param filePrefix
	 * 		may be null, in which case the filename must only consist of the date-time string.
	 * @param fileFilter may be null, in which case all files and subfolders are included 
	 * @param formatter
	 * 		may be null, in which case a default formatter is used.
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws TimeoutException
	 */
	Future<HttpResponse> upload(Path folder, String targetPath, FileConfiguration config, String filePrefix, 
			Predicate<Path> fileFilter, DateTimeFormatter formatter) throws IOException, URISyntaxException, TimeoutException;
	
}
