package com.test.platform.api.registry.core;

import java.time.Duration;
import java.util.List;

public interface ApiSpecClient {
	ApiSpecDocument fetch(RegisteredApi api, Duration timeout);

	List<ApiSpecSnapshot> fetchSnapshots(List<RegisteredApi> apis, Duration timeout, int parallelism);
}
