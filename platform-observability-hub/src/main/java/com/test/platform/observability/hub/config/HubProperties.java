package com.test.platform.observability.hub.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.hub")
public class HubProperties {
	private Duration requestTimeout = Duration.ofSeconds(2);
	private int parallelism = 8;
	private List<Service> services = new ArrayList<>();

	public Duration getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(Duration requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public int getParallelism() {
		return parallelism;
	}

	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}

	public List<Service> getServices() {
		return services;
	}

	public void setServices(List<Service> services) {
		this.services = services;
	}

	public static final class Service {
		private String name;
		private String baseUrl;
		private Map<String, String> tags = new HashMap<>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public Map<String, String> getTags() {
			return tags;
		}

		public void setTags(Map<String, String> tags) {
			this.tags = tags;
		}
	}
}
