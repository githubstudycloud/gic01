package com.test.platform.api.registry.core;

import java.net.URI;
import java.time.Instant;

public record ApiSpecSnapshot(String name, URI specUri, Instant fetchedAt, int statusCode, String contentType,
		int contentLength, String sha256, String error) {
}
