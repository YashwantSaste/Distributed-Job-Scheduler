package com.project.scheduler.common.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = Job.Builder.class)
public final class Job {

	private final UUID id;
	private final String name;
	private final String description;
	private final String payload;
	private final String cronExpression;
	private final ScheduleType scheduleType;
	private final int priority, retryCount, maxRetries;
	private final JobStatus status;
	private final Instant nextExecutionTime, lastExecutionTime, createdAt, updatedAt;

	private Job(Builder builder) {
		id = builder.id;
		name = builder.name;
		description = builder.description;
		payload = builder.payload;
		scheduleType = builder.scheduleType;
		cronExpression = builder.cronExpression;
		priority = builder.priority;
		status = builder.status;
		retryCount = builder.retryCount;
		maxRetries = builder.maxRetries;
		nextExecutionTime = builder.nextExecutionTime;
		lastExecutionTime = builder.lastExecutionTime;
		createdAt = builder.createdAt;
		updatedAt = builder.updatedAt;
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public String payload() {
		return payload;
	}

	public ScheduleType scheduleType() {
		return scheduleType;
	}

	public String cronExpression() {
		return cronExpression;
	}

	public int priority() {
		return priority;
	}

	public JobStatus status() {
		return status;
	}

	public int retryCount() {
		return retryCount;
	}

	public int maxRetries() {
		return maxRetries;
	}

	public Instant nextExecutionTime() {
		return nextExecutionTime;
	}

	public Instant lastExecutionTime() {
		return lastExecutionTime;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public static Builder builder() {
		return new Builder();
	}

	@JsonPOJOBuilder(withPrefix = "")
	public static final class Builder {
		private UUID id = UUID.randomUUID();
		private String name, description = "", payload = "{}", cronExpression;
		private ScheduleType scheduleType = ScheduleType.ONE_TIME;
		private int priority, retryCount, maxRetries = 3;
		private JobStatus status = JobStatus.CREATED;
		private Instant nextExecutionTime, lastExecutionTime, createdAt = Instant.now(), updatedAt = Instant.now();

		public Builder id(UUID value) {
			id = value;
			return this;
		}

		public Builder name(String value) {
			name = value;
			return this;
		}

		public Builder description(String value) {
			description = value;
			return this;
		}

		public Builder payload(String value) {
			payload = value;
			return this;
		}

		public Builder scheduleType(ScheduleType value) {
			scheduleType = value;
			return this;
		}

		public Builder cronExpression(String value) {
			cronExpression = value;
			return this;
		}

		public Builder priority(int value) {
			priority = value;
			return this;
		}

		public Builder status(JobStatus value) {
			status = value;
			return this;
		}

		public Builder retryCount(int value) {
			retryCount = value;
			return this;
		}

		public Builder maxRetries(int value) {
			maxRetries = value;
			return this;
		}

		public Builder nextExecutionTime(Instant value) {
			nextExecutionTime = value;
			return this;
		}

		public Builder lastExecutionTime(Instant value) {
			lastExecutionTime = value;
			return this;
		}

		public Builder createdAt(Instant value) {
			createdAt = value;
			return this;
		}

		public Builder updatedAt(Instant value) {
			updatedAt = value;
			return this;
		}

		public Job build() {
			Objects.requireNonNull(id);
			Objects.requireNonNull(name);
			Objects.requireNonNull(scheduleType);
			Objects.requireNonNull(status);
			Objects.requireNonNull(createdAt);
			Objects.requireNonNull(updatedAt);
			if (maxRetries < 0 || retryCount < 0)
				throw new IllegalArgumentException("retry counts must be >= 0");
			return new Job(this);
		}
	}
}
