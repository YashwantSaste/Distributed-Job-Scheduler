package com.project.scheduler.kafka;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIdempotencyStore implements IdempotencyStore {

	private final Map<UUID, Instant> processedEvents = new ConcurrentHashMap<>();

	public boolean markIfNew(UUID id, Duration ttl) {
		Instant now = Instant.now();
		processedEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
		return processedEvents.putIfAbsent(id, now.plus(ttl)) == null;
	}
}
