package com.test.platform.observability.hub.core.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.platform.observability.hub.core.ObservedService;
import com.test.platform.observability.hub.core.ServiceSnapshot;
import com.test.platform.observability.hub.core.ServiceSnapshotFetcher;
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

public final class HttpActuatorSnapshotFetcher implements ServiceSnapshotFetcher {
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public HttpActuatorSnapshotFetcher(ObjectMapper objectMapper) {
    this(objectMapper, HttpClient.newHttpClient());
  }

  public HttpActuatorSnapshotFetcher(ObjectMapper objectMapper, HttpClient httpClient) {
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  @Override
  public List<ServiceSnapshot> fetch(List<ObservedService> services, Duration timeout, int parallelism) {
    if (services == null || services.isEmpty()) {
      return List.of();
    }
    int threads = Math.max(1, parallelism);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      List<CompletableFuture<ServiceSnapshot>> futures = new ArrayList<>(services.size());
      for (ObservedService service : services) {
        futures.add(CompletableFuture.supplyAsync(() -> fetchOne(service, timeout), executor));
      }
      return futures.stream().map(CompletableFuture::join).toList();
    } finally {
      executor.shutdown();
    }
  }

  private ServiceSnapshot fetchOne(ObservedService service, Duration timeout) {
    Instant fetchedAt = Instant.now();
    String error = null;

    EndpointResult health =
        fetchJsonFirstOk(
            service.baseUri(),
            List.of("/actuator/health/readiness", "/actuator/health"),
            timeout);

    EndpointResult info = fetchJson(service.baseUri().resolve("/actuator/info"), timeout);
    if (info.error != null && error == null) {
      error = info.error;
    }
    if (health.error != null && error == null) {
      error = health.error;
    }

    return new ServiceSnapshot(
        service.name(),
        service.baseUri(),
        fetchedAt,
        health.statusCode,
        health.body,
        info.statusCode,
        info.body,
        error);
  }

  private EndpointResult fetchJsonFirstOk(URI baseUri, List<String> paths, Duration timeout) {
    EndpointResult last = new EndpointResult(0, null, "no endpoints attempted");
    for (String path : paths) {
      URI uri = baseUri.resolve(path);
      last = fetchJson(uri, timeout);
      if (last.statusCode >= 200 && last.statusCode < 300 && last.body != null) {
        return last;
      }
    }
    return last;
  }

  private EndpointResult fetchJson(URI uri, Duration timeout) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(uri)
              .timeout(timeout)
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      JsonNode body = null;
      if (status >= 200 && status < 300 && response.body() != null && !response.body().isBlank()) {
        body = objectMapper.readTree(response.body());
      }
      return new EndpointResult(status, body, null);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return new EndpointResult(0, null, e.getClass().getSimpleName() + ": " + e.getMessage());
    } catch (RuntimeException e) {
      return new EndpointResult(0, null, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  private record EndpointResult(int statusCode, JsonNode body, String error) {}
}
