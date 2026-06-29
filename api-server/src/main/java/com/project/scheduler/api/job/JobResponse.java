package com.project.scheduler.api.job;

import java.time.Instant;
import java.util.UUID;

import com.project.scheduler.common.model.Job;
import com.project.scheduler.common.model.JobStatus;
import com.project.scheduler.common.model.ScheduleType;

public record JobResponse(UUID id, String name, String description, String payload, ScheduleType scheduleType,
		String cronExpression, int priority, JobStatus status, int retryCount, int maxRetries,
		Instant nextExecutionTime, Instant lastExecutionTime, Instant createdAt, Instant updatedAt) {

	public static JobResponse from(Job job) {
		return new JobResponse(job.id(), job.name(), job.description(), job.payload(), job.scheduleType(),
				job.cronExpression(), job.priority(), job.status(), job.retryCount(), job.maxRetries(),
				job.nextExecutionTime(), job.lastExecutionTime(), job.createdAt(), job.updatedAt());
	}
}
