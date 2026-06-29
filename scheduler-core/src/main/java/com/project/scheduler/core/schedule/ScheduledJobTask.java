package com.project.scheduler.core.schedule;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.project.scheduler.common.model.Job;

public final class ScheduledJobTask implements Delayed {

	private final Job job;
	private final Instant readyAt;

	public ScheduledJobTask(Job job, Instant readyAt) {
		this.job = job;
		this.readyAt = readyAt;
	}

	public Job job() {
		return job;
	}

	public Instant readyAt() {
		return readyAt;
	}

	public long getDelay(TimeUnit timeUnit) {
		return timeUnit.convert(Duration.between(Instant.now(), readyAt));
	}

	public int compareTo(Delayed other) {
		ScheduledJobTask otherTask = (ScheduledJobTask) other;
		int timeComparison = readyAt.compareTo(otherTask.readyAt);
		return timeComparison != 0 ? timeComparison : Integer.compare(otherTask.job.priority(), job.priority());
	}
}
