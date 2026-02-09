package com.test.platform.flow.core;

import java.util.Map;
import java.util.Optional;

/**
 * Storage for run-scoped artifacts.
 *
 * <p>
 * This is a port: production adapters may use Redis/DB/object storage.
 */
public interface PlatformFlowArtifactStore {
	void put(String runId, String key, Object value);

	<T> Optional<T> get(String runId, String key, Class<T> type);

	Map<String, Object> snapshot(String runId);

	void deleteRun(String runId);
}
