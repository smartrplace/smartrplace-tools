package org.test.felix.tests;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@ExamReactorStrategy(PerClass.class)
@RunWith(PaxExam.class)
public class FelixTest {


	@Configuration
	public Option[] configuration() throws IOException {
		return new Option[] {
				CoreOptions.vmOption("-ea"), 
				CoreOptions.junitBundles(),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.framework.security", "2.6.0"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.1.2"),
		};
	}

	@Test
	public void startupWorks() {
	}

	
}
