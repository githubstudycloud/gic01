package com.test.platform.observability.autoconfigure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public final class PlatformRequestIdFilter extends OncePerRequestFilter {
	private final PlatformRequestIdProperties properties;

	public PlatformRequestIdFilter(PlatformRequestIdProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = request.getHeader(properties.getHeaderName());
		if ((requestId == null || requestId.isBlank()) && properties.isGenerateIfMissing()) {
			requestId = UUID.randomUUID().toString().replace("-", "");
		}

		if (requestId == null || requestId.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		MDC.put(properties.getMdcKey(), requestId);
		response.setHeader(properties.getHeaderName(), requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(properties.getMdcKey());
		}
	}
}
