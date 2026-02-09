package com.test.platform.observability.hub.core;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;

public record ServiceSnapshot(String name, URI baseUri, Instant fetchedAt, int healthStatusCode, JsonNode health,
		int infoStatusCode, JsonNode info, String error) {
}
