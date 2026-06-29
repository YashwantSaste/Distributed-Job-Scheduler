package com.project.scheduler.watcher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.common.event.JobEventType;
import com.project.scheduler.common.event.JobLifecycleEvent;
import com.project.scheduler.common.model.Job;
import com.project.scheduler.core.schedule.JobQueue;
import com.project.scheduler.job.JobDispatchService;
import com.project.scheduler.kafka.KafkaProducerManager;
import com.project.scheduler.redis.RedisClientWrapper;

public final class WatcherEngine {
	private static final Logger log = LoggerFactory.getLogger(WatcherEngine.class);
	private final String watcherId;
	private final int batchSize;
	private final Duration lockTtl;
	private final JobDispatchService dispatch;
	private final RedisClientWrapper redis;
	private final KafkaProducerManager kafka;
	private final JobQueue queue;
	private final ReentrantLock lock = new ReentrantLock();
	private final AtomicInteger polls = new AtomicInteger();

	public WatcherEngine(String id, int batchSize, Duration ttl, JobDispatchService dispatchService,
			RedisClientWrapper redisClient, KafkaProducerManager kafkaProducerManager, JobQueue jobQueue) {
		watcherId = id;
		this.batchSize = batchSize;
		lockTtl = ttl;
		dispatch = dispatchService;
		redis = redisClient;
		kafka = kafkaProducerManager;
		queue = jobQueue;
	}

	public void pollAndDispatch() {
		if (!lock.tryLock())
			return;
		try {
			List<Job> due = dispatch.findDueJobs(Instant.now(), batchSize);
			due.forEach(queue::offer);
			List<Job> ready = queue.drainReady(batchSize);
			ready.forEach(this::dispatch);
			log.info("Watcher poll {} dispatched {} jobs", polls.incrementAndGet(), ready.size());
		} finally {
			lock.unlock();
		}
	}

	public void dispatch(Job job) {
		String owner = watcherId + ":" + polls.get();
		if (!redis.acquireJobLock(job.id(), owner, lockTtl))
			return;
		try {
			Instant at = Instant.now();
			kafka.publish(JobLifecycleEvent.of(JobEventType.JOB_RUN_REQUESTED, job.id(), job.name(),
					Map.of("source", "watcher", "watcherId", watcherId))).join();
			dispatch.markDispatched(job, at);
		} finally {
			redis.releaseJobLock(job.id(), owner);
		}
	}
}
