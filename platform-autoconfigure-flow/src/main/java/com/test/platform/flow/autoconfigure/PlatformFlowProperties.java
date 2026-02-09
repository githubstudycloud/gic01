package com.test.platform.flow.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.flow")
public class PlatformFlowProperties {
	/**
	 * Master switch for flow execution and endpoints.
	 */
	private boolean enabled = true;

	/**
	 * Executor concurrency for DAG steps.
	 */
	private int maxConcurrency = 8;

	/**
	 * Expose HTTP endpoints under /flows.
	 */
	private boolean webEnabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public boolean isWebEnabled() {
		return webEnabled;
	}

	public void setWebEnabled(boolean webEnabled) {
		this.webEnabled = webEnabled;
	}
}
