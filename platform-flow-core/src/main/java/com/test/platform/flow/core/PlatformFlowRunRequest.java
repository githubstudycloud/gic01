package com.test.platform.flow.core;

import java.util.Map;
import java.util.Set;

public final class PlatformFlowRunRequest {
	private final Map<String, Object> inputs;
	private final Set<String> targetStepIds;

	public PlatformFlowRunRequest(Map<String, Object> inputs, Set<String> targetStepIds) {
		this.inputs = inputs == null ? Map.of() : inputs;
		this.targetStepIds = targetStepIds == null ? Set.of() : Set.copyOf(targetStepIds);
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}

	public Set<String> getTargetStepIds() {
		return targetStepIds;
	}
}
