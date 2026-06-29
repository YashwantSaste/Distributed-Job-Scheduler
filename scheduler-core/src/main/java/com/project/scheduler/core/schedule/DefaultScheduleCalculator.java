package com.project.scheduler.core.schedule;

import java.time.Duration;
import java.time.Instant;

import com.project.scheduler.common.model.Job;
import com.project.scheduler.common.model.ScheduleType;

public final class DefaultScheduleCalculator implements ScheduleCalculator {

	private final SimpleCronExpression cron = new SimpleCronExpression();

	public Instant nextExecutionTime(Job job, Instant after) {
		return switch (job.scheduleType()) {
		case ONE_TIME -> null;
		case CRON -> cron.next(job.cronExpression(), after);
		case FIXED_DELAY -> fixedDelay(job, after);
		case FIXED_RATE -> fixedRate(job, after);
		};
	}

	private Instant fixedRate(Job job, Instant after) {
		Duration interval = parse(job.cronExpression());
		Instant base = job.nextExecutionTime() != null ? job.nextExecutionTime() : after;
		Instant nextExecutionTime = base.plus(interval);
		return nextExecutionTime.isAfter(after) ? nextExecutionTime : after.plus(interval);
	}

	private Instant fixedDelay(Job job, Instant after) {
		Duration interval = parse(job.cronExpression());
		Instant base = job.lastExecutionTime() == null ? after : job.lastExecutionTime();
		Instant nextExecutionTime = base.plus(interval);
		return nextExecutionTime.isAfter(after) ? nextExecutionTime : after.plus(interval);
	}

	private Duration parse(String expression) {
		if (expression == null || expression.isBlank()) {
			return Duration.ofMinutes(1);
		}
		if (expression.startsWith("PT")) {
			return Duration.parse(expression);
		}
		return Duration.ofSeconds(Long.parseLong(expression));
	}
}
