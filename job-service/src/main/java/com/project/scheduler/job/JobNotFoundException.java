package com.project.scheduler.job;

import java.util.UUID;

public final class JobNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final UUID jobId;

	public JobNotFoundException(UUID id) {
		super("Job not found: " + id);
		jobId = id;
	}

	public UUID jobId() {
		return jobId;
	}
}
