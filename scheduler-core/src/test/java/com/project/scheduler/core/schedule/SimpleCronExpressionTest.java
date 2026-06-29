package com.project.scheduler.core.schedule;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

class SimpleCronExpressionTest {
  @Test
  void calculatesNextFiveMinuteBoundary() {
    assertEquals(
        Instant.parse("2026-06-28T00:05:00Z"),
        new SimpleCronExpression().next("*/5 * * * *", Instant.parse("2026-06-28T00:01:00Z")));
  }

  @Test
  void respectsDayOfWeekConstraint() {
    // Monday 2026-06-29 at 09:00 UTC — next weekday slot after Friday 2026-06-26
    assertEquals(
        Instant.parse("2026-06-29T09:00:00Z"),
        new SimpleCronExpression().next("0 9 * * 1-5", Instant.parse("2026-06-26T10:00:00Z")));
  }
}
