package com.test.platform.lock.adapter.local;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.test.platform.lock.spi.LockHandle;
import com.test.platform.lock.spi.LockRequest;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LocalLockClientTest {
  @Test
  void allowsExclusiveLocking() {
    LocalLockClient client = new LocalLockClient();
    Optional<LockHandle> first = client.tryLock(LockRequest.once("a", Duration.ofSeconds(5)));
    assertTrue(first.isPresent());

    Optional<LockHandle> second = client.tryLock(LockRequest.once("a", Duration.ofSeconds(5)));
    assertFalse(second.isPresent());

    first.get().close();
    Optional<LockHandle> third = client.tryLock(LockRequest.once("a", Duration.ofSeconds(5)));
    assertTrue(third.isPresent());
    third.get().close();
  }
}

