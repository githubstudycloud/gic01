package com.test.platform.sample.biz.metrics.collector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformSampleMetricsCollectorAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PlatformSampleMetricsCollectorAutoConfiguration.class));

	@Test
	void registersCollectorStepsByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasBean("collectHostStep");
			assertThat(context).hasBean("collectJvmStep");
			assertThat(context).hasBean("collectProcessStep");
		});
	}

	@Test
	void canDisableModule() {
		contextRunner.withPropertyValues("platform.sample.metrics.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean("collectHostStep"));
	}
}
