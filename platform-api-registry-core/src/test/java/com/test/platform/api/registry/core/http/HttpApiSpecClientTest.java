package com.test.platform.api.registry.core.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.test.platform.api.registry.core.ApiSpecDocument;
import com.test.platform.api.registry.core.ApiSpecSnapshot;
import com.test.platform.api.registry.core.RegisteredApi;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpApiSpecClientTest {
	private HttpServer server;
	private HttpApiSpecClient client;
	private URI baseUri;

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/openapi.yaml", this::handleOpenApi);
		server.start();
		baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
		client = new HttpApiSpecClient();
	}

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void fetchDocument_success() {
		RegisteredApi api = new RegisteredApi("sample", baseUri.resolve("/openapi.yaml"));

		ApiSpecDocument doc = client.fetch(api, Duration.ofSeconds(2));

		assertEquals(200, doc.statusCode());
		assertNotNull(doc.body());
		assertTrue(new String(doc.body()).contains("openapi:"));
	}

	@Test
	void fetchSnapshots_parallel() {
		List<RegisteredApi> apis = List.of(new RegisteredApi("sample", baseUri.resolve("/openapi.yaml")),
				new RegisteredApi("missing", baseUri.resolve("/nope")));

		List<ApiSpecSnapshot> out = client.fetchSnapshots(apis, Duration.ofSeconds(2), 4);

		assertEquals(2, out.size());
		assertTrue(
				out.stream().anyMatch(s -> s.name().equals("sample") && s.statusCode() == 200 && s.sha256() != null));
		assertTrue(out.stream().anyMatch(s -> s.name().equals("missing") && s.statusCode() == 404));
	}

	private void handleOpenApi(HttpExchange exchange) throws IOException {
		byte[] body = ("openapi: 3.0.3\ninfo:\n  title: sample\n  version: 0\n").getBytes();
		exchange.getResponseHeaders().add("Content-Type", "application/yaml");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(body);
		}
	}
}
