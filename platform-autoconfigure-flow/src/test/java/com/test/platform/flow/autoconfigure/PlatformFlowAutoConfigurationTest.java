package com.test.platform.flow.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.test.platform.flow.core.PlatformFlowEngine;
import com.test.platform.flow.autoconfigure.web.PlatformFlowController;
import com.test.platform.flow.autoconfigure.web.PlatformFlowWebAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class PlatformFlowAutoConfigurationTest {
	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(PlatformFlowAutoConfiguration.class, PlatformFlowWebAutoConfiguration.class));

	@Test
	void registersEngineByDefault() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(PlatformFlowEngine.class));
	}

	@Test
	void registersWebControllerByDefault() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(PlatformFlowController.class));
	}

	@Test
	void canDisableFlow() {
		contextRunner.withPropertyValues("platform.flow.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(PlatformFlowEngine.class));
	}

	@Test
	void canDisableWebEndpoints() {
		contextRunner.withPropertyValues("platform.flow.web-enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(PlatformFlowController.class));
	}
}
