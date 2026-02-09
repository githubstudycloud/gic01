package com.test.platform.kernel.error;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class PlatformException extends RuntimeException {
	private final String code;
	private final Map<String, Object> details;

	public PlatformException(ErrorCode errorCode) {
		this(errorCode, Collections.emptyMap(), null);
	}

	public PlatformException(ErrorCode errorCode, Map<String, Object> details) {
		this(errorCode, details, null);
	}

	public PlatformException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
		super(Objects.requireNonNull(errorCode, "errorCode").message(), cause);
		this.code = errorCode.code();
		this.details = details == null ? Collections.emptyMap() : Collections.unmodifiableMap(details);
	}

	public String getCode() {
		return code;
	}

	public Map<String, Object> getDetails() {
		return details;
	}
}
