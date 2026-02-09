package com.test.platform.lock.autoconfigure.redis;

import com.test.platform.lock.adapter.redis.RedisLockClient;
import com.test.platform.lock.autoconfigure.PlatformLockProperties;
import com.test.platform.lock.spi.LockClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(PlatformLockProperties.class)
@ConditionalOnProperty(prefix = "platform.lock", name = "provider", havingValue = "redis")
@ConditionalOnClass(StringRedisTemplate.class)
public class PlatformRedisLockAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(LockClient.class)
  @ConditionalOnBean(StringRedisTemplate.class)
  public LockClient platformRedisLockClient(
      StringRedisTemplate stringRedisTemplate, PlatformLockProperties properties) {
    return new RedisLockClient(stringRedisTemplate, properties.getRedis().getKeyPrefix());
  }
}

