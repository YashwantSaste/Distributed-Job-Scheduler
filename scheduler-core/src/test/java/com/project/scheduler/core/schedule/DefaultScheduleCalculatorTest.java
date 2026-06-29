package com.project.scheduler.core.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.project.scheduler.common.model.Job;
import com.project.scheduler.common.model.ScheduleType;

class DefaultScheduleCalculatorTest {
	DefaultScheduleCalculator calculator = new DefaultScheduleCalculator();

	@Test
	void oneTimeJobHasNoNextExecutionAfterDispatch() {
		Job job = Job.builder().name("once").scheduleType(ScheduleType.ONE_TIME)
				.nextExecutionTime(Instant.parse("2026-06-28T00:00:00Z")).build();
		assertNull(calculator.nextExecutionTime(job, Instant.parse("2026-06-28T00:00:01Z")));
	}

	@Test
	void fixedRateUsesPreviousScheduledTimeAsBase() {
		Job job = Job.builder().name("rate").scheduleType(ScheduleType.FIXED_RATE).cronExpression("PT30S")
				.nextExecutionTime(Instant.parse("2026-06-28T00:00:00Z")).build();
		assertEquals(Instant.parse("2026-06-28T00:00:30Z"),
				calculator.nextExecutionTime(job, Instant.parse("2026-06-28T00:00:01Z")));
	}
}
