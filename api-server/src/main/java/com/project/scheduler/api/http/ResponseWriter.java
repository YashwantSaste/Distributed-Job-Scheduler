package com.project.scheduler.api.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.project.scheduler.common.json.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;

public final class ResponseWriter {
	private final JsonSerializer json;

	public ResponseWriter(JsonSerializer jsonSerializer) {
		json = jsonSerializer;
	}

	public void write(HttpExchange exchange, int statusCode, Object responseBody) throws IOException {
		byte[] p = json.toJson(responseBody).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
		exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
		exchange.sendResponseHeaders(statusCode, p.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(p);
		}
	}
}
