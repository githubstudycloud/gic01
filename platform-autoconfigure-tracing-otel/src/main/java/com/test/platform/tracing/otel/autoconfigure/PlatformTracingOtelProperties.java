package com.test.platform.tracing.otel.autoconfigure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.tracing.otel")
public class PlatformTracingOtelProperties {
	private boolean enabled = true;
	private String serviceName;
	private String instrumentationName = "platform";
	private final Export export = new Export();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getInstrumentationName() {
		return instrumentationName;
	}

	public void setInstrumentationName(String instrumentationName) {
		this.instrumentationName = instrumentationName;
	}

	public Export getExport() {
		return export;
	}

	public static final class Export {
		private boolean enabled = false;
		private String otlpEndpoint;
		private Duration timeout = Duration.ofSeconds(3);

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getOtlpEndpoint() {
			return otlpEndpoint;
		}

		public void setOtlpEndpoint(String otlpEndpoint) {
			this.otlpEndpoint = otlpEndpoint;
		}

		public Duration getTimeout() {
			return timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}
	}
}
