package com.project.scheduler.watcher;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.common.config.AppConfig;
import com.project.scheduler.common.config.ConfigurationLoader;
import com.project.scheduler.core.concurrent.ThreadPoolFactory;
import com.project.scheduler.core.schedule.DefaultScheduleCalculator;
import com.project.scheduler.core.schedule.JobQueue;
import com.project.scheduler.database.ConnectionPool;
import com.project.scheduler.database.job.JdbcJobRepository;
import com.project.scheduler.job.JobDispatchService;
import com.project.scheduler.kafka.KafkaProducerManager;
import com.project.scheduler.kafka.ProducerFactory;
import com.project.scheduler.redis.RedisClientWrapper;

public final class WatcherServiceApplication {
	private static final Logger log = LoggerFactory.getLogger(WatcherServiceApplication.class);

	public static void main(String[] args) {

		AppConfig appConfig = ConfigurationLoader.load("application.properties",
				Path.of("config", "watcher-service.properties"));

		ConnectionPool connectionPool = new ConnectionPool(
				appConfig.get("db.url", "jdbc:postgresql://localhost:5432/scheduler"),
				appConfig.get("db.username", "scheduler"), appConfig.get("db.password", "scheduler"),
				appConfig.getInt("db.pool.max-size", 5),
				Duration.ofMillis(appConfig.getInt("db.pool.acquisition-timeout-ms", 5000)));

		RedisClientWrapper redis = new RedisClientWrapper(appConfig.get("redis.host", "localhost"),
				appConfig.getInt("redis.port", 6379), appConfig.get("redis.password", ""));

		KafkaProducerManager kafka = new KafkaProducerManager(new ProducerFactory(appConfig).createJobEventProducer());
		WatcherEngine engine = new WatcherEngine(appConfig.get("watcher.id", "watcher-1"),
				appConfig.getInt("watcher.batch-size", 100),
				Duration.ofSeconds(appConfig.getInt("watcher.lock-ttl-seconds", 60)),
				new JobDispatchService(new JdbcJobRepository(connectionPool), new DefaultScheduleCalculator()), redis,
				kafka, new JobQueue());

		ScheduledExecutorService executorService = ThreadPoolFactory.scheduled("watcher-poll", 1);

		executorService.scheduleWithFixedDelay(engine::pollAndDispatch, 0,
				appConfig.getInt("watcher.poll-interval-seconds", 20), TimeUnit.SECONDS);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Stopping watcher-service");
			executorService.shutdown();
			kafka.close();
			redis.close();
			connectionPool.close();
		}));

		log.info("watcher-service started");
	}
}
