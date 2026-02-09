package com.test.platform.flow.spi;

import java.util.Set;

/**
 * A named flow that composes a set of step ids.
 *
 * <p>
 * A flow is just a "view" over steps; execution order is derived from the step
 * dependency graph.
 */
public interface PlatformFlowDefinition {
	/**
	 * Stable identifier of this flow (used in APIs, dashboards, and storage keys).
	 */
	String id();

	/**
	 * All step ids that belong to this flow.
	 *
	 * <p>
	 * This acts as a boundary: only these steps can be scheduled when running the
	 * flow.
	 */
	Set<String> stepIds();

	/**
	 * Default target steps for a full run.
	 *
	 * <p>
	 * If a run request does not specify targets, the engine will use these.
	 */
	Set<String> defaultTargetStepIds();
}
