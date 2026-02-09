package com.test.platform.sample.biz.crud.adapter;

import com.test.platform.sample.biz.crud.domain.TodoItem;
import com.test.platform.sample.biz.crud.port.TodoRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTodoRepository implements TodoRepository {
	private final ConcurrentHashMap<String, TodoItem> byId = new ConcurrentHashMap<>();

	@Override
	public TodoItem save(TodoItem item) {
		byId.put(item.id(), item);
		return item;
	}

	@Override
	public Optional<TodoItem> findById(String id) {
		return Optional.ofNullable(byId.get(id));
	}

	@Override
	public List<TodoItem> findAll() {
		ArrayList<TodoItem> list = new ArrayList<>(byId.values());
		list.sort(Comparator.comparing(TodoItem::createdAt).reversed());
		return List.copyOf(list);
	}

	@Override
	public boolean deleteById(String id) {
		return byId.remove(id) != null;
	}
}
