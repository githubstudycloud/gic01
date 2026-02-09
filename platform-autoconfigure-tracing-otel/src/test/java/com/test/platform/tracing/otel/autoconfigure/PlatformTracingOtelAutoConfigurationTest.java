package com.test.platform.tracing.otel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = PlatformTracingOtelAutoConfigurationTest.TestApp.class, properties = {
		"platform.tracing.otel.export.enabled=false"})
class PlatformTracingOtelAutoConfigurationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void providesCoreBeans() {
		assertThat(applicationContext.getBean(OpenTelemetrySdk.class)).isNotNull();
		assertThat(applicationContext.getBean(Tracer.class)).isNotNull();
		assertThat(applicationContext.getBean(Propagator.class)).isNotNull();
	}

	@Configuration
	@EnableAutoConfiguration
	static class TestApp {
	}
}
