package com.test.platform.sample.biz.crud;

import com.test.platform.sample.biz.crud.entry.TodoController;
import com.test.platform.sample.biz.crud.port.TodoRepository;
import com.test.platform.sample.biz.crud.service.TodoService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "platform.sample.crud", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformSampleCrudAutoConfiguration {
	@Bean
	public TodoRepository todoRepository() {
		return new com.test.platform.sample.biz.crud.adapter.InMemoryTodoRepository();
	}

	@Bean
	public TodoService todoService(TodoRepository repo) {
		return new TodoService(repo);
	}

	@Bean
	public TodoController todoController(TodoService service) {
		return new TodoController(service);
	}
}
