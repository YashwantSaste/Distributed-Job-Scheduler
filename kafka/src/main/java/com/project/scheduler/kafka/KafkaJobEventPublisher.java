package com.project.scheduler.kafka;

import java.util.Map;
import java.util.UUID;

import com.project.scheduler.common.event.JobEventType;
import com.project.scheduler.common.event.JobLifecycleEvent;
import com.project.scheduler.common.model.Job;
import com.project.scheduler.job.JobEventPublisher;

public final class KafkaJobEventPublisher implements JobEventPublisher {
	private final KafkaProducerManager producerManager;

	public KafkaJobEventPublisher(KafkaProducerManager producerManager) {
		this.producerManager = producerManager;
	}

	public void jobCreated(Job job) {
		publish(JobEventType.JOB_CREATED, job);
	}

	public void jobUpdated(Job job) {
		publish(JobEventType.JOB_UPDATED, job);
	}

	public void jobDeleted(UUID id) {
		producerManager.publish(JobLifecycleEvent.of(JobEventType.JOB_DELETED, id, null)).join();
	}

	public void jobRunRequested(Job job) {
		publish(JobEventType.JOB_RUN_REQUESTED, job, Map.of("source", "api-trigger"));
	}

	public void jobRetryRequested(Job job) {
		publish(JobEventType.JOB_RETRY_REQUESTED, job, Map.of("source", "api-retry"));
	}

	public void jobCancelRequested(Job job) {
		publish(JobEventType.JOB_CANCEL_REQUESTED, job);
	}

	public void close() {
		producerManager.close();
	}

	void publish(JobEventType eventType, Job job) {
		publish(eventType, job, Map.of());
	}

	void publish(JobEventType eventType, Job job, Map<String, String> metadata) {
		producerManager.publish(JobLifecycleEvent.of(eventType, job.id(), job.name(), metadata)).join();
	}
}
