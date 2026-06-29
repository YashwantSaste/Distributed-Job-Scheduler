package com.project.scheduler.common.config;

import java.time.Duration;
import java.util.Map;

public final class AppConfig {

	private final Map<String, String> values;

	public AppConfig(Map<String, String> values) {
		this.values = Map.copyOf(values);
	}

	public String get(String key, String defaultValue) {
		return values.getOrDefault(key, defaultValue);
	}

	public int getInt(String key, int defaultValue) {
		String configuredValue = values.get(key);
		return configuredValue == null ? defaultValue : Integer.parseInt(configuredValue);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String configuredValue = values.get(key);
		return configuredValue == null ? defaultValue : Boolean.parseBoolean(configuredValue);
	}

	public String require(String key) {
		String configuredValue = values.get(key);
		if (configuredValue == null || configuredValue.isBlank()) {
			throw new IllegalStateException("Missing configuration: " + key);
		}
		return configuredValue;
	}

	public Duration getDuration(String key, Duration defaultValue) {
		String configuredValue = values.get(key);
		return configuredValue == null ? defaultValue : Duration.parse(configuredValue);
	}

	public static String envKey(String key) {
		return key.toUpperCase().replace('.', '_').replace('-', '_');
	}
}
