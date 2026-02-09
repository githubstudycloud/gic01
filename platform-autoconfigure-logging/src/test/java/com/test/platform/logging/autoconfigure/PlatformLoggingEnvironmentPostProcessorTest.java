package com.test.platform.logging.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class PlatformLoggingEnvironmentPostProcessorTest {

	private final PlatformLoggingEnvironmentPostProcessor postProcessor = new PlatformLoggingEnvironmentPostProcessor();

	@Test
	void setsLoggingConfigWhenEnabledAndNotAlreadySet() {
		StandardEnvironment env = new StandardEnvironment();

		postProcessor.postProcessEnvironment(env, new SpringApplication(Object.class));

		assertThat(env.getProperty(PlatformLoggingEnvironmentPostProcessor.PROPERTY_LOGGING_CONFIG))
				.isEqualTo(PlatformLoggingEnvironmentPostProcessor.DEFAULT_LOGGING_CONFIG);
	}

	@Test
	void doesNotOverrideExistingLoggingConfig() {
		StandardEnvironment env = new StandardEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("test", Map
				.of(PlatformLoggingEnvironmentPostProcessor.PROPERTY_LOGGING_CONFIG, "classpath:custom-logback.xml")));

		postProcessor.postProcessEnvironment(env, new SpringApplication(Object.class));

		assertThat(env.getProperty(PlatformLoggingEnvironmentPostProcessor.PROPERTY_LOGGING_CONFIG))
				.isEqualTo("classpath:custom-logback.xml");
	}

	@Test
	void canBeDisabledViaProperty() {
		StandardEnvironment env = new StandardEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("test",
				Map.of(PlatformLoggingEnvironmentPostProcessor.PROPERTY_ENABLED, "false")));

		postProcessor.postProcessEnvironment(env, new SpringApplication(Object.class));

		assertThat(env.getProperty(PlatformLoggingEnvironmentPostProcessor.PROPERTY_LOGGING_CONFIG)).isNull();
	}
}
