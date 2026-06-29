package com.project.scheduler.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import com.project.scheduler.common.config.AppConfig;
import com.project.scheduler.common.event.JobLifecycleEvent;

public final class ProducerFactory {
	private final AppConfig config;

	public ProducerFactory(AppConfig config) {
		this.config = config;
	}

	public Producer<String, JobLifecycleEvent> createJobEventProducer() {
		Properties properties = new Properties();
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				config.get("kafka.bootstrap-servers", "localhost:9092"));
		properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventSerializer.class.getName());
		properties.put(ProducerConfig.ACKS_CONFIG, config.get("kafka.producer.acks", "all"));
		properties.put(ProducerConfig.RETRIES_CONFIG, config.getInt("kafka.producer.retries", 3));
		properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, config.getBoolean("kafka.producer.idempotence", true));
		return new KafkaProducer<>(properties);
	}
}
