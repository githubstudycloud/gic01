package com.test.platform.flow.core;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Storage for flow runs and step run states.
 *
 * <p>
 * This is a port: production adapters may use a durable store.
 */
public interface PlatformFlowRunRepository {
	PlatformFlowRun createRun(PlatformFlowRun run);

	Optional<PlatformFlowRun> findById(String runId);

	List<PlatformFlowRun> listByFlowId(String flowId, int limit);

	/**
	 * Atomically updates a single step run.
	 */
	void updateStepRun(String runId, String stepId, UnaryOperator<PlatformFlowStepRun> updater);

	/**
	 * Atomically updates the run as a whole (e.g. completion).
	 */
	void updateRun(String runId, UnaryOperator<PlatformFlowRun> updater);
}
