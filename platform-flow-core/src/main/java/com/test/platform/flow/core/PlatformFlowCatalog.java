package com.test.platform.flow.core;

import com.test.platform.flow.spi.PlatformFlowDefinition;
import com.test.platform.flow.spi.PlatformFlowStep;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validated catalog of flow definitions and steps.
 */
public final class PlatformFlowCatalog {
	private final Map<String, PlatformFlowDefinition> flowsById;
	private final Map<String, PlatformFlowStep> stepsById;

	private PlatformFlowCatalog(Map<String, PlatformFlowDefinition> flowsById,
			Map<String, PlatformFlowStep> stepsById) {
		this.flowsById = Map.copyOf(flowsById);
		this.stepsById = Map.copyOf(stepsById);
	}

	public static PlatformFlowCatalog of(Collection<PlatformFlowDefinition> flows, Collection<PlatformFlowStep> steps) {
		Objects.requireNonNull(flows, "flows");
		Objects.requireNonNull(steps, "steps");

		Map<String, PlatformFlowDefinition> flowsById = new HashMap<>();
		for (PlatformFlowDefinition f : flows) {
			if (flowsById.putIfAbsent(f.id(), f) != null) {
				throw new IllegalArgumentException("Duplicate flow id: " + f.id());
			}
		}

		Map<String, PlatformFlowStep> stepsById = new HashMap<>();
		for (PlatformFlowStep s : steps) {
			if (stepsById.putIfAbsent(s.id(), s) != null) {
				throw new IllegalArgumentException("Duplicate step id: " + s.id());
			}
		}

		for (PlatformFlowDefinition f : flowsById.values()) {
			validateFlow(f, stepsById);
		}

		return new PlatformFlowCatalog(flowsById, stepsById);
	}

	private static void validateFlow(PlatformFlowDefinition f, Map<String, PlatformFlowStep> stepsById) {
		if (f.stepIds() == null || f.stepIds().isEmpty()) {
			throw new IllegalArgumentException("Flow has no steps: " + f.id());
		}
		for (String stepId : f.stepIds()) {
			if (!stepsById.containsKey(stepId)) {
				throw new IllegalArgumentException("Flow " + f.id() + " references missing step: " + stepId);
			}
		}
		for (String stepId : f.stepIds()) {
			PlatformFlowStep step = stepsById.get(stepId);
			for (String req : step.requiredStepIds()) {
				if (!f.stepIds().contains(req)) {
					throw new IllegalArgumentException(
							"Flow " + f.id() + " step " + stepId + " requires non-member step: " + req);
				}
			}
		}
		detectCycles(f, stepsById);
	}

	private static void detectCycles(PlatformFlowDefinition f, Map<String, PlatformFlowStep> stepsById) {
		Set<String> visiting = new HashSet<>();
		Set<String> visited = new HashSet<>();
		for (String stepId : f.stepIds()) {
			dfs(stepId, f, stepsById, visiting, visited);
		}
	}

	private static void dfs(String stepId, PlatformFlowDefinition f, Map<String, PlatformFlowStep> stepsById,
			Set<String> visiting, Set<String> visited) {
		if (visited.contains(stepId)) {
			return;
		}
		if (visiting.contains(stepId)) {
			throw new IllegalArgumentException("Cycle detected in flow " + f.id() + " at step: " + stepId);
		}
		visiting.add(stepId);
		for (String dep : stepsById.get(stepId).requiredStepIds()) {
			if (f.stepIds().contains(dep)) {
				dfs(dep, f, stepsById, visiting, visited);
			}
		}
		visiting.remove(stepId);
		visited.add(stepId);
	}

	public PlatformFlowDefinition getFlow(String flowId) {
		PlatformFlowDefinition f = flowsById.get(flowId);
		if (f == null) {
			throw new IllegalArgumentException("Unknown flow: " + flowId);
		}
		return f;
	}

	public PlatformFlowStep getStep(String stepId) {
		PlatformFlowStep s = stepsById.get(stepId);
		if (s == null) {
			throw new IllegalArgumentException("Unknown step: " + stepId);
		}
		return s;
	}

	public Map<String, PlatformFlowDefinition> flows() {
		return flowsById;
	}

	public Map<String, PlatformFlowStep> steps() {
		return stepsById;
	}
}
