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
