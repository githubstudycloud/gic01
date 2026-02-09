package com.test.platform.flow.autoconfigure.web;

import com.test.platform.flow.core.PlatformFlowArtifactStore;
import com.test.platform.flow.core.PlatformFlowCatalog;
import com.test.platform.flow.core.PlatformFlowEngine;
import com.test.platform.flow.core.PlatformFlowRunRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "platform.flow", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "platform.flow", name = "web-enabled", havingValue = "true", matchIfMissing = true)
public class PlatformFlowWebAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	public PlatformFlowController platformFlowController(PlatformFlowCatalog catalog, PlatformFlowEngine engine,
			PlatformFlowRunRepository runRepository, PlatformFlowArtifactStore artifactStore) {
		return new PlatformFlowController(catalog, engine, runRepository, artifactStore);
	}
}
