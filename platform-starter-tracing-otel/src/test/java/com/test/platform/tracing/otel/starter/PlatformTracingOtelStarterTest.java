package com.test.platform.tracing.otel.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = PlatformTracingOtelStarterTest.TestApp.class, properties = {"management.tracing.enabled=true",
		"management.tracing.sampling.probability=1.0"})
class PlatformTracingOtelStarterTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void providesTracerBean() {
		assertThat(applicationContext.getBean(Tracer.class)).isNotNull();
	}

	@Configuration
	@EnableAutoConfiguration
	static class TestApp {
	}
}
