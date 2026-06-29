package com.project.scheduler.core.exception;

public class SchedulerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SchedulerException(String message) {
		super(message);
	}

	public SchedulerException(String message, Throwable cause) {
		super(message, cause);
	}
}
