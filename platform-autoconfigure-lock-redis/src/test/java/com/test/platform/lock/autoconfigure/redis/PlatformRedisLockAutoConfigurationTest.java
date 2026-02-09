package com.test.platform.lock.autoconfigure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.test.platform.lock.adapter.redis.RedisLockClient;
import com.test.platform.lock.spi.LockClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

class PlatformRedisLockAutoConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(PlatformRedisLockAutoConfiguration.class));

  @Test
  void registersRedisLockClientWhenProviderSelectedAndTemplateExists() {
    contextRunner
        .withPropertyValues("platform.lock.provider=redis")
        .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(LockClient.class);
              assertThat(context.getBean(LockClient.class)).isInstanceOf(RedisLockClient.class);
            });
  }
}

