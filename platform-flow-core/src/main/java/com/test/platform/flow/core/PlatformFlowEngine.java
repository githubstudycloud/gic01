package com.test.platform.flow.core;

import com.test.platform.flow.spi.PlatformFlowDefinition;
import com.test.platform.flow.spi.PlatformFlowStep;
import com.test.platform.flow.spi.PlatformFlowStepArtifacts;
import com.test.platform.flow.spi.PlatformFlowStepContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Executes a flow as a DAG, honoring step dependencies.
 *
 * <p>
 * This engine is intentionally minimal: it supports modular composition and
 * fast testing, and delegates durability concerns to repository/adapters.
 */
public final class PlatformFlowEngine {
	private final PlatformFlowCatalog catalog;
	private final PlatformFlowRunRepository runRepository;
	private final PlatformFlowArtifactStore artifactStore;
	private final Executor executor;
	private final ConcurrentHashMap<String, CompletableFuture<Void>> completionByRunId = new ConcurrentHashMap<>();

	public PlatformFlowEngine(PlatformFlowCatalog catalog, PlatformFlowRunRepository runRepository,
			PlatformFlowArtifactStore artifactStore, Executor executor) {
		this.catalog = catalog;
		this.runRepository = runRepository;
		this.artifactStore = artifactStore;
		this.executor = executor;
	}

	public String start(String flowId, PlatformFlowRunRequest request) {
		PlatformFlowDefinition flow = catalog.getFlow(flowId);
		Set<String> targets = request.getTargetStepIds().isEmpty()
				? flow.defaultTargetStepIds()
				: request.getTargetStepIds();
		if (targets == null || targets.isEmpty()) {
			throw new IllegalArgumentException("No target steps provided and flow has no defaults: " + flowId);
		}
		for (String t : targets) {
			if (!flow.stepIds().contains(t)) {
				throw new IllegalArgumentException("Target step not in flow " + flowId + ": " + t);
			}
		}

		Set<String> planned = closure(flow, targets);
		String runId = newRunId();
		Instant now = Instant.now();

		PlatformFlowRun run = PlatformFlowRun.newRunning(runId, flowId, now, request.getInputs(), targets, planned);
		runRepository.createRun(run);

		Map<String, CompletableFuture<PlatformFlowStepStatus>> futures = new HashMap<>();
		for (String stepId : topologicalOrder(flow, planned)) {
			PlatformFlowStep step = catalog.getStep(stepId);
			Set<String> deps = step.requiredStepIds();
			Set<String> plannedDeps = new HashSet<>();
			if (deps != null) {
				for (String dep : deps) {
					if (planned.contains(dep)) {
						plannedDeps.add(dep);
					}
				}
			}

			CompletableFuture<PlatformFlowStepStatus> stepFuture;
			if (plannedDeps.isEmpty()) {
				stepFuture = CompletableFuture
						.supplyAsync(() -> executeStep(runId, flow.id(), step, request.getInputs()), executor);
			} else {
				CompletableFuture<?>[] depFutures = plannedDeps.stream().map(futures::get)
						.toArray(CompletableFuture[]::new);
				stepFuture = CompletableFuture.allOf(depFutures).thenApplyAsync(_ignored -> {
					boolean depsOk = plannedDeps.stream()
							.allMatch(d -> futures.get(d).join() == PlatformFlowStepStatus.SUCCEEDED);
					if (!depsOk) {
						markSkipped(runId, stepId, "Prerequisite failed");
						return PlatformFlowStepStatus.SKIPPED;
					}
					return executeStep(runId, flow.id(), step, request.getInputs());
				}, executor);
			}

			futures.put(stepId, stepFuture);
		}

		CompletableFuture<Void> done = CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
				.thenRunAsync(() -> finalizeRun(runId, flow, targets), executor);
		completionByRunId.put(runId, done);
		return runId;
	}

	public void await(String runId, Duration timeout) {
		CompletableFuture<Void> f = completionByRunId.get(runId);
		if (f == null) {
			throw new IllegalArgumentException("Unknown run: " + runId);
		}
		try {
			f.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Timed out waiting for run: " + runId, e);
		}
	}

	private void finalizeRun(String runId, PlatformFlowDefinition flow, Set<String> targets) {
		runRepository.updateRun(runId, current -> {
			boolean anyFailed = current.getSteps().values().stream()
					.anyMatch(s -> s.getStatus() == PlatformFlowStepStatus.FAILED);
			boolean targetsOk = targets.stream().allMatch(t -> current.getSteps().get(t) != null
					&& current.getSteps().get(t).getStatus() == PlatformFlowStepStatus.SUCCEEDED);
			PlatformFlowRunStatus status = (!anyFailed && targetsOk)
					? PlatformFlowRunStatus.SUCCEEDED
					: PlatformFlowRunStatus.FAILED;
			return current.completed(Instant.now(), status);
		});
	}

