package com.test.platform.observability.hub.core;

import java.net.URI;
import java.util.Map;

public record ObservedService(String name, URI baseUri, Map<String, String> tags) {
}
