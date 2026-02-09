package com.test.platform.api.registry.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.api-registry")
public class ApiRegistryProperties {
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
		private URI baseUri;
		private String specPath = "/openapi.yaml";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public URI getBaseUri() {
			return baseUri;
		}

		public void setBaseUri(URI baseUri) {
			this.baseUri = baseUri;
		}

		public String getSpecPath() {
			return specPath;
		}

		public void setSpecPath(String specPath) {
			this.specPath = specPath;
		}

		public URI specUri() {
			if (baseUri == null) {
				return null;
			}
			return baseUri.resolve(specPath == null ? "/openapi.yaml" : specPath);
		}
	}
}
