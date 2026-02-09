package com.test.platform.sample.biz.crud.domain;

import java.time.Instant;

public record TodoItem(String id, String title, boolean done, Instant createdAt, Instant updatedAt) {
}
