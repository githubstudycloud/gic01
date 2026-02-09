package com.test.platform.logging.autoconfigure;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Sets platform logging defaults early, before the logging system initializes.
 *
 * <p>
 * We keep this as a small, override-friendly default:
 *
 * <ul>
 * <li>Do nothing if {@code logging.config} is already set
 * <li>Do nothing if {@code platform.logging.enabled=false}
 * <li>Do nothing if the logstash encoder is not on the classpath
 * </ul>
 */
public final class PlatformLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String PROPERTY_ENABLED = "platform.logging.enabled";
	static final String PROPERTY_LOGGING_CONFIG = "logging.config";
	static final String DEFAULT_LOGGING_CONFIG = "classpath:platform/logging/logback-spring.xml";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!isEnabled(environment)) {
			return;
		}
		if (environment.containsProperty(PROPERTY_LOGGING_CONFIG)) {
			return;
		}
		if (!isLogstashEncoderPresent()) {
			return;
		}

		environment.getPropertySources().addLast(new MapPropertySource("platformLoggingDefaults",
				Map.of(PROPERTY_LOGGING_CONFIG, DEFAULT_LOGGING_CONFIG)));
	}

	private boolean isEnabled(ConfigurableEnvironment environment) {
		return environment.getProperty(PROPERTY_ENABLED, Boolean.class, Boolean.TRUE);
	}

	private boolean isLogstashEncoderPresent() {
		// Keep it cheap: the class is provided by platform-starter-logging.
		try {
			Class.forName("net.logstash.logback.encoder.LogstashEncoder", false, getClass().getClassLoader());
			return true;
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}

	@Override
	public int getOrder() {
		// Run early so Logback picks up logging.config before initialization.
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}
}
