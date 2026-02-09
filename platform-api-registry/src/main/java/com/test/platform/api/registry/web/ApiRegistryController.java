package com.test.platform.api.registry.web;

import com.test.platform.api.registry.core.ApiSpecDocument;
import com.test.platform.api.registry.core.ApiSpecSnapshot;
import com.test.platform.api.registry.core.RegisteredApi;
import com.test.platform.api.registry.service.ApiRegistryService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/registry")
public class ApiRegistryController {
	private final ApiRegistryService apiRegistryService;

	public ApiRegistryController(ApiRegistryService apiRegistryService) {
		this.apiRegistryService = apiRegistryService;
	}

	@GetMapping("/apis")
	public List<RegisteredApi> apis() {
		return apiRegistryService.listApis();
	}

	@GetMapping("/snapshot")
	public List<ApiSpecSnapshot> snapshot() {
		return apiRegistryService.snapshot();
	}

	@GetMapping("/spec/{name}")
	public ResponseEntity<byte[]> spec(@PathVariable String name) {
		ApiSpecDocument doc = apiRegistryService.fetchSpec(name);
		if (doc == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
		int status = doc.statusCode();
		byte[] body = doc.body();
		if (status == 0) {
			status = 502;
		}

		HttpHeaders headers = new HttpHeaders();
		String contentType = doc.contentType();
		if (StringUtils.hasText(contentType)) {
			headers.set(HttpHeaders.CONTENT_TYPE, contentType);
		} else {
			headers.setContentType(MediaType.valueOf("application/yaml"));
		}
		if (body == null || body.length == 0) {
			String msg = doc.error() == null ? "" : doc.error();
			body = msg.getBytes(StandardCharsets.UTF_8);
		}
		return new ResponseEntity<>(body, headers, HttpStatus.valueOf(status));
	}
}
