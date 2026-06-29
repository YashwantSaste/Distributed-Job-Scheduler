package com.project.scheduler.executor.framework;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.project.scheduler.common.json.JacksonJsonSerializer;
import com.project.scheduler.common.model.Job;

public final class ShellExecutor implements JobExecutor {
    private final JacksonJsonSerializer json = new JacksonJsonSerializer();

    public ExecutionResult execute(Job job) throws Exception {
	JobPayload jobPayload = json.fromJson(job.payload(), JobPayload.class);

	if (jobPayload.command() == null || jobPayload.command().isBlank()) {
	    throw new IllegalArgumentException("Shell executor requires payload.command");
	}

	Process pr = new ProcessBuilder("sh", "-c", jobPayload.command()).redirectErrorStream(true).start();

	if (!pr.waitFor(Duration.ofMinutes(10).toSeconds(), TimeUnit.SECONDS)) {
	    pr.destroyForcibly();
	    throw new IllegalStateException("Shell command timed out");
	}

	String logs = new String(pr.getInputStream().readAllBytes());

	if (pr.exitValue() == 0) {
	    return ExecutionResult.success(logs);
	}

	throw new IllegalStateException("Shell command failed with exit " + pr.exitValue() + ": " + logs);
    }
}
