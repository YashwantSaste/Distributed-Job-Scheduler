package com.project.scheduler.kafka;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.serialization.Deserializer;

import com.project.scheduler.common.event.JobLifecycleEvent;
import com.project.scheduler.common.json.JacksonJsonSerializer;
import com.project.scheduler.common.json.JsonSerializer;

public final class EventDeserializer implements Deserializer<JobLifecycleEvent> {
	private final JsonSerializer json = new JacksonJsonSerializer();

	public JobLifecycleEvent deserialize(String topic, byte[] data) {
		return data == null || data.length == 0 ? null
				: json.fromJson(new String(data, StandardCharsets.UTF_8), JobLifecycleEvent.class);
	}
}
