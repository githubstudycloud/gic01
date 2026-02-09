package com.test.platform.flow.spi;

import java.util.Map;
import java.util.Optional;

/**
 * A run-scoped artifact bag shared by steps.
 *
 * <p>
 * This is intentionally simple for fast prototyping and modular composition.
 * Production adapters may back this with Redis/DB/object storage and only store
 * JSON-serializable values.
 */
public interface PlatformFlowStepArtifacts {
	void put(String key, Object value);

	<T> Optional<T> get(String key, Class<T> type);

	/**
	 * A read-only snapshot for debugging/inspection.
	 */
	Map<String, Object> snapshot();
}
