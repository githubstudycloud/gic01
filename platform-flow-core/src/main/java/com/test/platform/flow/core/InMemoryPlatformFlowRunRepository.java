package com.test.platform.flow.core;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class InMemoryPlatformFlowRunRepository implements PlatformFlowRunRepository {
	private final ConcurrentHashMap<String, PlatformFlowRun> byId = new ConcurrentHashMap<>();

	@Override
	public PlatformFlowRun createRun(PlatformFlowRun run) {
		byId.put(run.getRunId(), run);
		return run;
	}

	@Override
	public Optional<PlatformFlowRun> findById(String runId) {
		return Optional.ofNullable(byId.get(runId));
	}

	@Override
	public List<PlatformFlowRun> listByFlowId(String flowId, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		return byId.values().stream().filter(r -> r.getFlowId().equals(flowId))
				.sorted(Comparator.comparing(PlatformFlowRun::getCreatedAt).reversed()).limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	public void updateStepRun(String runId, String stepId, UnaryOperator<PlatformFlowStepRun> updater) {
		byId.computeIfPresent(runId, (_id, current) -> {
			PlatformFlowStepRun stepRun = current.getSteps().get(stepId);
			if (stepRun == null) {
				return current;
			}
			return current.withStepRun(stepId, updater.apply(stepRun));
		});
	}

	@Override
	public void updateRun(String runId, UnaryOperator<PlatformFlowRun> updater) {
		byId.computeIfPresent(runId, (_id, current) -> updater.apply(current));
	}
}
