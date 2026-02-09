package com.test.platform.api.registry.service;

import com.test.platform.api.registry.config.ApiRegistryProperties;
import com.test.platform.api.registry.core.ApiSpecClient;
import com.test.platform.api.registry.core.ApiSpecDocument;
import com.test.platform.api.registry.core.ApiSpecSnapshot;
import com.test.platform.api.registry.core.RegisteredApi;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ApiRegistryService {
	private final ApiRegistryProperties properties;
	private final ApiSpecClient apiSpecClient;

	public ApiRegistryService(ApiRegistryProperties properties, ApiSpecClient apiSpecClient) {
		this.properties = Objects.requireNonNull(properties, "properties");
		this.apiSpecClient = Objects.requireNonNull(apiSpecClient, "apiSpecClient");
	}

	public List<RegisteredApi> listApis() {
		return properties.getServices().stream().map(s -> new RegisteredApi(s.getName(), s.specUri()))
				.filter(a -> a.specUri() != null).toList();
	}

	public List<ApiSpecSnapshot> snapshot() {
		List<RegisteredApi> apis = listApis();
		Duration timeout = properties.getRequestTimeout();
		int parallelism = properties.getParallelism();
		return apiSpecClient.fetchSnapshots(apis, timeout, parallelism);
	}

	public ApiSpecDocument fetchSpec(String name) {
		RegisteredApi api = listApis().stream().filter(a -> a.name().equals(name)).findFirst().orElse(null);
		if (api == null) {
			return null;
		}
		return apiSpecClient.fetch(api, properties.getRequestTimeout());
	}
}
