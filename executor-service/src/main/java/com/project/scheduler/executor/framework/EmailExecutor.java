package com.project.scheduler.executor.framework;

import com.project.scheduler.common.json.JacksonJsonSerializer;
import com.project.scheduler.common.model.Job;

public final class EmailExecutor implements JobExecutor {

	private final JacksonJsonSerializer json = new JacksonJsonSerializer();

	public ExecutionResult execute(Job job) {
		JobPayload jobPayload = json.fromJson(job.payload(), JobPayload.class);
		if (jobPayload.to() == null || jobPayload.to().isBlank()) {
			throw new IllegalArgumentException("Email executor requires payload.to");
		}
		return ExecutionResult.success("Email queued to " + jobPayload.to() + " with subject " + jobPayload.subject());
	}
}
