package com.test.platform.api.registry.wiring;

import com.test.platform.api.registry.core.ApiSpecClient;
import com.test.platform.api.registry.core.http.HttpApiSpecClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiRegistryWiring {

	@Bean
	public ApiSpecClient apiSpecClient() {
		return new HttpApiSpecClient();
	}
}
