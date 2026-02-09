package com.test.platform.sample.biz.workflow;

import com.test.platform.flow.spi.PlatformFlowDefinition;
import com.test.platform.flow.spi.PlatformFlowStep;
import com.test.platform.flow.spi.PlatformFlowStepContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.sample.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformSampleWorkflowAutoConfiguration {
	@Bean
	public PlatformFlowDefinition demoWorkflowReleaseFlow() {
		return new PlatformFlowDefinition() {
			@Override
			public String id() {
				return "demo.workflow.release";
			}

			@Override
			public Set<String> stepIds() {
				return Set.of("wf.prepare", "wf.build", "wf.scan", "wf.test", "wf.deploy", "wf.notify");
			}

			@Override
			public Set<String> defaultTargetStepIds() {
				return Set.of("wf.notify");
			}
		};
	}

	@Bean
	public PlatformFlowStep wfPrepareStep() {
		return new SimpleStep("wf.prepare", Set.of(), ctx -> {
			maybeFail(ctx, "wf.prepare");
			ctx.artifacts().put("wf.plan",
					Map.of("ts", Instant.now().toString(), "version",
							String.valueOf(ctx.inputs().getOrDefault("version", "0.0.0")), "env",
							String.valueOf(ctx.inputs().getOrDefault("env", "dev"))));
		});
	}

	@Bean
	public PlatformFlowStep wfBuildStep() {
		return new SimpleStep("wf.build", Set.of("wf.prepare"), ctx -> {
			maybeFail(ctx, "wf.build");
			ctx.artifacts().put("wf.buildId", "build-" + ctx.runId());
		});
	}

	@Bean
	public PlatformFlowStep wfScanStep() {
		return new SimpleStep("wf.scan", Set.of("wf.prepare"), ctx -> {
			maybeFail(ctx, "wf.scan");
			ctx.artifacts().put("wf.scanReport", Map.of("ts", Instant.now().toString(), "status", "PASS", "issues", 0));
		});
	}

	@Bean
	public PlatformFlowStep wfTestStep() {
		return new SimpleStep("wf.test", Set.of("wf.build"), ctx -> {
			maybeFail(ctx, "wf.test");
			ctx.artifacts().put("wf.testReport", Map.of("ts", Instant.now().toString(), "passed", true));
		});
	}

	@Bean
	public PlatformFlowStep wfDeployStep() {
		return new SimpleStep("wf.deploy", Set.of("wf.test", "wf.scan"), ctx -> {
			maybeFail(ctx, "wf.deploy");
			Map<String, Object> deployment = new HashMap<>();
			deployment.put("ts", Instant.now().toString());
			deployment.put("artifact", ctx.artifacts().get("wf.buildId", String.class).orElse("n/a"));
			deployment.put("env", String.valueOf(ctx.inputs().getOrDefault("env", "dev")));
			ctx.artifacts().put("wf.deployment", Map.copyOf(deployment));
		});
	}

	@Bean
	public PlatformFlowStep wfNotifyStep() {
		return new SimpleStep("wf.notify", Set.of("wf.deploy"), ctx -> {
			maybeFail(ctx, "wf.notify");
			ctx.artifacts().put("wf.notification",
					Map.of("ts", Instant.now().toString(), "message", "Release completed", "runId", ctx.runId()));
		});
	}

	@FunctionalInterface
	private interface StepAction {
		void run(PlatformFlowStepContext ctx) throws Exception;
	}

	private static final class SimpleStep implements PlatformFlowStep {
		private static final Logger log = LoggerFactory.getLogger(SimpleStep.class);

		private final String id;
		private final Set<String> deps;
		private final StepAction action;

		SimpleStep(String id, Set<String> deps, StepAction action) {
			this.id = id;
			this.deps = deps == null ? Set.of() : Set.copyOf(deps);
			this.action = action;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public Set<String> requiredStepIds() {
			return deps;
		}

		@Override
		public void execute(PlatformFlowStepContext context) throws Exception {
			action.run(context);
			log.debug("Step {} done", id);
		}
	}

	private static void maybeFail(PlatformFlowStepContext ctx, String stepId) {
		Object v = ctx.inputs().get("failStepId");
		if (v != null && stepId.equals(String.valueOf(v))) {
			throw new RuntimeException("Injected failure at step: " + stepId);
		}
	}
}
