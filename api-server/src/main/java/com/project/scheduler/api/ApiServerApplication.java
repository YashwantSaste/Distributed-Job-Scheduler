package com.project.scheduler.api;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.api.http.HttpServerBootstrap;
import com.project.scheduler.common.config.AppConfig;
import com.project.scheduler.common.config.ConfigurationLoader;
import com.project.scheduler.common.json.JacksonJsonSerializer;
import com.project.scheduler.common.json.JsonSerializer;
import com.project.scheduler.core.concurrent.ThreadPoolFactory;
import com.project.scheduler.database.ConnectionPool;
import com.project.scheduler.database.execution.JdbcExecutionRepository;
import com.project.scheduler.database.job.JdbcJobRepository;
import com.project.scheduler.job.JobService;
import com.project.scheduler.job.JobValidator;
import com.project.scheduler.kafka.KafkaJobEventPublisher;
import com.project.scheduler.kafka.KafkaProducerManager;
import com.project.scheduler.kafka.ProducerFactory;

public final class ApiServerApplication {
	private static final Logger log = LoggerFactory.getLogger(ApiServerApplication.class);

	public static void main(String[] args) {

		AppConfig appConfig = ConfigurationLoader.load("application.properties",
				Path.of("config", "api-server.properties"));

		JsonSerializer json = new JacksonJsonSerializer();

		ThreadPoolExecutor pool = ThreadPoolFactory.bounded("api-http", appConfig.getInt("http.worker.core-size", 4),
				appConfig.getInt("http.worker.max-size", 16), appConfig.getInt("http.worker.queue-capacity", 256));

		ConnectionPool connectionPool = new ConnectionPool(
				appConfig.get("db.url", "jdbc:postgresql://localhost:5432/scheduler"),
				appConfig.get("db.username", "scheduler"), appConfig.get("db.password", "scheduler"),
				appConfig.getInt("db.pool.max-size", 10),
				Duration.ofMillis(appConfig.getInt("db.pool.acquisition-timeout-ms", 5000)));

		KafkaJobEventPublisher publisher = new KafkaJobEventPublisher(
				new KafkaProducerManager(new ProducerFactory(appConfig).createJobEventProducer()));

		JobService jobService = new JobService(new JdbcJobRepository(connectionPool),
				new JdbcExecutionRepository(connectionPool), new JobValidator(), publisher);

		HttpServerBootstrap server = new HttpServerBootstrap(appConfig, json, pool, jobService);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Stopping API server");
			server.stop();
			publisher.close();
			connectionPool.close();
			pool.shutdown();
		}));

		server.start();
	}
}
