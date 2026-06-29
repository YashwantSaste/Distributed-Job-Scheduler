package com.project.scheduler.job;

import java.time.Instant;
import java.util.List;

import com.project.scheduler.common.model.Job;
import com.project.scheduler.core.schedule.ScheduleCalculator;
import com.project.scheduler.database.job.JobRepository;

public final class JobDispatchService {
	private final JobRepository jobRepository;
	private final ScheduleCalculator scheduleCalculator;

	public JobDispatchService(JobRepository jobRepository, ScheduleCalculator scheduleCalculator) {
		this.jobRepository = jobRepository;
		this.scheduleCalculator = scheduleCalculator;
	}

	public List<Job> findDueJobs(Instant now, int limit) {
		return jobRepository.findDueJobs(now, limit);
	}

	public Job markDispatched(Job job, Instant dispatchedAt) {
		Job updatedJob = Job.builder().id(job.id()).name(job.name()).description(job.description())
				.payload(job.payload()).scheduleType(job.scheduleType()).cronExpression(job.cronExpression())
				.priority(job.priority()).status(job.status()).retryCount(job.retryCount()).maxRetries(job.maxRetries())
				.nextExecutionTime(scheduleCalculator.nextExecutionTime(job, dispatchedAt))
				.lastExecutionTime(dispatchedAt).createdAt(job.createdAt()).updatedAt(dispatchedAt).build();
		return jobRepository.update(updatedJob);
	}
}
