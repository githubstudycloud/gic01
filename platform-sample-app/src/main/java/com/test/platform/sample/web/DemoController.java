package com.test.platform.sample.web;

import com.test.platform.lock.spi.LockClient;
import com.test.platform.lock.spi.LockHandle;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
	private final LockClient lockClient;

	public DemoController(LockClient lockClient) {
		this.lockClient = lockClient;
	}

	@GetMapping("/demo/ping")
	public Map<String, Object> ping() {
		return Map.of("ok", true);
	}

	@GetMapping("/demo/lock")
	public Map<String, Object> lock(@RequestParam String name, @RequestParam(defaultValue = "5") int ttlSeconds) {
		Optional<LockHandle> handle = lockClient.tryLock(name, Duration.ofSeconds(ttlSeconds));
		if (handle.isEmpty()) {
			return Map.of("name", name, "acquired", false);
		}
		LockHandle lockHandle = handle.get();
		try (lockHandle) {
			return Map.of("name", name, "acquired", true);
		}
	}
}
