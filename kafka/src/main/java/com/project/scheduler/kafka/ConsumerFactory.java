package com.project.scheduler.kafka;

import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.project.scheduler.common.config.AppConfig;
import com.project.scheduler.common.event.JobLifecycleEvent;

public final class ConsumerFactory {
	private final AppConfig config;

	public ConsumerFactory(AppConfig config) {
		this.config = config;
	}

	public Consumer<String, JobLifecycleEvent> createJobEventConsumer(String consumerGroupId) {
		Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				config.get("kafka.bootstrap-servers", "localhost:9092"));
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventDeserializer.class.getName());
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
				config.get("kafka.consumer.auto-offset-reset", "earliest"));

		String securityProtocol = config.get("kafka.security.protocol", null);
		if (securityProtocol != null) {
			properties.put("security.protocol", securityProtocol);
			properties.put("sasl.mechanism", config.get("kafka.sasl.mechanism", "PLAIN"));
			properties.put("sasl.jaas.config", config.get("kafka.sasl.jaas.config", ""));
		}

		while (true) {
			try {
				return new KafkaConsumer<>(properties);
			} catch (Exception e) {
				System.err.println("Kafka Consumer initialization failed (waiting for broker): " + e.getMessage());
				try {
					Thread.sleep(5000);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(ie);
				}
			}
		}
	}
}
