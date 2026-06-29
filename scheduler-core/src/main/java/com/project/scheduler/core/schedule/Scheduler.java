package com.project.scheduler.core.schedule;

import java.util.Collection;
import java.util.List;

import com.project.scheduler.common.model.Job;

public final class Scheduler {

	private final JobQueue jobQueue;
	private final TaskDispatcher taskDispatcher;

	public Scheduler(JobQueue jobQueue, TaskDispatcher taskDispatcher) {
		this.jobQueue = jobQueue;
		this.taskDispatcher = taskDispatcher;
	}

	public void submit(Collection<Job> jobs) {
		jobs.forEach(jobQueue::offer);
	}

	public int dispatchReady(int maxItems) {
		List<Job> readyJobs = jobQueue.drainReady(maxItems);
		readyJobs.forEach(taskDispatcher::dispatch);
		return readyJobs.size();
	}
}
