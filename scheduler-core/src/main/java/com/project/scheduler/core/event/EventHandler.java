package com.project.scheduler.core.event;

@FunctionalInterface
public interface EventHandler<T extends Event> {
	void handle(T event);
}
