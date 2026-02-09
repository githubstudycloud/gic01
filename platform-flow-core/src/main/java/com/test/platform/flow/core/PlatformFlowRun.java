package com.test.platform.flow.core;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlatformFlowRun {
	private final String runId;
	private final String flowId;
	private final PlatformFlowRunStatus status;
	private final Instant createdAt;
	private final Instant startedAt;
	private final Instant endedAt;
	private final Map<String, Object> inputs;
	private final Set<String> targetStepIds;
	private final Map<String, PlatformFlowStepRun> steps;

	private PlatformFlowRun(String runId, String flowId, PlatformFlowRunStatus status, Instant createdAt,
			Instant startedAt, Instant endedAt, Map<String, Object> inputs, Set<String> targetStepIds,
			Map<String, PlatformFlowStepRun> steps) {
		this.runId = runId;
		this.flowId = flowId;
		this.status = status;
		this.createdAt = createdAt;
		this.startedAt = startedAt;
		this.endedAt = endedAt;
		this.inputs = inputs;
		this.targetStepIds = targetStepIds;
		this.steps = steps;
	}

	public static PlatformFlowRun newRunning(String runId, String flowId, Instant now, Map<String, Object> inputs,
			Set<String> targetStepIds, Set<String> plannedStepIds) {
		Map<String, PlatformFlowStepRun> stepRuns = new LinkedHashMap<>();
		for (String stepId : plannedStepIds) {
			stepRuns.put(stepId, PlatformFlowStepRun.pending(stepId));
		}
		return new PlatformFlowRun(runId, flowId, PlatformFlowRunStatus.RUNNING, now, now, null, copyInputs(inputs),
				Set.copyOf(targetStepIds), Collections.unmodifiableMap(stepRuns));
	}

	private static Map<String, Object> copyInputs(Map<String, Object> inputs) {
		if (inputs == null || inputs.isEmpty()) {
			return Map.of();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(inputs));
	}

	public PlatformFlowRun withStepRun(String stepId, PlatformFlowStepRun stepRun) {
		Map<String, PlatformFlowStepRun> next = new LinkedHashMap<>(this.steps);
		next.put(stepId, stepRun);
		return new PlatformFlowRun(runId, flowId, status, createdAt, startedAt, endedAt, inputs, targetStepIds,
				Collections.unmodifiableMap(next));
	}

	public PlatformFlowRun completed(Instant now, PlatformFlowRunStatus finalStatus) {
		return new PlatformFlowRun(runId, flowId, finalStatus, createdAt, startedAt, now, inputs, targetStepIds, steps);
	}

	public String getRunId() {
		return runId;
	}

	public String getFlowId() {
		return flowId;
	}

	public PlatformFlowRunStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Optional<Instant> getStartedAt() {
		return Optional.ofNullable(startedAt);
	}

	public Optional<Instant> getEndedAt() {
		return Optional.ofNullable(endedAt);
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}

	public Set<String> getTargetStepIds() {
		return targetStepIds;
	}

	public Map<String, PlatformFlowStepRun> getSteps() {
		return steps;
	}
}
