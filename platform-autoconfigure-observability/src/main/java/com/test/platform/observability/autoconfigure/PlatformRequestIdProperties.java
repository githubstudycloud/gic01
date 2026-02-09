package com.test.platform.observability.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.observability.request-id")
public class PlatformRequestIdProperties {
	private boolean enabled = true;
	private String headerName = "X-Request-Id";
	private String mdcKey = "requestId";
	private boolean generateIfMissing = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHeaderName() {
		return headerName;
	}

	public void setHeaderName(String headerName) {
		this.headerName = headerName;
	}

	public String getMdcKey() {
		return mdcKey;
	}

	public void setMdcKey(String mdcKey) {
		this.mdcKey = mdcKey;
	}

	public boolean isGenerateIfMissing() {
		return generateIfMissing;
	}

	public void setGenerateIfMissing(boolean generateIfMissing) {
		this.generateIfMissing = generateIfMissing;
	}
}
