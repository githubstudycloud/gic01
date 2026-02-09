package com.test.platform.lock.autoconfigure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.lock")
public class PlatformLockProperties {
	private Provider provider = Provider.local;

	private Duration defaultTtl = Duration.ofSeconds(30);
	private Duration defaultWaitTime = Duration.ZERO;
	private Duration defaultRetryInterval = Duration.ofMillis(50);

	private Redis redis = new Redis();

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public Duration getDefaultTtl() {
		return defaultTtl;
	}

	public void setDefaultTtl(Duration defaultTtl) {
		this.defaultTtl = defaultTtl;
	}

	public Duration getDefaultWaitTime() {
		return defaultWaitTime;
	}

	public void setDefaultWaitTime(Duration defaultWaitTime) {
		this.defaultWaitTime = defaultWaitTime;
	}

	public Duration getDefaultRetryInterval() {
		return defaultRetryInterval;
	}

	public void setDefaultRetryInterval(Duration defaultRetryInterval) {
		this.defaultRetryInterval = defaultRetryInterval;
	}

	public Redis getRedis() {
		return redis;
	}

	public void setRedis(Redis redis) {
		this.redis = redis;
	}

	public enum Provider {
		local, redis
	}

	public static final class Redis {
		private String keyPrefix = "platform:lock:";

		public String getKeyPrefix() {
			return keyPrefix;
		}

		public void setKeyPrefix(String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}
	}
}
