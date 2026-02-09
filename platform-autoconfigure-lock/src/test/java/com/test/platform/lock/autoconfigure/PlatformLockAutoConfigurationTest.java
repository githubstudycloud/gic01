package com.test.platform.lock.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.test.platform.lock.spi.LockClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformLockAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PlatformLockAutoConfiguration.class));

	@Test
	void registersLocalLockClientByDefault() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(LockClient.class));
	}

	@Test
	void doesNotRegisterLocalLockClientWhenRedisProviderSelected() {
		contextRunner.withPropertyValues("platform.lock.provider=redis")
				.run(context -> assertThat(context).doesNotHaveBean(LockClient.class));
	}
}
