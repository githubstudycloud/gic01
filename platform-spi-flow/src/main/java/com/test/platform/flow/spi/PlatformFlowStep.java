package com.test.platform.flow.spi;

import java.util.Set;

/**
 * A single unit of work inside a flow.
 *
 * <p>
 * Steps declare their dependencies by step id; this allows modules to be
 * composed without direct compile-time references.
 */
public interface PlatformFlowStep {
	/**
	 * Stable identifier of this step (namespaced is recommended, e.g.
	 * {@code collect.cpu}).
	 */
	String id();

	/**
	 * Step ids that must succeed before this step can run.
	 */
	Set<String> requiredStepIds();

	/**
	 * Execute the step.
	 *
	 * <p>
	 * Use {@link PlatformFlowStepContext#artifacts()} to write outputs for
	 * downstream steps.
	 */
	void execute(PlatformFlowStepContext context) throws Exception;
}
