package com.test.platform.lock.adapter.redis;

import com.test.platform.lock.spi.LockClient;
import com.test.platform.lock.spi.LockHandle;
import com.test.platform.lock.spi.LockRequest;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisLockClient implements LockClient {
  private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
      new DefaultRedisScript<>(
          """
          if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
          else
            return 0
          end
          """,
          Long.class);

  private final StringRedisTemplate redis;
  private final String keyPrefix;

  public RedisLockClient(StringRedisTemplate redis, String keyPrefix) {
    this.redis = redis;
    this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
  }

  @Override
  public Optional<LockHandle> tryLock(LockRequest request) {
    Instant deadline = Instant.now().plus(request.waitTime());
    long retryMillis = Math.max(1, request.retryInterval().toMillis());

    String key = keyPrefix + request.name();
    String token = UUID.randomUUID().toString().replace("-", "");

    while (true) {
      Boolean acquired =
          redis.opsForValue().setIfAbsent(key, token, request.ttl());
      if (Boolean.TRUE.equals(acquired)) {
        return Optional.of(new Handle(request.name(), key, token));
      }

      if (!Instant.now().isBefore(deadline)) {
        return Optional.empty();
      }

      try {
        Thread.sleep(retryMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return Optional.empty();
      }
    }
  }

  private final class Handle implements LockHandle {
    private final String name;
    private final String key;
    private final String token;
    private boolean closed;

    private Handle(String name, String key, String token) {
      this.name = name;
      this.key = key;
      this.token = token;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      redis.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
    }
  }
}

