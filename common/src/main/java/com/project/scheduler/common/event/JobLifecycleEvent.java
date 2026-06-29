package com.project.scheduler.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record JobLifecycleEvent(UUID eventId, JobEventType type, UUID jobId, String jobName,
		Map<String, String> metadata, Instant occurredAt) {

	public static JobLifecycleEvent of(JobEventType eventType, UUID jobId, String jobName) {
		return new JobLifecycleEvent(UUID.randomUUID(), eventType, jobId, jobName, Map.of(), Instant.now());
	}

	public static JobLifecycleEvent of(JobEventType eventType, UUID jobId, String jobName,
			Map<String, String> metadata) {
		return new JobLifecycleEvent(UUID.randomUUID(), eventType, jobId, jobName, Map.copyOf(metadata), Instant.now());
	}
}
