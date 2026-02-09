package com.test.platform.observability.hub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ObservabilityHubApplication.class, properties = {"platform.hub.services[0].name=sample",
		"platform.hub.services[0].base-url=http://localhost:8080"})
class ObservabilityHubApplicationTest {
	@Test
	void contextLoads() {
	}
}
