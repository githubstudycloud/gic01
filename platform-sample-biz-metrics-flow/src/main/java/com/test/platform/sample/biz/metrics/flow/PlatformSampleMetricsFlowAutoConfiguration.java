package com.test.platform.sample.biz.metrics.flow;

import com.test.platform.flow.spi.PlatformFlowDefinition;
import java.util.Set;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.sample.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformSampleMetricsFlowAutoConfiguration {
	@Bean
	public PlatformFlowDefinition demoMetricsFlow() {
		return new PlatformFlowDefinition() {
			@Override
			public String id() {
				return "demo.metrics";
			}

			@Override
			public Set<String> stepIds() {
				return Set.of("collect.host", "collect.jvm", "collect.proc", "measure.jvm", "measure.host",
						"measure.summary");
			}

			@Override
			public Set<String> defaultTargetStepIds() {
				return Set.of("measure.summary");
			}
		};
	}
}
