package com.test.platform.sample.biz.crud.service;

import com.test.platform.sample.biz.crud.domain.TodoItem;
import com.test.platform.sample.biz.crud.port.TodoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TodoService {
	private final TodoRepository repo;

	public TodoService(TodoRepository repo) {
		this.repo = repo;
	}

	public TodoItem create(String title) {
		String id = UUID.randomUUID().toString().replace("-", "");
		Instant now = Instant.now();
		TodoItem item = new TodoItem(id, normalize(title), false, now, now);
		return repo.save(item);
	}

	public Optional<TodoItem> get(String id) {
		return repo.findById(id);
	}

	public List<TodoItem> list() {
		return repo.findAll();
	}

	public Optional<TodoItem> update(String id, String title, Boolean done) {
		return repo.findById(id).map(current -> {
			String nextTitle = title == null ? current.title() : normalize(title);
			boolean nextDone = done == null ? current.done() : done;
			TodoItem next = new TodoItem(current.id(), nextTitle, nextDone, current.createdAt(), Instant.now());
			return repo.save(next);
		});
	}

	public boolean delete(String id) {
		return repo.deleteById(id);
	}

	private static String normalize(String title) {
		if (title == null) {
			return "";
		}
		return title.trim();
	}
}
