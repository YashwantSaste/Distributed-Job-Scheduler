package com.project.scheduler.kafka;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.serialization.Serializer;

import com.project.scheduler.common.json.JacksonJsonSerializer;
import com.project.scheduler.common.json.JsonSerializer;

public final class EventSerializer<T> implements Serializer<T> {

	private final JsonSerializer json = new JacksonJsonSerializer();

	public byte[] serialize(String topic, T data) {
		return data == null ? null : json.toJson(data).getBytes(StandardCharsets.UTF_8);
	}
}
