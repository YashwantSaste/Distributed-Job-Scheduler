package com.project.scheduler.api.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.api.job.JobController;
import com.project.scheduler.common.config.AppConfig;
import com.project.scheduler.common.json.JsonSerializer;
import com.project.scheduler.job.JobService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class HttpServerBootstrap {

	private static final Logger log = LoggerFactory.getLogger(HttpServerBootstrap.class);
	private static final String UUID = "(?<id>[0-9a-fA-F-]{36})";

	private final AppConfig appConfig;
	private final JsonSerializer jsonSerializer;
	private final Executor serverExecutor;
	private final JobService jobService;

	private HttpServer server;

	public HttpServerBootstrap(AppConfig appConfig, JsonSerializer jsonSerializer, Executor serverExecutor,
			JobService jobService) {
		this.appConfig = appConfig;
		this.jsonSerializer = jsonSerializer;
		this.serverExecutor = serverExecutor;
		this.jobService = jobService;
	}

	public void start() {
		try {
			String host = appConfig.get("http.host", "0.0.0.0");
			int port = appConfig.getInt("http.port", 8080);

			server = HttpServer.create(new InetSocketAddress(host, port), appConfig.getInt("http.backlog", 64));

			server.setExecutor(serverExecutor);
			server.createContext("/", router());
			server.start();

			log.info("API server listening on {}:{}", host, port);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	public void stop() {
		if (server != null) {
			server.stop(5);
		}
	}

	private Router router() {
		ResponseWriter responseWriter = new ResponseWriter(jsonSerializer);
		JobController jobController = new JobController(jobService, new RequestReader(jsonSerializer), responseWriter);

		Router router = new Router(responseWriter);

		router.add("GET", "^/health$", (exchange, pathParameters) -> responseWriter.write(exchange, 200,
				new ApiResponse("ok", "api-server is healthy")));

		router.add("POST", "^/jobs$", jobController::create);
		router.add("GET", "^/jobs$", jobController::list);
		router.add("GET", "^/jobs/" + UUID + "$", jobController::get);
		router.add("PUT", "^/jobs/" + UUID + "$", jobController::update);
		router.add("DELETE", "^/jobs/" + UUID + "$", jobController::delete);
		router.add("POST", "^/jobs/" + UUID + "/cancel$", jobController::cancel);
		router.add("POST", "^/jobs/" + UUID + "/pause$", jobController::pause);
		router.add("POST", "^/jobs/" + UUID + "/resume$", jobController::resume);
		router.add("GET", "^/jobs/" + UUID + "/history$", jobController::history);
		router.add("POST", "^/jobs/" + UUID + "/retry$", jobController::retry);
		router.add("POST", "^/jobs/" + UUID + "/trigger$", jobController::triggerPending);

		router.add("GET", "^/$", (exchange, pathParameters) -> redirect(exchange, "/ui/index.html"));
		router.add("GET", "^/ui$", (exchange, pathParameters) -> redirect(exchange, "/ui/index.html"));
		router.add("GET", "^/ui/$", (exchange, pathParameters) -> redirect(exchange, "/ui/index.html"));

		router.add("GET", "^/ui/index.html$",
				(exchange, pathParameters) -> serveResource(exchange, "/ui/index.html", "text/html"));

		router.add("GET", "^/ui/styles.css$",
				(exchange, pathParameters) -> serveResource(exchange, "/ui/styles.css", "text/css"));

		router.add("GET", "^/ui/app.js$",
				(exchange, pathParameters) -> serveResource(exchange, "/ui/app.js", "application/javascript"));

		router.add("GET", "^/openapi.yaml$",
				(exchange, pathParameters) -> serveResource(exchange, "/openapi/openapi.yaml", "application/yaml"));

		router.add("GET", "^/docs$", (exchange, pathParameters) -> redirect(exchange, "/swagger-ui/index.html"));
		router.add("GET", "^/swagger-ui$", (exchange, pathParameters) -> redirect(exchange, "/swagger-ui/index.html"));
		router.add("GET", "^/swagger-ui/$", (exchange, pathParameters) -> redirect(exchange, "/swagger-ui/index.html"));
		router.add("GET", "^/swagger-ui/index.html$",
				(exchange, pathParameters) -> serveResource(exchange, "/swagger-ui/index.html", "text/html"));

		return router;
	}

	private void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {

		try (InputStream is = getClass().getResourceAsStream(resourcePath)) {

			if (is == null) {
				byte[] errorBytes = ("Resource not found: " + resourcePath).getBytes(StandardCharsets.UTF_8);

				exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
				exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

				exchange.sendResponseHeaders(404, errorBytes.length);

				try (OutputStream os = exchange.getResponseBody()) {
					os.write(errorBytes);
				}
				return;
			}

			byte[] bytes = is.readAllBytes();

			exchange.getResponseHeaders().set("Content-Type", contentType);
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

			exchange.sendResponseHeaders(200, bytes.length);

			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}
	}

	private void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().set("Location", location);
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}
}