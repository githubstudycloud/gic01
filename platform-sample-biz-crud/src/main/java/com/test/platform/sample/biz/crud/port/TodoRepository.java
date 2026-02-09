package com.test.platform.sample.biz.crud.port;

import com.test.platform.sample.biz.crud.domain.TodoItem;
import java.util.List;
import java.util.Optional;

public interface TodoRepository {
	TodoItem save(TodoItem item);

	Optional<TodoItem> findById(String id);

	List<TodoItem> findAll();

	boolean deleteById(String id);
}
