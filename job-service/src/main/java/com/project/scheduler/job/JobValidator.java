package com.project.scheduler.job;

import java.util.ArrayList;
import java.util.List;

import com.project.scheduler.common.model.ScheduleType;
import com.project.scheduler.common.validation.ValidationResult;
import com.project.scheduler.common.validation.Validator;
import com.project.scheduler.job.command.CreateJobCommand;
import com.project.scheduler.job.command.UpdateJobCommand;

public final class JobValidator implements Validator<CreateJobCommand> {

	public ValidationResult validate(CreateJobCommand command) {
		List<String> errors = new ArrayList<>();
		name(command.name(), errors);
		schedule(command.scheduleType(), command.cronExpression(), command.nextExecutionTime(), errors);
		if (command.maxRetries() != null && command.maxRetries() < 0)
			errors.add("maxRetries must be >= 0");
		return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.invalid(errors);
	}

	public ValidationResult validateUpdate(UpdateJobCommand command) {
		List<String> errors = new ArrayList<>();
		name(command.name(), errors);
		schedule(command.scheduleType(), command.cronExpression(), command.nextExecutionTime(), errors);
		if (command.maxRetries() != null && command.maxRetries() < 0)
			errors.add("maxRetries must be >= 0");
		return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.invalid(errors);
	}

	void name(String jobName, List<String> errors) {
		if (jobName == null || jobName.isBlank())
			errors.add("name is required");
		else if (jobName.length() > 255)
			errors.add("name must be <= 255 characters");
	}

	void schedule(ScheduleType scheduleType, String cronExpression, java.time.Instant nextExecutionTime,
			List<String> errors) {
		if (scheduleType == null) {
			errors.add("scheduleType is required");
			return;
		}
		if (scheduleType == ScheduleType.CRON && (cronExpression == null || cronExpression.isBlank()))
			errors.add("cronExpression is required for CRON jobs");
		if ((scheduleType == ScheduleType.FIXED_DELAY || scheduleType == ScheduleType.FIXED_RATE)
				&& (cronExpression == null || cronExpression.isBlank()))
			errors.add("cronExpression is required for FIXED_DELAY and FIXED_RATE jobs (interval)");
		if (scheduleType == ScheduleType.ONE_TIME && nextExecutionTime == null)
			errors.add("nextExecutionTime is required for ONE_TIME jobs");
	}
}
