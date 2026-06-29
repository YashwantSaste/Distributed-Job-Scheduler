package com.project.scheduler.common.json;

public interface JsonSerializer {

	String toJson(Object value);

	<T> T fromJson(String json, Class<T> type);
}
