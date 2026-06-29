package com.project.scheduler.api.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public final class Router implements HttpHandler {
	private static final Logger log = LoggerFactory.getLogger(Router.class);
	private final List<Route> routes = new ArrayList<>();
	private final ResponseWriter responses;

	public Router(ResponseWriter responseWriter) {
		responses = responseWriter;
	}

	public void add(String httpMethod, String pathRegex, Route.RouteHandler routeHandler) {
		routes.add(new Route(httpMethod, pathRegex, routeHandler));
	}

	public void handle(HttpExchange exchange) throws IOException {
		String httpMethod = exchange.getRequestMethod(), requestPath = exchange.getRequestURI().getPath();
		log.info("HTTP {} {}", httpMethod, requestPath);
		if ("OPTIONS".equalsIgnoreCase(httpMethod)) {
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
			exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
			exchange.sendResponseHeaders(204, -1);
			exchange.close();
			return;
		}
		try {
			for (Route route : routes)
				if (route.matches(httpMethod, requestPath)) {
					route.handle(exchange);
					return;
				}
			responses.write(exchange, 404,
					new ApiResponse("not_found", "No route matched " + httpMethod + " " + requestPath));
		} catch (IllegalArgumentException exception) {
			responses.write(exchange, 400, new ApiResponse("bad_request", exception.getMessage()));
		} catch (Exception exception) {
			log.error("Unhandled HTTP request failure", exception);
			responses.write(exchange, 500, new ApiResponse("internal_error", "Unexpected server error"));
		}
	}
}
