package com.project.scheduler.job;

import java.util.UUID;

import com.project.scheduler.common.model.Job;

public interface JobEventPublisher extends AutoCloseable {

	void jobCreated(Job jobob);

	void jobUpdated(Job jobob);

	void jobDeleted(UUID id);

	void jobRunRequested(Job jobob);

	void jobRetryRequested(Job jobob);

	void jobCancelRequested(Job jobob);

	default void close() {

	}

	static JobEventPublisher defaultPublisher() {

		return new JobEventPublisher() {
			public void jobCreated(Job job) {
			}

			public void jobUpdated(Job job) {
			}

			public void jobDeleted(UUID id) {
			}

			public void jobRunRequested(Job job) {
			}

			public void jobRetryRequested(Job job) {
			}

			public void jobCancelRequested(Job job) {
			}
		};
	}
}
