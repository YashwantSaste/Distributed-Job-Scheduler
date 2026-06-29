package com.project.scheduler.api.job;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.api.http.ApiResponse;
import com.project.scheduler.api.http.QueryParameters;
import com.project.scheduler.api.http.RequestReader;
import com.project.scheduler.api.http.ResponseWriter;
import com.project.scheduler.common.validation.ValidationException;
import com.project.scheduler.database.DatabaseException;
import com.project.scheduler.job.JobNotFoundException;
import com.project.scheduler.job.JobService;
import com.project.scheduler.job.command.CreateJobCommand;
import com.project.scheduler.job.command.UpdateJobCommand;
import com.sun.net.httpserver.HttpExchange;

public final class JobController {

	private static final Logger log = LoggerFactory.getLogger(JobController.class);
	private final JobService jobService;
	private final RequestReader requestReader;
	private final ResponseWriter responseWriter;

	public JobController(JobService jobService, RequestReader requestReader, ResponseWriter responseWriter) {
		this.jobService = jobService;
		this.requestReader = requestReader;
		this.responseWriter = responseWriter;
	}

	public void create(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange, () -> responseWriter.write(exchange, 201,
				JobResponse.from(jobService.create(requestReader.readJson(exchange, CreateJobCommand.class)))));
	}

	public void list(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange, () -> {
			QueryParameters queryParameters = QueryParameters.from(exchange);
			responseWriter.write(exchange, 200,
					jobService
							.list(queryParameters.intValue("limit", 50, 1, 200),
									queryParameters.intValue("offset", 0, 0, 100000))
							.stream().map(JobResponse::from).toList());
		});
	}

	public void get(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange,
				() -> responseWriter.write(exchange, 200, JobResponse.from(jobService.get(id(pathParameters)))));
	}

	public void update(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange, () -> responseWriter.write(exchange, 200, JobResponse.from(
				jobService.update(id(pathParameters), requestReader.readJson(exchange, UpdateJobCommand.class)))));
	}

	public void delete(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange, () -> {
			UUID id = id(pathParameters);
			jobService.delete(id);
			responseWriter.write(exchange, 200, new ApiResponse("deleted", "Job deleted: " + id));
		});
	}

	public void pause(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange,
				() -> responseWriter.write(exchange, 200, JobResponse.from(jobService.pause(id(pathParameters)))));
	}

	public void resume(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange,
				() -> responseWriter.write(exchange, 200, JobResponse.from(jobService.resume(id(pathParameters)))));
	}

	public void cancel(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange,
				() -> responseWriter.write(exchange, 200, JobResponse.from(jobService.cancel(id(pathParameters)))));
	}

	public void history(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange, () -> {
			QueryParameters queryParameters = QueryParameters.from(exchange);
			responseWriter.write(exchange, 200,
					jobService
							.history(id(pathParameters), queryParameters.intValue("limit", 50, 1, 200),
									queryParameters.intValue("offset", 0, 0, 100000))
							.stream().map(ExecutionResponse::from).toList());
		});
	}

	public void triggerPending(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange,
				() -> responseWriter.write(exchange, 202, JobResponse.from(jobService.trigger(id(pathParameters)))));
	}

	public void retry(HttpExchange exchange, Map<String, String> pathParameters) throws IOException {
		handle(exchange,
				() -> responseWriter.write(exchange, 202, JobResponse.from(jobService.retry(id(pathParameters)))));
	}

	void handle(HttpExchange exchange, ThrowingHandler handler) throws IOException {
		try {
			handler.handle();
		} catch (ValidationException exception) {
			responseWriter.write(exchange, 400, new ErrorResponse("validation_failed", exception.errors()));
		} catch (JobNotFoundException exception) {
			responseWriter.write(exchange, 404, new ApiResponse("not_found", exception.getMessage()));
		} catch (IllegalArgumentException exception) {
			responseWriter.write(exchange, 400, new ApiResponse("bad_request", exception.getMessage()));
		} catch (DatabaseException exception) {
			log.error("Database failure", exception);
			responseWriter.write(exchange, 503, new ApiResponse("database_unavailable", "Database operation failed"));
		}
	}

	static UUID id(Map<String, String> pathParameters) {
		return UUID.fromString(pathParameters.get("id"));
	}

	@FunctionalInterface
	interface ThrowingHandler {
		void handle() throws IOException;
	}

	record ErrorResponse(String code, List<String> errors) {
	}
}
