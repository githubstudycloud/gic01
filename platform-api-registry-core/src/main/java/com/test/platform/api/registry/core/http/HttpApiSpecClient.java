package com.test.platform.api.registry.core.http;

import com.test.platform.api.registry.core.ApiSpecClient;
import com.test.platform.api.registry.core.ApiSpecDocument;
import com.test.platform.api.registry.core.ApiSpecSnapshot;
import com.test.platform.api.registry.core.RegisteredApi;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HttpApiSpecClient implements ApiSpecClient {
	private final HttpClient httpClient;

	public HttpApiSpecClient() {
		this(HttpClient.newHttpClient());
	}

	public HttpApiSpecClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public ApiSpecDocument fetch(RegisteredApi api, Duration timeout) {
		Instant fetchedAt = Instant.now();
		try {
			URI uri = api.specUri();
			HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
					.header("Accept", "application/yaml, application/json, */*").GET().build();
			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

			String contentType = response.headers().firstValue("Content-Type").orElse(null);
			int status = response.statusCode();
			byte[] body = response.body();
			if (status >= 200 && status < 300 && body != null && body.length > 0) {
				return new ApiSpecDocument(api.name(), uri, fetchedAt, status, contentType, body, null);
			}
			String error = "HTTP " + status;
			return new ApiSpecDocument(api.name(), uri, fetchedAt, status, contentType, body, error);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return new ApiSpecDocument(api.name(), api.specUri(), fetchedAt, 0, null, null,
					e.getClass().getSimpleName() + ": " + e.getMessage());
		} catch (RuntimeException e) {
			return new ApiSpecDocument(api.name(), api.specUri(), fetchedAt, 0, null, null,
					e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	@Override
	public List<ApiSpecSnapshot> fetchSnapshots(List<RegisteredApi> apis, Duration timeout, int parallelism) {
		if (apis == null || apis.isEmpty()) {
			return List.of();
		}
		int threads = Math.max(1, parallelism);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			List<CompletableFuture<ApiSpecSnapshot>> futures = new ArrayList<>(apis.size());
			for (RegisteredApi api : apis) {
				futures.add(CompletableFuture.supplyAsync(() -> fetch(api, timeout).toSnapshot(), executor));
			}
			return futures.stream().map(CompletableFuture::join).toList();
		} finally {
			executor.shutdown();
		}
	}
}
