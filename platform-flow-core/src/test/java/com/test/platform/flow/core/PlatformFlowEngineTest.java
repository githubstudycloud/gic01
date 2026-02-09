package com.test.platform.flow.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.test.platform.flow.spi.PlatformFlowDefinition;
import com.test.platform.flow.spi.PlatformFlowStep;
import com.test.platform.flow.spi.PlatformFlowStepContext;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class PlatformFlowEngineTest {
	@Test
	void runsStepsWithDependencies() {
		CountDownLatch aStarted = new CountDownLatch(1);
		CountDownLatch bStarted = new CountDownLatch(1);

		PlatformFlowStep stepA = new PlatformFlowStep() {
			@Override
			public String id() {
				return "collect.a";
			}

			@Override
			public Set<String> requiredStepIds() {
				return Set.of();
			}

			@Override
			public void execute(PlatformFlowStepContext context) throws Exception {
				aStarted.countDown();
				// wait for B to start too (parallel scheduling)
				assertThat(bStarted.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
				context.artifacts().put("a", "A");
			}
		};

		PlatformFlowStep stepB = new PlatformFlowStep() {
			@Override
			public String id() {
				return "collect.b";
			}

			@Override
			public Set<String> requiredStepIds() {
				return Set.of();
			}

			@Override
			public void execute(PlatformFlowStepContext context) throws Exception {
				bStarted.countDown();
				assertThat(aStarted.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
				context.artifacts().put("b", "B");
			}
		};

		PlatformFlowStep stepC = new PlatformFlowStep() {
			@Override
			public String id() {
				return "metric.c";
			}

			@Override
			public Set<String> requiredStepIds() {
				return Set.of("collect.a", "collect.b");
			}

			@Override
			public void execute(PlatformFlowStepContext context) {
				assertThat(context.artifacts().get("a", String.class)).contains("A");
				assertThat(context.artifacts().get("b", String.class)).contains("B");
				context.artifacts().put("c", "C");
			}
		};

		PlatformFlowDefinition flow = new PlatformFlowDefinition() {
			@Override
			public String id() {
				return "test.flow";
			}

			@Override
			public Set<String> stepIds() {
				return Set.of("collect.a", "collect.b", "metric.c");
			}

			@Override
			public Set<String> defaultTargetStepIds() {
				return Set.of("metric.c");
			}
		};

		PlatformFlowCatalog catalog = PlatformFlowCatalog.of(Set.of(flow), Set.of(stepA, stepB, stepC));
		InMemoryPlatformFlowRunRepository repo = new InMemoryPlatformFlowRunRepository();
		InMemoryPlatformFlowArtifactStore artifacts = new InMemoryPlatformFlowArtifactStore();
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			PlatformFlowEngine engine = new PlatformFlowEngine(catalog, repo, artifacts, executor);

			String runId = engine.start("test.flow", new PlatformFlowRunRequest(Map.of(), Set.of()));
			engine.await(runId, Duration.ofSeconds(5));

			PlatformFlowRun run = repo.findById(runId).orElseThrow();
			assertThat(run.getStatus()).isEqualTo(PlatformFlowRunStatus.SUCCEEDED);
			assertThat(artifacts.get(runId, "c", String.class)).contains("C");
		} finally {
			executor.shutdownNow();
		}
	}
}
