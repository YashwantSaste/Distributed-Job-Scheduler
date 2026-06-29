package com.project.scheduler.common.validation;

import java.util.List;

public final class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final List<String> errors;

	public ValidationException(List<String> errors) {
		super(String.join(", ", errors));
		this.errors = List.copyOf(errors);
	}

	public List<String> errors() {
		return errors;
	}
}
