package com.test.platform.observability.hub.core;

import java.time.Duration;
import java.util.List;

public interface ServiceSnapshotFetcher {
	List<ServiceSnapshot> fetch(List<ObservedService> services, Duration timeout, int parallelism);
}
