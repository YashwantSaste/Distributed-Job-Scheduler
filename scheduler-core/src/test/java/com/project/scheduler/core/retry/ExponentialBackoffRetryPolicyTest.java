package com.project.scheduler.core.retry;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

class ExponentialBackoffRetryPolicyTest {
  @Test
  void calculatesCappedExponentialBackoff() {
    ExponentialBackoffRetryPolicy retryPolicy =
        new ExponentialBackoffRetryPolicy(Duration.ofSeconds(1), Duration.ofSeconds(5));
    assertEquals(Duration.ofSeconds(1), retryPolicy.backoff(0));
    assertEquals(Duration.ofSeconds(2), retryPolicy.backoff(1));
    assertEquals(Duration.ofSeconds(4), retryPolicy.backoff(2));
    assertEquals(Duration.ofSeconds(5), retryPolicy.backoff(10));
  }

  @Test
  void stopsAtMaxRetries() {
    ExponentialBackoffRetryPolicy retryPolicy =
        new ExponentialBackoffRetryPolicy(Duration.ofSeconds(1), Duration.ofSeconds(5));
    assertTrue(retryPolicy.shouldRetry(2, 3));
    assertFalse(retryPolicy.shouldRetry(3, 3));
  }
}
