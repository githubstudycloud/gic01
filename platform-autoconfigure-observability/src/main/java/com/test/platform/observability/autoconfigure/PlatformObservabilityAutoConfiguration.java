package com.test.platform.observability.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration
@EnableConfigurationProperties(PlatformRequestIdProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OncePerRequestFilter.class)
public class PlatformObservabilityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "platform.observability.request-id",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public PlatformRequestIdFilter platformRequestIdFilter(PlatformRequestIdProperties properties) {
    return new PlatformRequestIdFilter(properties);
  }
}

