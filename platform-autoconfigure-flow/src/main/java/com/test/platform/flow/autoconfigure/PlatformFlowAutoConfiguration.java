package com.test.platform.flow.autoconfigure;

import com.test.platform.flow.core.InMemoryPlatformFlowArtifactStore;
import com.test.platform.flow.core.InMemoryPlatformFlowRunRepository;
import com.test.platform.flow.core.PlatformFlowArtifactStore;
import com.test.platform.flow.core.PlatformFlowCatalog;
import com.test.platform.flow.core.PlatformFlowEngine;
import com.test.platform.flow.core.PlatformFlowRunRepository;
import com.test.platform.flow.spi.PlatformFlowDefinition;
import com.test.platform.flow.spi.PlatformFlowStep;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PlatformFlowProperties.class)
@ConditionalOnProperty(prefix = "platform.flow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformFlowAutoConfiguration {
	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(name = "platformFlowExecutor")
	public ExecutorService platformFlowExecutor(PlatformFlowProperties properties) {
		int size = Math.max(1, properties.getMaxConcurrency());
		return Executors.newFixedThreadPool(size, newNamedThreadFactory("platform-flow-"));
	}

	@Bean
	@ConditionalOnMissingBean
	public PlatformFlowRunRepository platformFlowRunRepository() {
		return new InMemoryPlatformFlowRunRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public PlatformFlowArtifactStore platformFlowArtifactStore() {
		return new InMemoryPlatformFlowArtifactStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public PlatformFlowCatalog platformFlowCatalog(ObjectProvider<PlatformFlowDefinition> definitions,
			ObjectProvider<PlatformFlowStep> steps) {
		List<PlatformFlowDefinition> flowDefs = definitions.orderedStream().toList();
		List<PlatformFlowStep> flowSteps = steps.orderedStream().toList();
		return PlatformFlowCatalog.of(flowDefs, flowSteps);
	}

	@Bean
	@ConditionalOnMissingBean
	public PlatformFlowEngine platformFlowEngine(PlatformFlowCatalog catalog, PlatformFlowRunRepository runRepository,
			PlatformFlowArtifactStore artifactStore, ExecutorService platformFlowExecutor) {
		return new PlatformFlowEngine(catalog, runRepository, artifactStore, platformFlowExecutor);
	}

	private static ThreadFactory newNamedThreadFactory(String prefix) {
		AtomicInteger seq = new AtomicInteger();
		return r -> {
			Thread t = new Thread(r);
			t.setName(prefix + seq.incrementAndGet());
			t.setDaemon(true);
			return t;
		};
	}
}
