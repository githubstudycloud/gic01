package com.test.platform.lock.adapter.local;

import com.test.platform.lock.spi.LockClient;
import com.test.platform.lock.spi.LockHandle;
import com.test.platform.lock.spi.LockRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class LocalLockClient implements LockClient {
  private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  @Override
  public Optional<LockHandle> tryLock(LockRequest request) {
    Instant deadline = Instant.now().plus(request.waitTime());
    long retryMillis = Math.max(1, request.retryInterval().toMillis());

    while (true) {
      String name = request.name();
      ReentrantLock lock = locks.computeIfAbsent(name, ignored -> new ReentrantLock());

      if (lock.isHeldByCurrentThread()) {
        return Optional.empty();
      }

      boolean acquired;
      try {
        acquired = lock.tryLock(0, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return Optional.empty();
      }

      if (acquired) {
        return Optional.of(new Handle(name, lock));
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
    private final ReentrantLock lock;
    private boolean closed;

    private Handle(String name, ReentrantLock lock) {
      this.name = name;
      this.lock = lock;
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
      lock.unlock();
      if (!lock.isLocked() && !lock.hasQueuedThreads()) {
        locks.remove(name, lock);
      }
    }
  }
}
