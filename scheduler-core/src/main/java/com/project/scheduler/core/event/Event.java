package com.project.scheduler.core.event;

import java.time.Instant;
import java.util.UUID;

public interface Event {

	UUID eventId();

	Instant occurredAt();

	String type();
}
