package org.smartrplace.tools.upload.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.ssl.KeyStoreNotAvailableException;
import org.smartrplace.tools.ssl.SslService;
import org.smartrplace.tools.upload.api.FileConfiguration;
import org.smartrplace.tools.upload.client.FileUploadClient;
import org.smartrplace.tools.upload.utils.DateTimeUtils;
import org.smartrplace.tools.upload.utils.ZipUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

// TODO set character encodings and content types where necessary?
@Component(
		service=FileUploadClient.class,
		configurationPid=FileUploadClient.CLIENT_PID,
		configurationPolicy=ConfigurationPolicy.REQUIRE,
		property= {
				"service.factoryPid=" + FileUploadClient.CLIENT_PID
		}
)
@Designate(factory=true, ocd=FileUploadConfiguration.class)
public class FileUploadClientImpl implements FileUploadClient {

	private HttpClientContext clientContext;
	private Path tempFolder;
	private FileUploadConfiguration config;
	private volatile URL remote;
	private volatile CloseableHttpAsyncClient client;
	private final ObjectWriter jsonWriter = new ObjectMapper().writerFor(FileConfiguration.class);
	
	@Reference(service=SslService.class)
	protected ComponentServiceObjects<SslService> sslService;
	
	@Activate
	protected void activate(BundleContext ctx, FileUploadConfiguration config) throws KeyStoreNotAvailableException, IOException {
		this.config = config;
		this.tempFolder = ctx.getDataFile("temp").toPath();
		clearTempFolder();
		Files.createDirectories(tempFolder);
		try {
			this.remote = new URL(config.remoteUrl());
		} catch (MalformedURLException e) {
			throw new ComponentException("Invalid URL configured: " + config.remoteUrl());
		}
		clearTempFolder();
		final boolean isHttps = "https".equalsIgnoreCase(remote.getProtocol());
//		https://www.baeldung.com/httpclient-4-basic-authentication
		final CredentialsProvider auth = new BasicCredentialsProvider();
		final UsernamePasswordCredentials userPw = new UsernamePasswordCredentials(config.remoteUser(), config.remotePw());
		auth.setCredentials(AuthScope.ANY, userPw);
		final HttpAsyncClientBuilder builder = HttpAsyncClients.custom();
		builder.setDefaultCredentialsProvider(auth);
		final HttpHost targetHost = new HttpHost(remote.getHost(), remote.getPort(), remote.getProtocol().toLowerCase());
		final AuthCache authCache = new BasicAuthCache();
		authCache.put(targetHost, new BasicScheme());
		
		// Add AuthCache to the execution context
		this.clientContext = HttpClientContext.create();
		clientContext.setCredentialsProvider(auth);
		clientContext.setAuthCache(authCache);
		
		if (isHttps) {
			final SslService ssl = sslService.getService();
			try {
				builder.setSSLContext(config.disableHostCertVerification() ? ssl.getUnsafeTrustAllContext(false) : ssl.getContext(false, true));
			} finally {
				sslService.ungetService(ssl);
			}
		}
		if (config.disableHostnameVerification())
			builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
		this.client = builder.build();
		client.start();
		if (config.disableHostCertVerification() || config.disableHostnameVerification()) {
			LoggerFactory.getLogger(getClass()).error("!!!!!!!!!!!");
			LoggerFactory.getLogger(getClass()).error("Running in debug mode; server SSL verification disabled in file upload client configuration.");
			LoggerFactory.getLogger(getClass()).error("!!!!!!!!!!!");
		}
	}
	
	private final void clearTempFolder() {
		if (!Files.isDirectory(tempFolder))
			return;
		try (final Stream<Path> files = Files.list(tempFolder)) {
			files
				.filter(Files::isRegularFile)
				.forEach(t -> {
					try {
						Files.delete(t);
					} catch (IOException e) {
						LoggerFactory.getLogger(FileUploadClientImpl.class).warn("Failed to delete temp file");
					}
				});
		} catch (IOException e) {
			LoggerFactory.getLogger(FileUploadClientImpl.class).warn("Failed to access temp folder",e);
		}
	}
		
	@Deactivate
	protected void deactivate() throws IOException {
		final CloseableHttpAsyncClient client = this.client;
		if (client != null)
			client.close();
	}
	
