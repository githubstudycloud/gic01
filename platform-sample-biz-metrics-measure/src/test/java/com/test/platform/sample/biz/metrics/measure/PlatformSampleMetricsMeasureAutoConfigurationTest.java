package com.test.platform.sample.biz.metrics.measure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformSampleMetricsMeasureAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PlatformSampleMetricsMeasureAutoConfiguration.class));

	@Test
	void registersMeasureStepsByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasBean("measureJvmStep");
			assertThat(context).hasBean("measureHostStep");
			assertThat(context).hasBean("measureSummaryStep");
		});
	}

	@Test
	void canDisableModule() {
		contextRunner.withPropertyValues("platform.sample.metrics.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean("measureSummaryStep"));
	}
}
