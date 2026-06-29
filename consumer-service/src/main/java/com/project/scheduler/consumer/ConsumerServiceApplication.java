package com.project.scheduler.consumer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.common.config.AppConfig;
import com.project.scheduler.common.config.ConfigurationLoader;
import com.project.scheduler.common.event.JobEventType;
import com.project.scheduler.kafka.ConsumerFactory;
import com.project.scheduler.kafka.JobTopics;
import com.project.scheduler.kafka.KafkaConsumerManager;
import com.project.scheduler.redis.RedisClientWrapper;
import com.project.scheduler.redis.RedisIdempotencyStore;

public final class ConsumerServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(ConsumerServiceApplication.class);

	public static void main(String[] args) {

		AppConfig appConfig = ConfigurationLoader.load("application.properties",
				Path.of("config", "consumer-service.properties"));

		RedisClientWrapper redis = new RedisClientWrapper(appConfig.get("redis.host", "localhost"),
				appConfig.getInt("redis.port", 6379), appConfig.get("redis.password", ""));

		KafkaConsumerManager consumerManager = new KafkaConsumerManager(
				new ConsumerFactory(appConfig)
						.createJobEventConsumer(appConfig.get("kafka.consumer.group-id", "job-consumer-service")),
				new RedisIdempotencyStore(redis), e -> {
					if (e.type() == JobEventType.JOB_CANCEL_REQUESTED) {
						redis.requestCancellation(e.jobId(),
								Duration.ofSeconds(appConfig.getInt("redis.cancellation-ttl-seconds", 86400)));
						log.info("Cancellation flag set for job {}", e.jobId());
					} else
						log.info("Job event {} for job {} is ready for executor selection", e.type(), e.jobId());
				}, Duration.ofMillis(appConfig.getInt("kafka.consumer.poll-timeout-ms", 1000)),
				Duration.ofSeconds(appConfig.getInt("kafka.consumer.idempotency-ttl-seconds", 86400)));

		consumerManager.subscribe(List.of(JobTopics.JOB_RUN, JobTopics.JOB_RETRY, JobTopics.JOB_CANCEL));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			consumerManager.close();
			redis.close();
		}));

		consumerManager.run();
	}
}
