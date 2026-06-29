package com.project.scheduler.executor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.common.config.AppConfig;
import com.project.scheduler.common.config.ConfigurationLoader;
import com.project.scheduler.common.event.JobEventType;
import com.project.scheduler.common.event.JobLifecycleEvent;
import com.project.scheduler.core.concurrent.ThreadPoolFactory;
import com.project.scheduler.core.retry.ExponentialBackoffRetryPolicy;
import com.project.scheduler.database.ConnectionPool;
import com.project.scheduler.database.execution.JdbcExecutionRepository;
import com.project.scheduler.database.job.JdbcJobRepository;
import com.project.scheduler.executor.framework.JobExecutorFactory;
import com.project.scheduler.kafka.ConsumerFactory;
import com.project.scheduler.kafka.JobTopics;
import com.project.scheduler.kafka.KafkaConsumerManager;
import com.project.scheduler.kafka.KafkaProducerManager;
import com.project.scheduler.kafka.ProducerFactory;
import com.project.scheduler.redis.RedisClientWrapper;
import com.project.scheduler.redis.RedisIdempotencyStore;

public final class ExecutorServiceApplication {
	private static final Logger log = LoggerFactory.getLogger(ExecutorServiceApplication.class);

	public static void main(String[] a) {
		AppConfig appConfig = ConfigurationLoader.load("application.properties",
				Path.of("config", "executor-service.properties"));

		ThreadPoolExecutor workers = ThreadPoolFactory.bounded("job-executor",
				appConfig.getInt("executor.pool.core-size", 4), appConfig.getInt("executor.pool.max-size", 16),
				appConfig.getInt("executor.pool.queue-capacity", 256));

		ScheduledExecutorService schedulerService = ThreadPoolFactory.scheduled("executor-heartbeat", 1);

		ConnectionPool connectionPool = new ConnectionPool(
				appConfig.get("db.url", "jdbc:postgresql://localhost:5432/scheduler"),
				appConfig.get("db.username", "scheduler"), appConfig.get("db.password", "scheduler"),
				appConfig.getInt("db.pool.max-size", 10),
				Duration.ofMillis(appConfig.getInt("db.pool.acquisition-timeout-ms", 5000)));

		RedisClientWrapper redis = new RedisClientWrapper(appConfig.get("redis.host", "localhost"),
				appConfig.getInt("redis.port", 6379), appConfig.get("redis.password", ""));

		KafkaProducerManager producer = new KafkaProducerManager(new ProducerFactory(appConfig).createJobEventProducer());

		ExecutionOrchestrator orchestrator = new ExecutionOrchestrator(appConfig.get("executor.id", "executor-1"),
				new JdbcJobRepository(connectionPool), new JdbcExecutionRepository(connectionPool),
				new JobExecutorFactory(),
				new ExponentialBackoffRetryPolicy(Duration.ofMillis(appConfig.getInt("retry.initial-backoff-ms", 1000)),
						Duration.ofMillis(appConfig.getInt("retry.max-backoff-ms", 60000))),
				redis, producer, workers);

		KafkaConsumerManager consumer = new KafkaConsumerManager(
				new ConsumerFactory(appConfig)
						.createJobEventConsumer(appConfig.get("kafka.consumer.group-id", "executor-service")),
				new RedisIdempotencyStore(redis), event -> handle(orchestrator, event),
				Duration.ofMillis(appConfig.getInt("kafka.consumer.poll-timeout-ms", 1000)),
				Duration.ofSeconds(appConfig.getInt("kafka.consumer.idempotency-ttl-seconds", 86400)));

		consumer.subscribe(List.of(JobTopics.JOB_RUN, JobTopics.JOB_RETRY));

		schedulerService.scheduleWithFixedDelay(
				() -> orchestrator
						.heartbeat(Duration.ofSeconds(appConfig.getInt("executor.heartbeat-ttl-seconds", 30))),
				0, appConfig.getInt("executor.heartbeat-seconds", 10), TimeUnit.SECONDS);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			consumer.close();
			schedulerService.shutdown();
			workers.shutdown();
			producer.close();
			redis.close();
			connectionPool.close();
		}));
		log.info("executor-service consuming job-run and job-retry topics");
		consumer.run();
	}

	static void handle(ExecutionOrchestrator orchestrator, JobLifecycleEvent event) {
		log.info("Received event {} for job {}", event.type(), event.jobId());

		int retryNumber = parseInt(event.metadata().get("retryNumber"), 0);

		if (event.type() == JobEventType.JOB_RETRY_REQUESTED) {
			long backoffMillis = parseLong(event.metadata().get("backoffMillis"), 0);

			if (backoffMillis > 0) {
				try {
					Thread.sleep(backoffMillis);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		log.info("Submitting job {} for execution", event.jobId());
		orchestrator.executeAsync(event.jobId(), retryNumber);
	}

	static int parseInt(String value, int defaultValue) {
		return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
	}

	static long parseLong(String value, long defaultValue) {
		return value == null || value.isBlank() ? defaultValue : Long.parseLong(value);
	}
}
