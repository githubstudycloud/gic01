package com.test.platform.api.registry.core;

import java.net.URI;
import java.util.Objects;

public record RegisteredApi(String name, URI specUri) {
	public RegisteredApi {
		name = Objects.requireNonNull(name, "name");
		specUri = Objects.requireNonNull(specUri, "specUri");
	}
}
