package com.project.scheduler.job.command;

import com.project.scheduler.common.model.ScheduleType;
import java.time.Instant;

public record UpdateJobCommand(
    String name,
    String description,
    String payload,
    ScheduleType scheduleType,
    String cronExpression,
    Integer priority,
    Integer maxRetries,
    Instant nextExecutionTime) {}
