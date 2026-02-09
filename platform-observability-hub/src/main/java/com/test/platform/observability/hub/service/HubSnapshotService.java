package com.test.platform.observability.hub.service;

import com.test.platform.observability.hub.config.HubProperties;
import com.test.platform.observability.hub.core.ObservedService;
import com.test.platform.observability.hub.core.ServiceSnapshot;
import com.test.platform.observability.hub.core.ServiceSnapshotFetcher;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HubSnapshotService {
	private final HubProperties properties;
	private final ServiceSnapshotFetcher fetcher;

	public HubSnapshotService(HubProperties properties, ServiceSnapshotFetcher fetcher) {
		this.properties = properties;
		this.fetcher = fetcher;
	}

	public List<ObservedService> services() {
		return properties.getServices().stream()
				.map(s -> new ObservedService(s.getName(), URI.create(s.getBaseUrl()), s.getTags())).toList();
	}

	public List<ServiceSnapshot> snapshot() {
		return fetcher.fetch(services(), properties.getRequestTimeout(), properties.getParallelism());
	}
}
