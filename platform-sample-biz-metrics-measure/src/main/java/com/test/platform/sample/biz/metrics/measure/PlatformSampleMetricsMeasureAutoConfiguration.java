package com.test.platform.sample.biz.metrics.measure;

import com.test.platform.flow.spi.PlatformFlowStep;
import com.test.platform.flow.spi.PlatformFlowStepContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.sample.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformSampleMetricsMeasureAutoConfiguration {
	@Bean
	public PlatformFlowStep measureJvmStep() {
		return new MeasureJvmStep();
	}

	@Bean
	public PlatformFlowStep measureHostStep() {
		return new MeasureHostStep();
	}

	@Bean
	public PlatformFlowStep measureSummaryStep() {
		return new MeasureSummaryStep();
	}

	private static final class MeasureJvmStep implements PlatformFlowStep {
		private static final Logger log = LoggerFactory.getLogger(MeasureJvmStep.class);

		@Override
		public String id() {
			return "measure.jvm";
		}

		@Override
		public Set<String> requiredStepIds() {
			return Set.of("collect.jvm");
		}

		@Override
		public void execute(PlatformFlowStepContext context) {
			Map<String, Object> jvm = mapArtifact(context, "metrics.jvm");
			long free = longValue(jvm, "freeMemory");
			long total = longValue(jvm, "totalMemory");
			long max = longValue(jvm, "maxMemory");
			long used = Math.max(0L, total - free);
			double utilization = (max <= 0L) ? 0.0d : (used * 1.0d / max);

			Map<String, Object> metric = Map.of("ts", Instant.now().toString(), "usedMemory", used, "maxMemory", max,
					"heapUtilization", utilization);
			context.artifacts().put("metric.jvm", metric);
			log.debug("metric.jvm calculated");
		}
	}

	private static final class MeasureHostStep implements PlatformFlowStep {
		private static final Logger log = LoggerFactory.getLogger(MeasureHostStep.class);

		@Override
		public String id() {
			return "measure.host";
		}

		@Override
		public Set<String> requiredStepIds() {
			return Set.of("collect.host", "collect.proc");
		}

		@Override
		public void execute(PlatformFlowStepContext context) {
			Map<String, Object> host = mapArtifact(context, "metrics.host");
			Map<String, Object> proc = mapArtifact(context, "metrics.proc");

			Map<String, Object> metric = new HashMap<>();
			metric.put("ts", Instant.now().toString());
			metric.put("hostname", host.getOrDefault("hostname", "unknown"));
			metric.put("pid", proc.getOrDefault("pid", -1));
			metric.put("cpuCores", Runtime.getRuntime().availableProcessors());
			context.artifacts().put("metric.host", Map.copyOf(metric));
			log.debug("metric.host calculated");
		}
	}

	private static final class MeasureSummaryStep implements PlatformFlowStep {
		private static final Logger log = LoggerFactory.getLogger(MeasureSummaryStep.class);

		@Override
		public String id() {
			return "measure.summary";
		}

		@Override
		public Set<String> requiredStepIds() {
			return Set.of("measure.jvm", "measure.host");
		}

		@Override
		public void execute(PlatformFlowStepContext context) {
			Map<String, Object> jvmMetric = mapArtifact(context, "metric.jvm");
			Map<String, Object> hostMetric = mapArtifact(context, "metric.host");

			double heapUtil = doubleValue(jvmMetric, "heapUtilization");
			int score = heapUtil >= 0.85d ? 60 : heapUtil >= 0.75d ? 80 : 95;

			Map<String, Object> summary = new HashMap<>();
			summary.put("ts", Instant.now().toString());
			summary.put("score", score);
			summary.put("heapUtilization", heapUtil);
			summary.put("hostname", hostMetric.getOrDefault("hostname", "unknown"));
			summary.put("pid", hostMetric.getOrDefault("pid", -1));

			context.artifacts().put("metric.summary", Map.copyOf(summary));
			log.debug("metric.summary calculated");
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> mapArtifact(PlatformFlowStepContext context, String key) {
		return (Map<String, Object>) context.artifacts().get(key, Map.class).orElse(Map.of());
	}

	private static long longValue(Map<String, Object> map, String key) {
		Object v = map.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static double doubleValue(Map<String, Object> map, String key) {
		Object v = map.get(key);
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		return 0.0d;
	}
}
