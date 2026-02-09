package com.test.platform.sample.web;

import com.test.platform.lock.spi.LockClient;
import com.test.platform.lock.spi.LockHandle;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
	public Map<String, Object> lock(@RequestParam String name, @RequestParam(defaultValue = "5") int ttlSeconds,
			@RequestParam(required = false) Long holdMillis) {
		Duration ttl = Duration.of(ttlSeconds, ChronoUnit.SECONDS);
		Optional<LockHandle> handle = lockClient.tryLock(name, ttl);
		if (handle.isEmpty()) {
			return Map.of("name", name, "acquired", false);
		}
		LockHandle lockHandle = handle.get();
		try (lockHandle) {
			sleepQuietly(cappedHoldMillis(holdMillis, ttl));
			return Map.of("name", name, "acquired", true);
		}
	}

	private static long cappedHoldMillis(Long holdMillis, Duration ttl) {
		if (holdMillis == null || holdMillis <= 0) {
			return 0;
		}
		long max = Math.min(ttl.toMillis(), 10_000L);
		return Math.min(holdMillis, max);
	}

	private static void sleepQuietly(long millis) {
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
