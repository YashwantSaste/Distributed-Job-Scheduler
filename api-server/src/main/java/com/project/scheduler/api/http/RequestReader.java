package com.project.scheduler.api.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.project.scheduler.common.json.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;

public final class RequestReader {
	private final JsonSerializer json;

	public RequestReader(JsonSerializer jsonSerializer) {
		json = jsonSerializer;
	}

	public <T> T readJson(HttpExchange exchange, Class<T> targetType) throws IOException {
		String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		if (requestBody.isBlank())
			throw new IllegalArgumentException("Request body is required");
		return json.fromJson(requestBody, targetType);
	}
}
