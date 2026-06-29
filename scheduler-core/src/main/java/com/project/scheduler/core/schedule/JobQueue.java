package com.project.scheduler.core.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;

import com.project.scheduler.common.model.Job;

public final class JobQueue {

	private final DelayQueue<ScheduledJobTask> queue = new DelayQueue<>();

	public void offer(Job job) {
		if (job.nextExecutionTime() != null) {
			queue.offer(new ScheduledJobTask(job, job.nextExecutionTime()));
		}
	}

	public List<Job> drainReady(int maxItems) {
		List<Job> readyJobs = new ArrayList<>();
		while (readyJobs.size() < maxItems) {
			ScheduledJobTask task = queue.poll();
			if (task == null) {
				break;
			}
			readyJobs.add(task.job());
		}
		readyJobs.sort((leftJob, rightJob) -> Integer.compare(rightJob.priority(), leftJob.priority()));
		return readyJobs;
	}

	public int size() {
		return queue.size();
	}
}
