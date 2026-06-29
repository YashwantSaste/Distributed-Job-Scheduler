package com.project.scheduler.executor.framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JobExecutorFactory {
    private final Map<String, JobExecutor> executors = new ConcurrentHashMap<>();

    public JobExecutorFactory() {
	register("http", new HttpExecutor());
	register("webhook", new WebhookExecutor());
	register("shell", new ShellExecutor());
	register("email", new EmailExecutor());
    }

    public void register(String executorType, JobExecutor executor) {
	executors.put(executorType, executor);
    }

    public JobExecutor get(String executorType) {
	JobExecutor executor = executors
		.get(executorType == null || executorType.isBlank() ? "http" : executorType.toLowerCase());
	if (executor == null) {
	    throw new IllegalArgumentException("Unsupported executor type: " + executorType);
	}
	return executor;
    }
}
