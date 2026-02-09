package com.test.platform.lock.spi;

public interface LockHandle extends AutoCloseable {
	String name();

	@Override
	void close();
}
