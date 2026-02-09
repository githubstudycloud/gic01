package com.test.platform.lock.spi;

import java.time.Duration;
import java.util.Optional;

public interface LockClient {
	Optional<LockHandle> tryLock(LockRequest request);

	default Optional<LockHandle> tryLock(String name, Duration ttl) {
		return tryLock(LockRequest.once(name, ttl));
	}

	default Optional<LockHandle> tryLock(String name, Duration ttl, Duration waitTime, Duration retryInterval) {
		return tryLock(LockRequest.retry(name, ttl, waitTime, retryInterval));
	}
}
