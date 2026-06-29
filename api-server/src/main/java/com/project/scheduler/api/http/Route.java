package com.project.scheduler.api.http;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

final class Route {
	private final String method;
	private final Pattern path;
	private final RouteHandler handler;

	Route(String httpMethod, String pathRegex, RouteHandler routeHandler) {
		method = httpMethod;
		path = Pattern.compile(pathRegex);
		handler = routeHandler;
	}

	boolean matches(String httpMethod, String requestPath) {
		return method.equalsIgnoreCase(httpMethod) && path.matcher(requestPath).matches();
	}

	void handle(HttpExchange exchange) throws IOException {
		Matcher matcher = path.matcher(exchange.getRequestURI().getPath());
		matcher.matches();
		handler.handle(exchange, PathParameters.from(matcher));
	}

	@FunctionalInterface
	interface RouteHandler {
		void handle(HttpExchange exchange, Map<String, String> pathParameters) throws IOException;
	}
}
