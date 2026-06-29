package com.project.scheduler.core.schedule;

import java.time.Instant;

import com.project.scheduler.common.model.Job;

public interface ScheduleCalculator {

	Instant nextExecutionTime(Job job, Instant after);
}