	private PlatformFlowStepStatus executeStep(String runId, String flowId, PlatformFlowStep step,
			Map<String, Object> inputs) {
		Instant now = Instant.now();
		runRepository.updateStepRun(runId, step.id(), r -> r.running(now));

		PlatformFlowStepArtifacts artifacts = new StepArtifacts(runId, artifactStore);
		PlatformFlowStepContext ctx = new DefaultStepContext(runId, flowId, inputs, artifacts);
		try {
			step.execute(ctx);
			runRepository.updateStepRun(runId, step.id(), r -> r.succeeded(Instant.now()));
			return PlatformFlowStepStatus.SUCCEEDED;
		} catch (Exception e) {
			runRepository.updateStepRun(runId, step.id(), r -> r.failed(Instant.now(), safeMessage(e)));
			return PlatformFlowStepStatus.FAILED;
		}
	}

	private void markSkipped(String runId, String stepId, String reason) {
		runRepository.updateStepRun(runId, stepId, r -> r.skipped(Instant.now(), reason));
	}

	private static String safeMessage(Exception e) {
		String msg = e.getMessage();
		if (msg == null || msg.isBlank()) {
			return e.getClass().getSimpleName();
		}
		return msg;
	}

	private static String newRunId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private List<String> topologicalOrder(PlatformFlowDefinition flow, Set<String> planned) {
		Map<String, Integer> inDegree = new HashMap<>();
		Map<String, Set<String>> dependents = new HashMap<>();

		for (String stepId : planned) {
			PlatformFlowStep step = catalog.getStep(stepId);
			Set<String> deps = step.requiredStepIds();
			int degree = 0;
			if (deps != null) {
				for (String dep : deps) {
					if (!planned.contains(dep)) {
						continue;
					}
					if (!flow.stepIds().contains(dep)) {
						throw new IllegalArgumentException(
								"Flow " + flow.id() + " step " + stepId + " requires non-member step: " + dep);
					}
					degree++;
					dependents.computeIfAbsent(dep, _ignored -> new HashSet<>()).add(stepId);
				}
			}
			inDegree.put(stepId, degree);
		}

		ArrayDeque<String> q = new ArrayDeque<>();
		planned.stream().sorted().forEach(stepId -> {
			if (inDegree.getOrDefault(stepId, 0) == 0) {
				q.addLast(stepId);
			}
		});

		List<String> order = new ArrayList<>(planned.size());
		while (!q.isEmpty()) {
			String stepId = q.removeFirst();
			order.add(stepId);
			for (String next : dependents.getOrDefault(stepId, Set.of())) {
				int updated = inDegree.compute(next, (_k, v) -> v == null ? 0 : (v - 1));
				if (updated == 0) {
					q.addLast(next);
				}
			}
		}

		if (order.size() != planned.size()) {
			throw new IllegalStateException("Cycle detected in flow " + flow.id());
		}
		return order;
	}

	private Set<String> closure(PlatformFlowDefinition flow, Set<String> targets) {
		Set<String> planned = new HashSet<>();
		ArrayDeque<String> q = new ArrayDeque<>(targets);
		while (!q.isEmpty()) {
			String stepId = q.removeFirst();
			if (!planned.add(stepId)) {
				continue;
			}
			PlatformFlowStep step = catalog.getStep(stepId);
			for (String dep : step.requiredStepIds()) {
				if (!flow.stepIds().contains(dep)) {
					throw new IllegalArgumentException(
							"Flow " + flow.id() + " step " + stepId + " requires non-member step: " + dep);
				}
				q.addLast(dep);
			}
		}
		return Set.copyOf(planned);
	}

	private static final class DefaultStepContext implements PlatformFlowStepContext {
		private final String runId;
		private final String flowId;
		private final Map<String, Object> inputs;
		private final PlatformFlowStepArtifacts artifacts;

		DefaultStepContext(String runId, String flowId, Map<String, Object> inputs,
				PlatformFlowStepArtifacts artifacts) {
			this.runId = runId;
			this.flowId = flowId;
			this.inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
			this.artifacts = artifacts;
		}

		@Override
		public String runId() {
			return runId;
		}

		@Override
		public String flowId() {
			return flowId;
		}

		@Override
		public Map<String, Object> inputs() {
			return inputs;
		}

		@Override
		public PlatformFlowStepArtifacts artifacts() {
			return artifacts;
		}
	}

	private static final class StepArtifacts implements PlatformFlowStepArtifacts {
		private final String runId;
		private final PlatformFlowArtifactStore store;

		StepArtifacts(String runId, PlatformFlowArtifactStore store) {
			this.runId = runId;
			this.store = store;
		}

		@Override
		public void put(String key, Object value) {
			store.put(runId, key, value);
		}

		@Override
		public <T> java.util.Optional<T> get(String key, Class<T> type) {
			return store.get(runId, key, type);
		}

		@Override
		public Map<String, Object> snapshot() {
			return store.snapshot(runId);
		}
	}
}
