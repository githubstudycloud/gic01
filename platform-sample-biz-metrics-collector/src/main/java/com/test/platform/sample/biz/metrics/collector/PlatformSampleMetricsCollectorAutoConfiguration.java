package com.test.platform.sample.biz.metrics.collector;

import com.test.platform.flow.spi.PlatformFlowStep;
import com.test.platform.flow.spi.PlatformFlowStepContext;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.sample.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformSampleMetricsCollectorAutoConfiguration {
	@Bean
	public PlatformFlowStep collectHostStep() {
		return new CollectHostStep();
	}

	@Bean
	public PlatformFlowStep collectJvmStep() {
		return new CollectJvmStep();
	}

	@Bean
	public PlatformFlowStep collectProcessStep() {
		return new CollectProcessStep();
	}

	private static final class CollectHostStep implements PlatformFlowStep {
		private static final Logger log = LoggerFactory.getLogger(CollectHostStep.class);

		@Override
		public String id() {
			return "collect.host";
		}

		@Override
		public Set<String> requiredStepIds() {
			return Set.of();
		}

		@Override
		public void execute(PlatformFlowStepContext context) {
			Map<String, Object> data = Map.of("ts", Instant.now().toString(), "hostname", hostname(), "osName",
					System.getProperty("os.name", ""), "osVersion", System.getProperty("os.version", ""), "osArch",
					System.getProperty("os.arch", ""), "javaVersion", System.getProperty("java.version", ""));
			context.artifacts().put("metrics.host", data);
			log.debug("metrics.host collected");
		}
	}

	private static final class CollectJvmStep implements PlatformFlowStep {
		private static final Logger log = LoggerFactory.getLogger(CollectJvmStep.class);

		@Override
		public String id() {
			return "collect.jvm";
		}

		@Override
		public Set<String> requiredStepIds() {
			return Set.of();
		}

		@Override
		public void execute(PlatformFlowStepContext context) {
			Runtime rt = Runtime.getRuntime();
			Map<String, Object> data = Map.of("ts", Instant.now().toString(), "availableProcessors",
					rt.availableProcessors(), "freeMemory", rt.freeMemory(), "totalMemory", rt.totalMemory(),
					"maxMemory", rt.maxMemory());
			context.artifacts().put("metrics.jvm", data);
			log.debug("metrics.jvm collected");
		}
	}

	private static final class CollectProcessStep implements PlatformFlowStep {
		private static final Logger log = LoggerFactory.getLogger(CollectProcessStep.class);

		@Override
		public String id() {
			return "collect.proc";
		}

		@Override
		public Set<String> requiredStepIds() {
			return Set.of("collect.host", "collect.jvm");
		}

		@Override
		public void execute(PlatformFlowStepContext context) {
			long pid = ProcessHandle.current().pid();
			Map<String, Object> data = Map.of("ts", Instant.now().toString(), "pid", pid, "user",
					System.getProperty("user.name", ""));
			context.artifacts().put("metrics.proc", data);
			log.debug("metrics.proc collected");
		}
	}

	private static String hostname() {
		String env = System.getenv("HOSTNAME");
		if (env == null || env.isBlank()) {
			env = System.getenv("COMPUTERNAME");
		}
		if (env != null && !env.isBlank()) {
			return env;
		}
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "unknown";
		}
	}
}
