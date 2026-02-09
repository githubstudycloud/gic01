package com.test.platform.sample.biz.crud;

import static org.assertj.core.api.Assertions.assertThat;

import com.test.platform.sample.biz.crud.entry.TodoController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class PlatformSampleCrudAutoConfigurationTest {
	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PlatformSampleCrudAutoConfiguration.class));

	@Test
	void registersControllerInWebApp() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(TodoController.class));
	}

	@Test
	void canDisableModule() {
		contextRunner.withPropertyValues("platform.sample.crud.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(TodoController.class));
	}
}
