package com.project.scheduler.core.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public final class InMemoryEventBus {

	private final Map<Class<?>, List<EventHandler<? extends Event>>> handlers = new ConcurrentHashMap<>();
	private final Executor executor;

	public InMemoryEventBus(Executor executor) {
		this.executor = executor;
	}

	public <T extends Event> void subscribe(Class<T> eventType, EventHandler<T> eventHandler) {
		handlers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(eventHandler);
	}

	public void publish(Event event) {
		for (EventHandler<? extends Event> eventHandler : handlers.getOrDefault(event.getClass(), List.of())) {
			CompletableFuture.runAsync(() -> invoke(eventHandler, event), executor);
		}
	}

	@SuppressWarnings("unchecked")
	static <T extends Event> void invoke(EventHandler<? extends Event> eventHandler, Event event) {
		((EventHandler<T>) eventHandler).handle((T) event);
	}
}
