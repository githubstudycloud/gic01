package com.test.platform.flow.autoconfigure.web;

import com.test.platform.flow.core.PlatformFlowArtifactStore;
import com.test.platform.flow.core.PlatformFlowCatalog;
import com.test.platform.flow.core.PlatformFlowEngine;
import com.test.platform.flow.core.PlatformFlowRun;
import com.test.platform.flow.core.PlatformFlowRunRepository;
import com.test.platform.flow.core.PlatformFlowRunRequest;
import com.test.platform.flow.core.PlatformFlowStepRun;
import com.test.platform.flow.spi.PlatformFlowDefinition;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/flows")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "platform.flow", name = "web-enabled", havingValue = "true", matchIfMissing = true)
public class PlatformFlowController {
	private final PlatformFlowCatalog catalog;
	private final PlatformFlowEngine engine;
	private final PlatformFlowRunRepository runRepository;
	private final PlatformFlowArtifactStore artifactStore;

	public PlatformFlowController(PlatformFlowCatalog catalog, PlatformFlowEngine engine,
			PlatformFlowRunRepository runRepository, PlatformFlowArtifactStore artifactStore) {
		this.catalog = catalog;
		this.engine = engine;
		this.runRepository = runRepository;
		this.artifactStore = artifactStore;
	}

	@GetMapping
	public List<FlowDto> listFlows() {
		return catalog.flows().values().stream().map(PlatformFlowController::toDto)
				.sorted((a, b) -> a.id.compareTo(b.id)).toList();
	}

	@GetMapping("/{flowId}")
	public FlowDto getFlow(@PathVariable String flowId) {
		return toDto(catalog.getFlow(flowId));
	}

	@PostMapping("/{flowId}/runs")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public StartRunResponse start(@PathVariable String flowId, @RequestBody(required = false) StartRunRequest req) {
		Map<String, Object> inputs = req == null || req.inputs == null ? Map.of() : req.inputs;
		Set<String> targets = req == null || req.targets == null ? Set.of() : req.targets;
		String runId = engine.start(flowId, new PlatformFlowRunRequest(inputs, targets));
		return new StartRunResponse(runId);
	}

	@GetMapping("/{flowId}/runs")
	public List<RunDto> listRuns(@PathVariable String flowId, @RequestParam(defaultValue = "10") int limit) {
		return runRepository.listByFlowId(flowId, Math.min(Math.max(limit, 1), 100)).stream()
				.map(r -> toRunDto(r, artifactStore.snapshot(r.getRunId()).keySet())).toList();
	}

	@GetMapping("/{flowId}/runs/{runId}")
	public RunDto getRun(@PathVariable String flowId, @PathVariable String runId) {
		PlatformFlowRun run = runRepository.findById(runId).orElseThrow();
		if (!run.getFlowId().equals(flowId)) {
			throw new IllegalArgumentException("Run does not belong to flow " + flowId + ": " + runId);
		}
		return toRunDto(run, artifactStore.snapshot(runId).keySet());
	}

	@GetMapping("/{flowId}/runs/{runId}/artifacts")
	public Map<String, Object> getRunArtifacts(@PathVariable String flowId, @PathVariable String runId) {
		PlatformFlowRun run = runRepository.findById(runId).orElseThrow();
		if (!run.getFlowId().equals(flowId)) {
			throw new IllegalArgumentException("Run does not belong to flow " + flowId + ": " + runId);
		}
		return artifactStore.snapshot(runId);
	}

	@PostMapping("/{flowId}/runs/{runId}/retry")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public StartRunResponse retry(@PathVariable String flowId, @PathVariable String runId) {
		PlatformFlowRun previous = runRepository.findById(runId).orElseThrow();
		if (!previous.getFlowId().equals(flowId)) {
			throw new IllegalArgumentException("Run does not belong to flow " + flowId + ": " + runId);
		}
		String newRunId = engine.start(flowId,
				new PlatformFlowRunRequest(previous.getInputs(), previous.getTargetStepIds()));
		return new StartRunResponse(newRunId);
	}

	private static FlowDto toDto(PlatformFlowDefinition f) {
		return new FlowDto(f.id(), f.stepIds(), f.defaultTargetStepIds());
	}

	private static RunDto toRunDto(PlatformFlowRun run, Set<String> artifactKeys) {
		List<StepRunDto> steps = run.getSteps().values().stream().map(PlatformFlowController::toStepDto)
				.sorted((a, b) -> a.stepId.compareTo(b.stepId)).toList();
		return new RunDto(run.getRunId(), run.getFlowId(), run.getStatus().name(), run.getCreatedAt(),
				run.getStartedAt().orElse(null), run.getEndedAt().orElse(null), run.getTargetStepIds(), artifactKeys,
				steps);
	}

	private static StepRunDto toStepDto(PlatformFlowStepRun step) {
		return new StepRunDto(step.getStepId(), step.getStatus().name(), step.getStartedAt().orElse(null),
				step.getEndedAt().orElse(null), step.getErrorMessage().orElse(null));
	}

	public static final class StartRunRequest {
		public Map<String, Object> inputs;
		public Set<String> targets;
	}

	public record StartRunResponse(String runId) {
	}

	public record FlowDto(String id, Set<String> stepIds, Set<String> defaultTargets) {
	}

	public record RunDto(String runId, String flowId, String status, Instant createdAt, Instant startedAt,
			Instant endedAt, Set<String> targets, Set<String> artifactKeys, List<StepRunDto> steps) {
	}

	public record StepRunDto(String stepId, String status, Instant startedAt, Instant endedAt, String errorMessage) {
	}
}
