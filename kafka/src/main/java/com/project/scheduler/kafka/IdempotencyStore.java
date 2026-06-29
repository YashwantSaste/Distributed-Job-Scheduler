package com.project.scheduler.kafka;

import java.time.Duration;
import java.util.UUID;

public interface IdempotencyStore {
	boolean markIfNew(UUID eventId, Duration ttl);
}
