package com.test.platform.lock.autoconfigure;

import com.test.platform.lock.adapter.local.LocalLockClient;
import com.test.platform.lock.spi.LockClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PlatformLockProperties.class)
public class PlatformLockAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(LockClient.class)
  @ConditionalOnProperty(
      prefix = "platform.lock",
      name = "provider",
      havingValue = "local",
      matchIfMissing = true)
  public LockClient platformLocalLockClient() {
    return new LocalLockClient();
  }
}

