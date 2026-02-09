package com.test.platform.sample.biz.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformSampleWorkflowAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PlatformSampleWorkflowAutoConfiguration.class));

	@Test
	void registersFlowAndStepsByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasBean("demoWorkflowReleaseFlow");
			assertThat(context).hasBean("wfPrepareStep");
			assertThat(context).hasBean("wfNotifyStep");
		});
	}

	@Test
	void canDisableModule() {
		contextRunner.withPropertyValues("platform.sample.workflow.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean("demoWorkflowReleaseFlow"));
	}
}
