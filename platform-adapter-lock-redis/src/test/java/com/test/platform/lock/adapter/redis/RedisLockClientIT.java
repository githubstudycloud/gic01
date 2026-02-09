package com.test.platform.lock.adapter.redis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.test.platform.lock.spi.LockHandle;
import com.test.platform.lock.spi.LockRequest;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

class RedisLockClientIT {
	@Test
	void providesMutualExclusionWithUnlock() {
		Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is required for this IT");

		GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
		redisContainer.start();
		try {
			LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisContainer.getHost(),
					redisContainer.getFirstMappedPort());
			connectionFactory.afterPropertiesSet();
			try {
				StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
				template.afterPropertiesSet();

				RedisLockClient client = new RedisLockClient(template, "platform:lock:");
				Optional<LockHandle> first = client.tryLock(LockRequest.once("x", Duration.ofSeconds(5)));
				assertTrue(first.isPresent());

				Optional<LockHandle> second = client.tryLock(LockRequest.once("x", Duration.ofSeconds(5)));
				assertFalse(second.isPresent());

				first.get().close();

				Optional<LockHandle> third = client.tryLock(LockRequest.once("x", Duration.ofSeconds(5)));
				assertTrue(third.isPresent());
				third.get().close();
			} finally {
				connectionFactory.destroy();
			}
		} finally {
			redisContainer.stop();
		}
	}
}
