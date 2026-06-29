package com.project.scheduler.kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import com.project.scheduler.common.event.JobLifecycleEvent;

public final class KafkaConsumerManager implements Runnable, AutoCloseable {
	private final Consumer<String, JobLifecycleEvent> consumer;
	private final IdempotencyStore idempotencyStore;
	private final JobEventHandler eventHandler;
	private final Duration pollInterval;
	private final Duration ttl;
	private final AtomicBoolean run = new AtomicBoolean(true);

	public KafkaConsumerManager(Consumer<String, JobLifecycleEvent> consumer, IdempotencyStore idempotencyStore,
			JobEventHandler eventHandler, Duration pollInterval, Duration ttl) {
		this.consumer = consumer;
		this.idempotencyStore = idempotencyStore;
		this.eventHandler = eventHandler;
		this.pollInterval = pollInterval;
		this.ttl = ttl;
	}

	public void subscribe(Collection<String> topics) {
		consumer.subscribe(topics);
	}

	public void run() {
		while (run.get()) {
			ConsumerRecords<String, JobLifecycleEvent> rs = consumer.poll(pollInterval);
			rs.forEach(record -> {
				JobLifecycleEvent event = record.value();
				if (event != null && idempotencyStore.markIfNew(event.eventId(), ttl))
					eventHandler.handle(event);
			});
			if (!rs.isEmpty())
				consumer.commitSync();
		}
	}

	public void close() {
		run.set(false);
		consumer.wakeup();
		consumer.close(Duration.ofSeconds(5));
	}
}
