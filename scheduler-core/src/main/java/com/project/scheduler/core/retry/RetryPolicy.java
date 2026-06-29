package com.project.scheduler.core.retry;

import java.time.Duration;

public interface RetryPolicy {

	boolean shouldRetry(int retryNumber, int maxRetries);

	Duration backoff(int retryNumber);
}
