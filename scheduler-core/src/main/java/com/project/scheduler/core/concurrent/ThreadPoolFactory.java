package com.project.scheduler.core.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPoolFactory {

	private ThreadPoolFactory() {
	}

	public static ThreadPoolExecutor bounded(String threadNamePrefix, int corePoolSize, int maxPoolSize,
			int queueCapacity) {
		return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(queueCapacity), threadFactory(threadNamePrefix),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	public static ScheduledExecutorService scheduled(String threadNamePrefix, int poolSize) {
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(poolSize,
				threadFactory(threadNamePrefix));
		executor.setRemoveOnCancelPolicy(true);
		return executor;
	}

	static ThreadFactory threadFactory(String threadNamePrefix) {
		AtomicInteger threadCounter = new AtomicInteger();
		return runnable -> {
			Thread thread = Thread.ofPlatform().unstarted(runnable);
			thread.setName(threadNamePrefix + "-" + threadCounter.incrementAndGet());
			return thread;
		};
	}
}
