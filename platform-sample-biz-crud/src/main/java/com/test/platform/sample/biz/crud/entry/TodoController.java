package com.test.platform.sample.biz.crud.entry;

import com.test.platform.sample.biz.crud.domain.TodoItem;
import com.test.platform.sample.biz.crud.service.TodoService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/crud/todos")
public class TodoController {
	private final TodoService service;

	public TodoController(TodoService service) {
		this.service = service;
	}

	@GetMapping
	public List<TodoItem> list() {
		return service.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TodoItem create(@RequestBody CreateTodoRequest req) {
		if (req == null || req.title() == null || req.title().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
		}
		return service.create(req.title());
	}

	@GetMapping("/{id}")
	public TodoItem get(@PathVariable String id) {
		return service.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "todo not found"));
	}

	@PutMapping("/{id}")
	public TodoItem update(@PathVariable String id, @RequestBody UpdateTodoRequest req) {
		if (req == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
		}
		return service.update(id, req.title(), req.done())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "todo not found"));
	}

	@DeleteMapping("/{id}")
	public Map<String, Object> delete(@PathVariable String id) {
		boolean deleted = service.delete(id);
		return Map.of("id", id, "deleted", deleted);
	}

	public record CreateTodoRequest(String title) {
	}

	public record UpdateTodoRequest(String title, Boolean done) {
	}
}
