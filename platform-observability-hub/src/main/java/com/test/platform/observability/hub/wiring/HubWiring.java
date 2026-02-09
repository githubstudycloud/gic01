package com.test.platform.observability.hub.wiring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.platform.observability.hub.config.HubProperties;
import com.test.platform.observability.hub.core.ServiceSnapshotFetcher;
import com.test.platform.observability.hub.core.http.HttpActuatorSnapshotFetcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HubProperties.class)
public class HubWiring {
  @Bean
  @ConditionalOnMissingBean(ObjectMapper.class)
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public ServiceSnapshotFetcher serviceSnapshotFetcher(ObjectMapper objectMapper) {
    return new HttpActuatorSnapshotFetcher(objectMapper);
  }
}
