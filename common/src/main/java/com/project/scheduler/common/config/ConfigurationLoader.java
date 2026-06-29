package com.project.scheduler.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ConfigurationLoader {
	private ConfigurationLoader() {
	}

	public static AppConfig load(String resourceName, Path externalFile) {
		Map<String, String> values = new HashMap<>();
		loadClasspath(resourceName, values);
		loadExternal(externalFile, values);
		overlayEnvironment(values);
		return new AppConfig(values);
	}

	static void loadClasspath(String resourceName, Map<String, String> values) {
		if (resourceName == null || resourceName.isBlank()) {
			return;
		}
		try (InputStream inputStream = ConfigurationLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
			if (inputStream != null) {
				Properties properties = new Properties();
				properties.load(inputStream);
				properties.forEach((key, value) -> values.put(String.valueOf(key), String.valueOf(value)));
			}
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	static void loadExternal(Path externalFile, Map<String, String> values) {
		if (externalFile == null || !Files.exists(externalFile)) {
			return;
		}
		try (InputStream inputStream = Files.newInputStream(externalFile)) {
			Properties properties = new Properties();
			properties.load(inputStream);
			properties.forEach((key, value) -> values.put(String.valueOf(key), String.valueOf(value)));
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	static void overlayEnvironment(Map<String, String> values) {
		Map<String, String> environment = System.getenv();
		for (String key : values.keySet().toArray(String[]::new)) {
			String environmentValue = environment.get(AppConfig.envKey(key));
			if (environmentValue != null) {
				values.put(key, environmentValue);
			}
		}
	}
}
