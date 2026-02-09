package com.test.platform.sample.biz.metrics.flow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformSampleMetricsFlowAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PlatformSampleMetricsFlowAutoConfiguration.class));

	@Test
	void registersFlowDefinitionByDefault() {
		contextRunner.run(context -> assertThat(context).hasBean("demoMetricsFlow"));
	}

	@Test
	void canDisableModule() {
		contextRunner.withPropertyValues("platform.sample.metrics.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean("demoMetricsFlow"));
	}
}
