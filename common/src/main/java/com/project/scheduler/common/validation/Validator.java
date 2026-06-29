package com.project.scheduler.common.validation;

public interface Validator<T> {

	ValidationResult validate(T value);
}
