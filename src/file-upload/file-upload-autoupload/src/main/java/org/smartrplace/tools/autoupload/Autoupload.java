package org.smartrplace.tools.autoupload;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpResponse;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.upload.api.FileConfiguration;
import org.smartrplace.tools.upload.client.FileUploadClient;

@Component(
		service=Runnable.class,
		configurationPid=AutouploadConfiguration.FACTORY_PID,
		configurationPolicy=ConfigurationPolicy.REQUIRE,
		property= {
				"service.factoryPid=" + AutouploadConfiguration.FACTORY_PID
		}
)
@Designate(factory=true, ocd=AutouploadConfiguration.class)
public class Autoupload implements Runnable {

	private BundleContext ctx;
	private AutouploadConfiguration config;
	private long timeout;
	private TimeUnit timeoutUnit;
	private String remoteRelativePath;
	
	@Activate
    protected void activate(BundleContext ctx, AutouploadConfiguration config) {
    	this.config = config;
    	this.ctx = ctx;
    	this.timeout = config.timeout();
    	if (timeout <= 0)
    		throw new ComponentException("Invalid timeout " + timeout);
    	try {
    		timeoutUnit = TimeUnit.valueOf(config.timeoutUnit());
    	} catch (IllegalArgumentException e) {
    		throw new ComponentException("Invalid time unit " + config.timeoutUnit());
    	}
    	String pathRemote = config.remotePath().trim();
    	if (pathRemote.isEmpty())
    		this.remoteRelativePath = null;
    	else {
    		if (!pathRemote.startsWith("/"))
    			pathRemote = "/" + pathRemote;
    		if (pathRemote.endsWith("/"))
    			pathRemote = pathRemote.substring(0, pathRemote.length()-1);
    		this.remoteRelativePath = pathRemote;
    	}
    }
	
	@Override
	public void run() {
		try {
			final Path path = Paths.get(config.localPath());
			if (!Files.exists(path)) {
				LoggerFactory.getLogger(getClass()).warn("File does not exist {}", config.localPath());
				return;
			}
			final FileConfiguration fileConfig = new FileConfiguration(); // TODO
			setFilename(fileConfig, path.getFileName());
			final Collection<ServiceReference<FileUploadClient>> clients = ctx.getServiceReferences(FileUploadClient.class, "(remoteUrl=" + config.remoteUrl() + ")");
			if (clients == null || clients.isEmpty()) {
				LoggerFactory.getLogger(getClass()).warn("No appropriate client for url {}", config.remoteUrl());
				return;
			}
			final ServiceReference<FileUploadClient> ref = clients.iterator().next();
			final FileUploadClient client = ctx.getService(ref);
			try {
				if (client == null) {
					LoggerFactory.getLogger(getClass()).warn("Client is null for url {}", config.remoteUrl());
					return;
				}
				LoggerFactory.getLogger(getClass()).info("Starting autoupload for file {}, url {}", config.localPath(), config.remoteUrl());
				final Future<HttpResponse> future;
				if (config.incrementalUpload()) {
					final String format = config.incrementalUploadDateTimeFormat();
					final DateTimeFormatter formatter;
					if (format.isEmpty())
						formatter = null;
					else {
						try {
							formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH);
						} catch (IllegalArgumentException e) {
							LoggerFactory.getLogger(getClass()).warn("Invalid date time format {} for file upload",format);
							return;
						}
					}
					String prefix = config.incrementalUploadFilePrefix().trim();
					if (prefix.isEmpty())
						prefix = null;
					future = client.upload(path, remoteRelativePath, fileConfig, prefix, null, formatter);
				}
				else
					future = client.upload(path, remoteRelativePath, null, fileConfig);
				future.get(timeout, timeoutUnit);
			} finally {
				ctx.ungetService(ref);
			}
		} catch (InvalidSyntaxException | URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (IOException | TimeoutException | ExecutionException | CancellationException e) {
			LoggerFactory.getLogger(getClass()).warn("Failed to upload file",e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	private static void setFilename(final FileConfiguration config, final Path filename) {
		final String fl = filename.toString();
		final int idx = fl.lastIndexOf('.');
		final String prefix = idx > 0 ? fl.substring(0, idx) : fl;
		config.filePrefix  =prefix;
		if (idx > 0 && idx < fl.length()-1)
			config.fileEnding = fl.substring(idx+1);
	}
	
}

