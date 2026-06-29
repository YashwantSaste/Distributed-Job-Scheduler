package com.project.scheduler.kafka;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.project.scheduler.common.event.JobLifecycleEvent;

public final class KafkaProducerManager implements AutoCloseable {
	private final Producer<String, JobLifecycleEvent> producer;

	public KafkaProducerManager(Producer<String, JobLifecycleEvent> producer) {
		this.producer = producer;
	}

	public CompletableFuture<Void> publish(JobLifecycleEvent event) {
		CompletableFuture<Void> publishFuture = new CompletableFuture<>();
		producer.send(new ProducerRecord<>(JobTopics.forType(event.type()), event.jobId().toString(), event),
				(metadata, exception) -> {
					if (exception != null)
						publishFuture.completeExceptionally(exception);
					else
						publishFuture.complete(null);
				});
		return publishFuture;
	}

	public void flush() {
		producer.flush();
	}

	public void close() {
		producer.close(Duration.ofSeconds(5));
	}
}
