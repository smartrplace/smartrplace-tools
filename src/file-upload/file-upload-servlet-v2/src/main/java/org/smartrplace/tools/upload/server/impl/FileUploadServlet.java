package org.smartrplace.tools.upload.server.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.servlet.api.ServletAccessControl;
import org.smartrplace.tools.servlet.api.ServletConstants;
import org.smartrplace.tools.upload.api.FileConfiguration;
import org.smartrplace.tools.upload.api.FileConfigurations;
import org.smartrplace.tools.upload.server.FileUploadConstants;
import org.smartrplace.tools.upload.utils.DateTimeUtils;
import org.smartrplace.tools.upload.utils.ZipUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

// TODO
// config file/clean up for incremental upload (SlotsDB)
// config for incremental file uploader (SlotsDb)
@Component(
		service=Servlet.class,
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=" + FileUploadConstants.PATH + "/*",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=" + ServletConstants.CONTEXT_FILTER
		},
		configurationPid=FileUploadConstants.FILE_UPLOAD_PID,
		configurationPolicy=ConfigurationPolicy.REQUIRE
)
@Designate(ocd=FileUploadConfiguration.class)
public class FileUploadServlet extends HttpServlet  {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(FileUploadServlet.class);
	private final ObjectMapper mapper = new ObjectMapper();
	private final ObjectReader jsonReader = mapper.readerFor(FileConfiguration.class);
	private final ObjectReader jsonMapReader = mapper.readerFor(FileConfigurations.class);
	private final ObjectWriter jsonMapWriter = mapper.writerFor(FileConfigurations.class);
	
	@Reference
	private ServletAccessControl accessControl;
	
	private volatile Path tempFolder;
	private volatile FileUploadConfiguration config;
	
	@Activate
	protected void activate(BundleContext ctx, FileUploadConfiguration config) {
		this.config = config;
		try {
			Paths.get(config.uploadFolder());
		} catch (InvalidPathException e) {
			throw new ComponentException(e);
		}
		this.tempFolder = ctx.getDataFile("temp").toPath();
		try {
			Files.createDirectories(tempFolder);
		} catch (IOException e) {
			throw new ComponentException(e);
		}
	}
	
	@Modified
	protected void modified(FileUploadConfiguration config) {
		this.config = config;
	}
	
	private Void doGetInternal(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String user = (String) req.getAttribute(ServletContextHelper.REMOTE_USER);
		logger.trace("GET request received from user {}", user);
		if (user == null) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}
		String accept = req.getHeader("Accept");
		accept = accept == null ? "text/plain" : accept.toLowerCase();
		String path0 = req.getPathInfo();
		if (path0 == null)
			path0 = "/.";
		final FileUploadConfiguration config = this.config;
		final Path dir = Paths.get(config.uploadFolder(), user, path0.substring(1));
		if (accept.startsWith("text/plain")) {
			final PrintWriter writer = resp.getWriter();
			if (Files.isRegularFile(dir)) {
				writer.write(path0);
				writer.write('\n');
				writer.write("size: ");
				writer.write(String.valueOf(Files.size(dir)));
				writer.write('\n');
				writer.write("last modified: ");
				writer.write(String.valueOf(Files.getLastModifiedTime(dir).toMillis()));
				writer.write('\n');
			}
			else {
				if (!Files.isDirectory(dir)) {
					resp.setStatus(HttpServletResponse.SC_OK);
					return null;
				}
				final AtomicBoolean first = new AtomicBoolean(true);
				try (final Stream<Path> stream = Files.list(dir)) {
					stream
						.map(path -> path.getFileName().toString())
						.forEach(path -> {
							if (!first.getAndSet(false))
								writer.write(',');
							writer.write(path);
						});
				}
			}
			writer.flush();
		} else if (accept.startsWith("application/octet-stream")) {
			if (Files.isRegularFile(dir)) {
				try (final OutputStream out = resp.getOutputStream()){
					Files.copy(dir, out);
					out.flush();
				}
			} else {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return null;
			}
		} else if (accept.startsWith("application/zip")) {
			if (!Files.exists(dir)) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return null;
			}
			try (final OutputStream out = resp.getOutputStream()){
				ZipUtils.zipFolder(dir, out);
				out.flush();
			}
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot provide content type " + accept);
			return null;
		}
		resp.setStatus(HttpServletResponse.SC_OK);
		return null;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final AccessControlContext ctx = accessControl.getAccessControlContext();
		if (ctx == null)
			return;
		try {
			AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> doGetInternal(req, resp), ctx);
		} catch (PrivilegedActionException e) {
			throwInternal(e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!req.getContentType().toLowerCase().startsWith("multipart/form-data")) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported content type");
			return;
		}
		final String user = (String) req.getAttribute(ServletContextHelper.REMOTE_USER);
		logger.trace("POST request received from user {}", user);
		if (user == null) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		final AccessControlContext ctx = accessControl.getAccessControlContext();
		if (ctx == null)
			return;
		String path0 = req.getPathInfo();
		if (path0 == null)
			path0 = "/.";
		if (path0.length() > config.maxPathLength()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Path info too long. Max: " + config.maxPathLength());
			return;
		}
		if (depth(path0) > config.maxDepth()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Path depth too high. Max: " + config.maxDepth());
			return;
		}
		final String path = path0;
		// TODO replace by standard multi part config
		final MultipartConfigElement mce = new MultipartConfigElement(tempFolder.toString(), config.maxFileSize(), config.maxRequestSize(), (int) config.fileSizeThreshold());
		final MultiPartInputStreamParser mpisp = new MultiPartInputStreamParser(req.getInputStream(), req.getContentType(), mce, tempFolder.toFile()); 
		final FileUploadConfiguration config = this.config;
		mpisp.getParts();
		final Part configPart = mpisp.getPart("config"); // fails due to jetty bug if we did not call mpisp.getParts() before // https://github.com/eclipse/jetty.project/issues/2892
	    final AtomicBoolean success = new AtomicBoolean(true);
	    final AtomicInteger response = new AtomicInteger(-1);
	    final StringBuilder report = new StringBuilder();
		try {
			final Collection<Part> parts = mpisp.getParts();
			parts.stream()
				.filter(part -> part.getContentType() != null && part.getContentType().startsWith("application/octet-stream"))
				.forEach(filePart -> {
				    String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
				    if (fileName == null || fileName.isEmpty()) 
				    	fileName = "unspecified";
				    if (fileName.length() > config.maxFileNameLength()) {
				    	logger.warn("File name too long: {}. Skipping this.", fileName);
				    	return; // skip this one
				    }
				    Path target = null;
				    boolean dirCreated = false;
				    try (InputStream is = filePart.getInputStream()) {
				    	target = DateTimeUtils.buildFilePath(config.uploadFolder(), path.substring(1), fileName, user);
				    	if (target == null)
				    		return;
				    	final Path dir = target.getParent();
				    	dirCreated = !Files.exists(dir);
				    	if (dirCreated)
				    		Files.createDirectories(dir);
				    	final Path configPath = target.getParent().resolve(config.configFileName());
				    	final FileConfiguration existingConfig = getExistingConfig(configPath, target.getFileName());
				    	final Path target0 = target;
			    		if (existingConfig == null && configPart != null) { // TODO force overwrite option? // TODO it is not clear which files this config belongs to!
			    			// TODO parse config part into a FileConfiguraiton first, set applicable file names and store it then!
				    		final FileConfiguration newConfig;
				    		try (final InputStream in = configPart.getInputStream()) {
				    			 newConfig = jsonReader.readValue(in);
				    		}
				    		if (!prefixSuffiXMatch(target0, newConfig)) {
				    			success.set(false);
				    			report.append("Invalid request: configuration does not apply to provided file ").append(fileName).append('\n');
				    			logger.warn("Invalid request: configuration does not apply to provided file {}", fileName);
				    			return;
				    		}
				    		addConfig(configPath, newConfig);
				    	}
				    	AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
					    	Files.copy(is, target0, StandardCopyOption.REPLACE_EXISTING);
					    	logger.debug("New file from user {} at {}",user,target0);
					    	return null;
				    	}, ctx);
				    } catch (IOException | SecurityException | PrivilegedActionException e) {
				    	logger.error("Could not store file", e);
				    	success.set(false);
				    	try {
				    		if (dirCreated) {
				    			final Path dir = target.getParent();
				    			if (Files.isDirectory(dir)) {
				    				FileUtils.deleteDirectory(dir.toFile());
				    			}
				    		}
				    	} catch (Exception ignore) {}
				    	if (e instanceof SecurityException) {
				    		response.set(HttpServletResponse.SC_FORBIDDEN);
				    		report.append("Permission to store file at " + target + " denied.\n");
				    	}
				    }
				});
			final Path dir = Paths.get(config.uploadFolder(), user, path.substring(1));
			final AtomicBoolean hasZipPart = new AtomicBoolean(false);
			final List<Path> zipFiles = new ArrayList<>();
			parts.stream()
				.filter(part -> part.getContentType() != null && part.getContentType().startsWith("application/zip"))
				.forEach(zipPart -> {
					hasZipPart.set(true);
				    try (InputStream is = zipPart.getInputStream();
				    		final ZipInputStream zis = new ZipInputStream(is)) {
				    	ZipEntry entry;
				    	while (true) {
				    		entry = zis.getNextEntry();
				    		if (entry == null)
				    			break;
				    		final Path targetFile = dir.resolve(entry.getName());
				    		final Path parent = targetFile.getParent();
				    		final boolean existed = Files.exists(parent);
				    		if (!existed) {
				    			Files.createDirectories(parent);
				    		}
				    		try {
				    			AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
						    		Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
							    	return null;
						    	}, ctx);
				    			zipFiles.add(targetFile);
				    		} catch(SecurityException | PrivilegedActionException e) {
				    			logger.warn("Failed to unzip file", e);
				    	    	success.set(false);
				    	    	if (!existed) {
				    	    		try {
				    	    			Files.delete(parent);
				    	    		} catch (Exception ignore) {}
				    	    	}
				    		}
				    	}
				    } catch (IOException e) {
				    	throw new UncheckedIOException(e);
				    }
				    logger.debug("New files from user {} at {}",user,dir);
				});
			// FIXME not working yet!
			if (hasZipPart.get() && configPart != null) {
				final Path configFile = dir.resolve(config.configFileName());
				final FileConfiguration cfg;
	    		try (final Reader reader = new InputStreamReader(configPart.getInputStream(), StandardCharsets.UTF_8)) {
	    			cfg = jsonReader.readValue(reader);
	    		}
	    		for (Path zip: zipFiles) {
	    			final FileConfiguration existingConfig = getExistingConfig(configFile, zip);
	    			if (existingConfig == null)
	    				addConfig(configFile, cfg);
	    		}
			}
		} catch (UncheckedIOException e) {
	    	resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    	logger.warn("Failed to write file to disk",e.getCause());
	    	success.set(false);
	    	return;
		}
		// TODO report successful/failed files back to client, implement retry strategy in client
		if (success.get())
			resp.setStatus(HttpServletResponse.SC_OK);
		else {
			final int responseCode = response.get() > 0 ? response.get() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			resp.sendError(responseCode, report.toString());
		}
	}
	
	
	static Path getValidFilename(String folder, String filename, String user, FileUploadConfiguration config) throws IOException {
		String[] components = filename.split("\\.");
		String ending = null;
		if (components.length > 1) {
			ending = components[components.length-1];
			filename = filename.substring(0, filename.lastIndexOf('.'));
		}
		Path dir = Paths.get(config.uploadFolder(), user, folder);
		Files.createDirectories(dir);
		Path target;
		//do {
			StringBuilder newFilename = new StringBuilder();
			newFilename.append(filename); //.append('_').append(getTimestamp());
			if (ending != null)
				newFilename.append('.').append(ending);
			target = dir.resolve(newFilename.toString());
			
		//} while (Files.exists(target));
		return target; 
	}
	
	private static final int depth(String path) {
		if (path.isEmpty() || path.equals("."))
			return 0;
		path = Paths.get(path).normalize().toString();
		return path.length() - path.replace("/", "").replace("\\", "").length() - 1;
	}
	
	private static boolean prefixSuffiXMatch(final Path path, final FileConfiguration config) {
		if (config.filePrefix == null)
			return false;
		final String filename = path.getFileName().toString();
		return filename.startsWith(config.filePrefix) && (config.fileEnding == null || filename.endsWith(config.fileEnding)); 
	}
	
	private final FileConfiguration getExistingConfig(final Path path, final Path filename) throws IOException {
		if (path == null || !Files.isRegularFile(path))
			return null;
		final FileConfigurations configs;
		try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			 configs = jsonMapReader.readValue(reader);
		}
		return configs.configurations.stream()
			.filter(cfg -> prefixSuffiXMatch(filename, cfg))
			.findAny().orElse(null);
	}
	
	// TODO some sort of synchronization to avoid the (unlikely) case of duplicates?
	private final void addConfig(final Path configPath, final FileConfiguration cfg) throws IOException {
		final FileConfigurations configs;
		if (Files.exists(configPath)) {
			try (final BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
				configs = jsonMapReader.readValue(reader);
			}
		} else {
			Files.createDirectories(configPath.getParent().normalize());
			configs = new FileConfigurations();
		}
		configs.configurations.add(cfg);
		try (final BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
			jsonMapWriter.writeValue(writer, configs);
		}
	}
	
	private static final void throwInternal(final PrivilegedActionException pae) throws ServletException, IOException {
		final Throwable cause = pae.getCause();
		if (cause instanceof ServletException)
			throw (ServletException) cause;
		if (cause instanceof IOException)
			throw (IOException) cause;
		if (cause instanceof RuntimeException)
			throw (RuntimeException) cause;
		if (cause instanceof Error)
			throw (Error) cause;
		throw new RuntimeException(cause);
	}
	
}
