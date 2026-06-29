package com.project.scheduler.common.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = Execution.Builder.class)
public final class Execution {

	private final UUID id;
	private final UUID jobId;
	private final ExecutionStatus status;
	private final Instant startedAt, completedAt;
	private final String executorId, errorMessage, logs;
	private final int retryNumber;

	private Execution(Builder builder) {
		id = builder.id;
		jobId = builder.jobId;
		status = builder.status;
		startedAt = builder.startedAt;
		completedAt = builder.completedAt;
		executorId = builder.executorId;
		errorMessage = builder.errorMessage;
		retryNumber = builder.retryNumber;
		logs = builder.logs;
	}

	public UUID id() {
		return id;
	}

	public UUID jobId() {
		return jobId;
	}

	public ExecutionStatus status() {
		return status;
	}

	public Instant startedAt() {
		return startedAt;
	}

	public Instant completedAt() {
		return completedAt;
	}

	public String executorId() {
		return executorId;
	}

	public String errorMessage() {
		return errorMessage;
	}

	public int retryNumber() {
		return retryNumber;
	}

	public String logs() {
		return logs;
	}

	public static Builder builder() {
		return new Builder();
	}

	@JsonPOJOBuilder(withPrefix = "")
	public static final class Builder {

		private UUID id = UUID.randomUUID(), jobId;
		private ExecutionStatus status = ExecutionStatus.QUEUED;
		private Instant startedAt, completedAt;
		private String executorId, errorMessage, logs = "";
		private int retryNumber;

		public Builder id(UUID value) {
			id = value;
			return this;
		}

		public Builder jobId(UUID value) {
			jobId = value;
			return this;
		}

		public Builder status(ExecutionStatus value) {
			status = value;
			return this;
		}

		public Builder startedAt(Instant value) {
			startedAt = value;
			return this;
		}

		public Builder completedAt(Instant value) {
			completedAt = value;
			return this;
		}

		public Builder executorId(String value) {
			executorId = value;
			return this;
		}

		public Builder errorMessage(String value) {
			errorMessage = value;
			return this;
		}

		public Builder retryNumber(int value) {
			retryNumber = value;
			return this;
		}

		public Builder logs(String value) {
			logs = value;
			return this;
		}

		public Execution build() {
			Objects.requireNonNull(id);
			Objects.requireNonNull(jobId);
			Objects.requireNonNull(status);
			if (retryNumber < 0)
				throw new IllegalArgumentException("retryNumber must be >= 0");
			return new Execution(this);
		}
	}
}
