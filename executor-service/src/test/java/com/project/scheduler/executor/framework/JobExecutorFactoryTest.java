package com.project.scheduler.executor.framework;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JobExecutorFactoryTest {
  @Test
  void selectsBuiltInExecutors() {
    JobExecutorFactory factory = new JobExecutorFactory();
    assertInstanceOf(HttpExecutor.class, factory.get("http"));
    assertInstanceOf(WebhookExecutor.class, factory.get("webhook"));
    assertInstanceOf(ShellExecutor.class, factory.get("shell"));
    assertInstanceOf(EmailExecutor.class, factory.get("email"));
  }

  @Test
  void rejectsUnsupportedExecutorType() {
    assertThrows(IllegalArgumentException.class, () -> new JobExecutorFactory().get("unknown"));
  }
}
