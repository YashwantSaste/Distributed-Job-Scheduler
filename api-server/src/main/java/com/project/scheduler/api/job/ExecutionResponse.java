package com.project.scheduler.api.job;

import com.project.scheduler.common.model.*;
import java.time.*;
import java.util.*;

public record ExecutionResponse(
    UUID id,
    UUID jobId,
    ExecutionStatus status,
    Instant startedAt,
    Instant completedAt,
    String executorId,
    String errorMessage,
    int retryNumber,
    String logs) {
  public static ExecutionResponse from(Execution execution) {
    return new ExecutionResponse(
        execution.id(),
        execution.jobId(),
        execution.status(),
        execution.startedAt(),
        execution.completedAt(),
        execution.executorId(),
        execution.errorMessage(),
        execution.retryNumber(),
        execution.logs());
  }
}
