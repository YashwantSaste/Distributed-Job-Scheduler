package com.project.scheduler.job.command;

import com.project.scheduler.common.model.ScheduleType;
import java.time.Instant;

public record CreateJobCommand(
    String name,
    String description,
    String payload,
    ScheduleType scheduleType,
    String cronExpression,
    int priority,
    Integer maxRetries,
    Instant nextExecutionTime) {}
