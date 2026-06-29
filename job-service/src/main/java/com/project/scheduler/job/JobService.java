package com.project.scheduler.job;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.common.model.Execution;
import com.project.scheduler.common.model.Job;
import com.project.scheduler.common.model.JobStatus;
import com.project.scheduler.common.model.ScheduleType;
import com.project.scheduler.common.validation.ValidationException;
import com.project.scheduler.common.validation.ValidationResult;
import com.project.scheduler.core.schedule.DefaultScheduleCalculator;
import com.project.scheduler.core.schedule.ScheduleCalculator;
import com.project.scheduler.database.execution.ExecutionRepository;
import com.project.scheduler.database.job.JobRepository;
import com.project.scheduler.job.command.CreateJobCommand;
import com.project.scheduler.job.command.UpdateJobCommand;

public final class JobService {
	private static final Logger log = LoggerFactory.getLogger(JobService.class);
	private final JobRepository jobRepository;
	private final ExecutionRepository executionRepository;
	private final JobValidator jobValidator;
	private final JobEventPublisher eventPublisher;
	private final ScheduleCalculator scheduleCalculator;

	public JobService(JobRepository jobRepository, ExecutionRepository executionRepository, JobValidator jobValidator) {
		this(jobRepository, executionRepository, jobValidator, JobEventPublisher.defaultPublisher(),
				new DefaultScheduleCalculator());
	}

	public JobService(JobRepository jobRepository, ExecutionRepository executionRepository, JobValidator jobValidator,
			JobEventPublisher eventPublisher) {
		this(jobRepository, executionRepository, jobValidator, eventPublisher, new DefaultScheduleCalculator());
	}

	public JobService(JobRepository jobRepository, ExecutionRepository executionRepository, JobValidator jobValidator,
			JobEventPublisher eventPublisher, ScheduleCalculator scheduleCalculator) {
		this.jobRepository = jobRepository;
		this.executionRepository = executionRepository;
		this.jobValidator = jobValidator;
		this.eventPublisher = eventPublisher;
		this.scheduleCalculator = scheduleCalculator;
	}

	public Job create(CreateJobCommand command) {
		validate(jobValidator.validate(command));
		Instant now = Instant.now();
		ScheduleType scheduleType = command.scheduleType();
		String cronExpression = nullIfBlank(command.cronExpression());
		Instant nextExecutionTime = resolveNextExecutionTime(scheduleType, cronExpression, command.nextExecutionTime(),
				now);
		Job job = Job.builder().name(command.name().trim()).description(defaultIfBlank(command.description(), ""))
				.payload(defaultIfBlank(command.payload(), "{}")).scheduleType(scheduleType).cronExpression(cronExpression)
				.priority(command.priority()).status(JobStatus.SCHEDULED)
				.maxRetries(command.maxRetries() == null ? 3 : command.maxRetries()).nextExecutionTime(nextExecutionTime)
				.createdAt(now).updatedAt(now).build();
		Job savedJob = jobRepository.save(job);
		eventPublisher.jobCreated(savedJob);
		return savedJob;
	}

	public Job get(UUID id) {
		return jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
	}

	public List<Job> list(int limit, int offset) {
		return jobRepository.findAll(limit, offset);
	}

	public Job update(UUID id, UpdateJobCommand command) {
		validate(jobValidator.validateUpdate(command));
		Job existingJob = get(id);
		ScheduleType scheduleType = command.scheduleType();
		String cronExpression = nullIfBlank(command.cronExpression());
		Instant now = Instant.now();
		Instant nextExecutionTime = command.nextExecutionTime() != null ? command.nextExecutionTime()
				: resolveNextExecutionTime(scheduleType, cronExpression, existingJob.nextExecutionTime(), now);
		Job updatedJob = Job.builder().id(existingJob.id()).name(command.name().trim())
				.description(defaultIfBlank(command.description(), "")).payload(defaultIfBlank(command.payload(), "{}"))
				.scheduleType(scheduleType).cronExpression(cronExpression)
				.priority(command.priority() == null ? existingJob.priority() : command.priority())
				.status(existingJob.status()).retryCount(existingJob.retryCount())
				.maxRetries(command.maxRetries() == null ? existingJob.maxRetries() : command.maxRetries())
				.nextExecutionTime(nextExecutionTime).lastExecutionTime(existingJob.lastExecutionTime())
				.createdAt(existingJob.createdAt()).updatedAt(now).build();
		Job savedJob = jobRepository.update(updatedJob);
		eventPublisher.jobUpdated(savedJob);
		return savedJob;
	}

	public void delete(UUID id) {
		if (!jobRepository.deleteById(id)) {
			throw new JobNotFoundException(id);
		}
		eventPublisher.jobDeleted(id);
	}

	public Job pause(UUID id) {
		return transition(id, JobStatus.PAUSED);
	}

	public Job resume(UUID id) {
		return transition(id, JobStatus.SCHEDULED);
	}

	public Job cancel(UUID id) {
		Job job = transition(id, JobStatus.CANCELLED);
		eventPublisher.jobCancelRequested(job);
		return job;
	}

	public Job trigger(UUID id) {
		Job job = get(id);
		eventPublisher.jobRunRequested(job);
		return job;
	}

	public Job retry(UUID id) {
		Job job = get(id);
		eventPublisher.jobRetryRequested(job);
		return job;
	}

	public List<Execution> history(UUID id, int limit, int offset) {
		get(id);
		return executionRepository.findByJobId(id, limit, offset);
	}

	Job transition(UUID id, JobStatus status) {
		Job existingJob = get(id);
		Job updatedJob = Job.builder().id(existingJob.id()).name(existingJob.name())
				.description(existingJob.description()).payload(existingJob.payload())
				.scheduleType(existingJob.scheduleType()).cronExpression(existingJob.cronExpression())
				.priority(existingJob.priority()).status(status).retryCount(existingJob.retryCount())
				.maxRetries(existingJob.maxRetries()).nextExecutionTime(existingJob.nextExecutionTime())
				.lastExecutionTime(existingJob.lastExecutionTime()).createdAt(existingJob.createdAt())
				.updatedAt(Instant.now()).build();
		Job savedJob = jobRepository.update(updatedJob);
		log.info("Job {} transitioned -> {}", id, status);
		return savedJob;
	}

	static void validate(ValidationResult validationResult) {
		if (!validationResult.valid()) {
			throw new ValidationException(validationResult.errors());
		}
	}

	static String defaultIfBlank(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}

	static String nullIfBlank(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	Instant resolveNextExecutionTime(ScheduleType scheduleType, String cronExpression, Instant provided,
			Instant now) {
		if (scheduleType == ScheduleType.ONE_TIME) {
			return provided;
		}
		if (provided != null) {
			return provided;
		}
		Job draft = Job.builder().scheduleType(scheduleType).cronExpression(cronExpression).build();
		return scheduleCalculator.nextExecutionTime(draft, now);
	}
}
