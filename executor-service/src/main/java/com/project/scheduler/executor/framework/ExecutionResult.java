package com.project.scheduler.executor.framework;

public record ExecutionResult(boolean success, String logs) {

	public static ExecutionResult success(String logs) {
		return new ExecutionResult(true, logs);
	}
}
