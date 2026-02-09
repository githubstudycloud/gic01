package com.test.platform.flow.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPlatformFlowArtifactStore implements PlatformFlowArtifactStore {
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> byRun = new ConcurrentHashMap<>();

	@Override
	public void put(String runId, String key, Object value) {
		byRun.computeIfAbsent(runId, _ignored -> new ConcurrentHashMap<>()).put(key, value);
	}

	@Override
	public <T> Optional<T> get(String runId, String key, Class<T> type) {
		Object value = byRun.getOrDefault(runId, new ConcurrentHashMap<>()).get(key);
		if (value == null) {
			return Optional.empty();
		}
		if (!type.isInstance(value)) {
			return Optional.empty();
		}
		return Optional.of(type.cast(value));
	}

	@Override
	public Map<String, Object> snapshot(String runId) {
		Map<String, Object> current = byRun.get(runId);
		if (current == null) {
			return Map.of();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(current));
	}

	@Override
	public void deleteRun(String runId) {
		byRun.remove(runId);
	}
}
