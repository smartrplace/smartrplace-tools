package org.smartrplace.tools.upload.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.smartrplace.tools.upload.api.FileConfiguration;

@ExamReactorStrategy(PerMethod.class)
public class BlockingTest extends SimpleContextTestBase {

	@Test
	public void userGetsBlockedAfterTooManyRequests() throws IOException, InterruptedException, URISyntaxException, ExecutionException, TimeoutException {
		final String test = "testString";
		final FileConfiguration cfg = new FileConfiguration();
		cfg.filePrefix = nextFilePrefix();
		cfg.fileEnding = ".json";
		int lastStatus = -1;
		for (int i=0; i<10; i++) { // default nr of allowed requests per minute is 4
			final byte[] bytes = test.getBytes(StandardCharsets.UTF_8);
			final InputStream stream = new ByteArrayInputStream(bytes);
			final Future<HttpResponse> future;
			future = client.upload(stream, bytes.length, null, null, cfg);
			final HttpResponse response;
			response = future.get(5, TimeUnit.SECONDS);
			lastStatus = response.getStatusLine().getStatusCode();
			if (lastStatus == 429)
				break;
		}
		Assert.assertEquals("User was never blocked", 429, lastStatus);
	}
	
}
