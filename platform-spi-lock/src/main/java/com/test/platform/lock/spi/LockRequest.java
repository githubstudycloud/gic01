package com.test.platform.lock.spi;

import java.time.Duration;
import java.util.Objects;

public record LockRequest(String name, Duration ttl, Duration waitTime, Duration retryInterval) {
  public LockRequest {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(ttl, "ttl");
    Objects.requireNonNull(waitTime, "waitTime");
    Objects.requireNonNull(retryInterval, "retryInterval");
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("ttl must be positive");
    }
    if (waitTime.isNegative()) {
      throw new IllegalArgumentException("waitTime must be >= 0");
    }
    if (retryInterval.isZero() || retryInterval.isNegative()) {
      throw new IllegalArgumentException("retryInterval must be positive");
    }
  }

  public static LockRequest once(String name, Duration ttl) {
    return new LockRequest(name, ttl, Duration.ZERO, Duration.ofMillis(50));
  }

  public static LockRequest retry(
      String name, Duration ttl, Duration waitTime, Duration retryInterval) {
    return new LockRequest(name, ttl, waitTime, retryInterval);
  }
}

