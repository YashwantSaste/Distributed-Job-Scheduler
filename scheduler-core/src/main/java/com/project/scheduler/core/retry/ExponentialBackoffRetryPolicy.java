package com.project.scheduler.core.retry;

import java.time.Duration;

public final class ExponentialBackoffRetryPolicy implements RetryPolicy {

	private final Duration initialBackoff;
	private final Duration maxBackoff;

	public ExponentialBackoffRetryPolicy(Duration initialBackoff, Duration maxBackoff) {
		this.initialBackoff = initialBackoff;
		this.maxBackoff = maxBackoff;
	}

	public boolean shouldRetry(int retryNumber, int maxRetries) {
		return retryNumber < maxRetries;
	}

	public Duration backoff(int retryNumber) {
		long multiplier = 1L << Math.min(Math.max(0, retryNumber), 20);
		Duration computedBackoff = initialBackoff.multipliedBy(multiplier);
		return computedBackoff.compareTo(maxBackoff) > 0 ? maxBackoff : computedBackoff;
	}
}
