package com.test.platform.api.registry.core;

import java.net.URI;
import java.time.Instant;

public record ApiSpecDocument(String name, URI specUri, Instant fetchedAt, int statusCode, String contentType,
		byte[] body, String error) {

	public ApiSpecSnapshot toSnapshot() {
		String sha256 = body == null ? null : Sha256.hex(body);
		int len = body == null ? 0 : body.length;
		return new ApiSpecSnapshot(name, specUri, fetchedAt, statusCode, contentType, len, sha256, error);
	}
}
