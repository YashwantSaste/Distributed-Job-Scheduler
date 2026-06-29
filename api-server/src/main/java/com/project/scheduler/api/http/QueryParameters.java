package com.project.scheduler.api.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

public final class QueryParameters {
	private final Map<String, String> values;

	private QueryParameters(Map<String, String> queryValues) {
		values = queryValues;
	}

	public static QueryParameters from(HttpExchange exchange) {
		String rawQuery = exchange.getRequestURI().getRawQuery();
		Map<String, String> parsedValues = new HashMap<>();
		if (rawQuery != null && !rawQuery.isBlank())
			for (String pair : rawQuery.split("&")) {
				String[] p = pair.split("=", 2);
				parsedValues.put(dec(p[0]), p.length == 2 ? dec(p[1]) : "");
			}
		return new QueryParameters(parsedValues);
	}

	public int intValue(String key, int defaultValue, int min, int max) {
		String rawValue = values.get(key);
		int parsedValue = rawValue == null || rawValue.isBlank() ? defaultValue : Integer.parseInt(rawValue);
		if (parsedValue < min || parsedValue > max)
			throw new IllegalArgumentException(parsedValue + " must be between " + min + " and " + max);
		return parsedValue;
	}

	static String dec(String encodedValue) {
		return URLDecoder.decode(encodedValue, StandardCharsets.UTF_8);
	}
}
