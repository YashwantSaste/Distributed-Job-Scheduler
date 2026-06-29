package com.project.scheduler.executor.framework;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.project.scheduler.common.json.JacksonJsonSerializer;
import com.project.scheduler.common.model.Job;

public class HttpExecutor implements JobExecutor {
	private final JacksonJsonSerializer json = new JacksonJsonSerializer();
	private final HttpClient client = HttpClient.newHttpClient();

	public ExecutionResult execute(Job job) throws Exception {
		JobPayload jobPayload = json.fromJson(job.payload(), JobPayload.class);
		if (jobPayload.url() == null || jobPayload.url().isBlank()) {
			throw new IllegalArgumentException("HTTP executor requires payload.url");
		}

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(jobPayload.url()));
		if (jobPayload.headers() != null) {
			jobPayload.headers().forEach(requestBuilder::header);
		}

		String httpMethod = jobPayload.method() == null || jobPayload.method().isBlank() ? "POST"
				: jobPayload.method().toUpperCase();
		HttpResponse<String> response = client.send(
				requestBuilder.method(httpMethod,
						jobPayload.body() == null ? HttpRequest.BodyPublishers.noBody()
								: HttpRequest.BodyPublishers.ofString(jobPayload.body()))
						.build(),
				HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 200 && response.statusCode() < 300) {
			return ExecutionResult.success("HTTP " + response.statusCode() + " " + response.body());
		}
		throw new IllegalStateException(
				"HTTP executor failed with status " + response.statusCode() + ": " + response.body());
	}
}
