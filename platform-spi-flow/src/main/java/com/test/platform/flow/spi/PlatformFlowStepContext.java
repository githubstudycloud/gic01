package com.test.platform.flow.spi;

import java.util.Map;

/**
 * Execution context for a single step.
 */
public interface PlatformFlowStepContext {
	String runId();

	String flowId();

	/**
	 * Immutable inputs provided when the run was started.
	 */
	Map<String, Object> inputs();

	PlatformFlowStepArtifacts artifacts();
}