	@Override
	public Future<HttpResponse> upload(final InputStream stream, final long size, final String targetPath, 
			final ContentType contentType, FileConfiguration config) throws URISyntaxException {
		Objects.requireNonNull(stream);
		Objects.requireNonNull(config);
		final HttpAsyncClient client = this.client;
		if (client == null)
			throw new IllegalStateException("Service inactive");
		final String path = appendPath(remote.getPath(), targetPath);
		final URIBuilder uriBuilder = new URIBuilder(remote.toURI());
		if (path != null)
			uriBuilder.setPath(path);
		final HttpPost post = new HttpPost(uriBuilder.build().toString());
		final ContentType ct = contentType == null ? ContentType.APPLICATION_OCTET_STREAM : contentType;
		final String targetFilename = config.fileEnding == null || config.fileEnding.isEmpty() ? config.filePrefix 
				: config.filePrefix + "." + config.fileEnding;
		final InputStreamBody body = new InputStreamBody(stream, ct, targetFilename) {
		
			@Override
			public long getContentLength() {
				return size;
			}
			
		};
		return AccessController.doPrivileged((PrivilegedAction<Future<HttpResponse>>) () -> {
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.addPart("file", body);
			
			final String configJson;
			try {
				configJson = jsonWriter.writeValueAsString(config);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Unexpected error",e);
			}
			builder.addTextBody("config", configJson, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
			HttpEntity entity = builder.build();
			post.setEntity(entity);
			return client.execute(post, clientContext, null);
		});
		
	}
	
	@Override
	public Future<HttpResponse> upload(Path file, String targetPath, ContentType contentType, FileConfiguration config0)
			throws IOException, URISyntaxException {
		Objects.requireNonNull(file);
		if (config0 == null) {
			config0 = new FileConfiguration();
			final String filename = file.getFileName().toString();
			final int idx = filename.lastIndexOf('.');
			config0.filePrefix = idx > 0 ? filename.substring(0, idx) : filename;
			config0.fileEnding = idx > 0 ? filename.substring(idx+1) : null;
		}
		final FileConfiguration config = config0;
		final HttpAsyncClient client = this.client;
		if (client == null)
			throw new IllegalStateException("Service inactive");
		final String path = appendPath(remote.getPath(), targetPath);
		final URIBuilder uriBuilder = new URIBuilder(remote.toURI());
		if (path != null)
			uriBuilder.setPath(path);
		final HttpPost post = new HttpPost(uriBuilder.build().toString());
		final ContentType ct = contentType == null ? ContentType.APPLICATION_OCTET_STREAM : contentType;
		final String targetFilename = config0.fileEnding == null || config0.fileEnding.isEmpty() ? config0.filePrefix 
				: config0.filePrefix + "." + config0.fileEnding;
		return AccessController.doPrivileged((PrivilegedAction<Future<HttpResponse>>) () -> {
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.addBinaryBody("file", file.toFile(), ct, targetFilename);
			final String configJson;
			try {
				configJson = jsonWriter.writeValueAsString(config);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Unexpected error",e);
			}
			builder.addTextBody("config", configJson, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
			HttpEntity entity = builder.build();
			//
			post.setEntity(entity);
			return client.execute(post, clientContext, null);
		});
	}
	
	private Future<HttpResponse> uploadInternal(Path folder, String targetPath, FileConfiguration config, String filePrefix,
			Predicate<Path> fileFilter, DateTimeFormatter formatter) throws IOException, URISyntaxException, TimeoutException {
		final HttpAsyncClient client = this.client;
		if (client == null)
			throw new IllegalStateException("Service inactive");
		final String path = appendPath(remote.getPath(), targetPath);
		final URIBuilder uriBuilder = new URIBuilder(remote.toURI());
		if (path != null)
			uriBuilder.setPath(path);
		final HttpGet get = new HttpGet(uriBuilder.build());
		get.setHeader("Accept", "text/plain;charset=utf-8");
		final Future<HttpResponse> respFuture = client.execute(get, clientContext, null);
		final HttpResponse resp;
		try {
			resp = respFuture.get(this.config.getRequestTimeoutSeconds(), TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			throw new RuntimeException(cause);
		}
		final int status = resp.getStatusLine().getStatusCode();
		if (status == 401)
			throw new SecurityException("Unauthorized");
		if (status / 100 != 2)
			throw new IOException("Unexpected server response: " + resp.getStatusLine().getReasonPhrase());
		final String response = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
		final Instant last = Arrays.stream(response.split(","))
			.map(file -> getInstant(file, filePrefix, formatter))
			.filter(inst -> inst != null)
			.max((i1,i2) -> i1.compareTo(i2))
			.orElse(null);
		// TODO File permission?
		final Path tempFile = Files.createTempFile(tempFolder, "upload", ".zip");
		try {
			try (final Stream<Path> files0 = Files.list(folder)) {
				Stream<Path> files = fileFilter == null ? files0 : files0.filter(fileFilter);
				if (fileFilter != null)
					files = files.filter(fileFilter);
				files = files
						.filter(file -> {
							final Instant inst = getInstant(file, filePrefix, formatter);
							if (inst == null)
								return false;
							if (last == null)
								return true;
							return inst.compareTo(last) > 0;
						});
				try (final OutputStream out = Files.newOutputStream(tempFile);
						final ZipOutputStream zio = new ZipOutputStream(out)) {
					files.forEach(f -> {
						try {
							ZipUtils.zipFile(f, folder, zio, true, fileFilter);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
					zio.flush();
				} catch (UncheckedIOException e) {
					throw e.getCause();
				}
			}
			final HttpPost post = new HttpPost(uriBuilder.build().toString());
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//			builder.addPart("file", new FileBody(tempFile.toFile(), "temp.zip", "application/zip", "utf-8"));
			builder.addBinaryBody("file", tempFile.toFile(), ContentType.create("application/zip"), "temp.zip"); 
			final String configJson;
			try {
				configJson = jsonWriter.writeValueAsString(config);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Unexpected error",e);
			}
			builder.addTextBody("config", configJson, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
			HttpEntity entity = builder.build();
			//
			post.setEntity(entity);
			return client.execute(post, clientContext, new FutureCallback<HttpResponse>() {
				
				private final void cleanup() {
					try {
						Files.delete(tempFile);
					} catch (Exception io) {
						LoggerFactory.getLogger(FileUploadClientImpl.class).warn("Failed to delete temp file {}",tempFile, io);
					}	
				}
				
				@Override
				public void failed(Exception arg0) {
					cleanup();
				}
				
				@Override
				public void completed(HttpResponse arg0) {
					cleanup();
				}
				
				@Override
				public void cancelled() {
					cleanup();
				}
			});
		} catch (Throwable e) {
			try {
				Files.delete(tempFile);
			} catch (Exception io) {
				LoggerFactory.getLogger(FileUploadClientImpl.class).warn("Failed to delete temp file {}",tempFile, io);
			}
			throw e;
		}
	}
	
	@Override
	public Future<HttpResponse> upload(Path folder, String targetPath, FileConfiguration config, String filePrefix,
			Predicate<Path> fileFilter, DateTimeFormatter formatter) throws IOException, URISyntaxException, TimeoutException {
		Objects.requireNonNull(folder);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Future<HttpResponse>>() {
	
				@Override
				public Future<HttpResponse> run() throws Exception {
					return uploadInternal(folder, targetPath, config, filePrefix, fileFilter, formatter);
				}
			});
		} catch (PrivilegedActionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof IOException)
				throw (IOException) cause;
			if (cause instanceof URISyntaxException)
				throw (URISyntaxException) cause;
			if (cause instanceof TimeoutException)
				throw (TimeoutException) cause;
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			throw new RuntimeException(cause);
		}
	}
	
	
	private static String appendPath(String pathBase, String newPath) {
		if (pathBase == null || pathBase.isEmpty())
			return newPath;
		if (newPath == null || newPath.isEmpty())
			return pathBase;
		final StringBuilder sb = new StringBuilder();
		sb.append(pathBase);
		if (!pathBase.endsWith("/"))
			sb.append('/');
		if (newPath.startsWith("/"))
			sb.append(newPath.substring(1));
		else
			sb.append(newPath);
		return sb.toString();
	}

//	private static boolean filenameMatches(final Path path, final String filePrefix, final DateTimeFormatter format) {
//		return getInstant(path, filePrefix, format) != null;
//	}
	
	private static Instant getInstant(final Path path, final String filePrefix, final DateTimeFormatter format) {
		return getInstant(path.getFileName().toString(), filePrefix, format);
	}
	
	private static Instant getInstant(String filename, final String filePrefix, final DateTimeFormatter format) {
		if (filePrefix != null  ) {
			if (!filename.startsWith(filePrefix))
				return null;
			if (filePrefix != null)
				filename = filename.substring(filePrefix.length()); 
		}
		final int nextDot = filename.indexOf('.');
		if (nextDot >= 0)
			filename = filename.substring(0, nextDot);
		return DateTimeUtils.parseAsInstant(filename, format);
	}
	
}
