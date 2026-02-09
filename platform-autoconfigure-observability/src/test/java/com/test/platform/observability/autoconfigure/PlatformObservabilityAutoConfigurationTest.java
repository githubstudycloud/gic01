package com.test.platform.observability.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class PlatformObservabilityAutoConfigurationTest {
	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PlatformObservabilityAutoConfiguration.class));

	@Test
	void registersRequestIdFilterByDefault() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(PlatformRequestIdFilter.class));
	}

	@Test
	void canDisableRequestIdFilter() {
		contextRunner.withPropertyValues("platform.observability.request-id.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(PlatformRequestIdFilter.class));
	}
}
