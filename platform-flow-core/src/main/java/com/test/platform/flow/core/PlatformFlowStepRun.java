package com.test.platform.flow.core;

import java.time.Instant;
import java.util.Optional;

public final class PlatformFlowStepRun {
	private final String stepId;
	private final PlatformFlowStepStatus status;
	private final Instant startedAt;
	private final Instant endedAt;
	private final String errorMessage;

	private PlatformFlowStepRun(String stepId, PlatformFlowStepStatus status, Instant startedAt, Instant endedAt,
			String errorMessage) {
		this.stepId = stepId;
		this.status = status;
		this.startedAt = startedAt;
		this.endedAt = endedAt;
		this.errorMessage = errorMessage;
	}

	public static PlatformFlowStepRun pending(String stepId) {
		return new PlatformFlowStepRun(stepId, PlatformFlowStepStatus.PENDING, null, null, null);
	}

	public PlatformFlowStepRun running(Instant now) {
		return new PlatformFlowStepRun(stepId, PlatformFlowStepStatus.RUNNING, now, null, null);
	}

	public PlatformFlowStepRun succeeded(Instant now) {
		return new PlatformFlowStepRun(stepId, PlatformFlowStepStatus.SUCCEEDED, startedAt, now, null);
	}

	public PlatformFlowStepRun skipped(Instant now, String reason) {
		return new PlatformFlowStepRun(stepId, PlatformFlowStepStatus.SKIPPED, startedAt, now, reason);
	}

	public PlatformFlowStepRun failed(Instant now, String message) {
		return new PlatformFlowStepRun(stepId, PlatformFlowStepStatus.FAILED, startedAt, now, message);
	}

	public String getStepId() {
		return stepId;
	}

	public PlatformFlowStepStatus getStatus() {
		return status;
	}

	public Optional<Instant> getStartedAt() {
		return Optional.ofNullable(startedAt);
	}

	public Optional<Instant> getEndedAt() {
		return Optional.ofNullable(endedAt);
	}

	public Optional<String> getErrorMessage() {
		return Optional.ofNullable(errorMessage);
	}
}
