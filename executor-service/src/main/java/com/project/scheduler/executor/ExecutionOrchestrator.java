package com.project.scheduler.executor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.scheduler.common.event.JobEventType;
import com.project.scheduler.common.event.JobLifecycleEvent;
import com.project.scheduler.common.json.JacksonJsonSerializer;
import com.project.scheduler.common.model.Execution;
import com.project.scheduler.common.model.ExecutionStatus;
import com.project.scheduler.common.model.Job;
import com.project.scheduler.common.model.JobStatus;
import com.project.scheduler.common.model.ScheduleType;
import com.project.scheduler.core.retry.RetryPolicy;
import com.project.scheduler.core.schedule.DefaultScheduleCalculator;
import com.project.scheduler.core.schedule.ScheduleCalculator;
import com.project.scheduler.database.execution.ExecutionRepository;
import com.project.scheduler.database.job.JobRepository;
import com.project.scheduler.executor.framework.ExecutionResult;
import com.project.scheduler.executor.framework.JobExecutorFactory;
import com.project.scheduler.executor.framework.JobPayload;
import com.project.scheduler.kafka.KafkaProducerManager;
import com.project.scheduler.redis.RedisClientWrapper;

public final class ExecutionOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(ExecutionOrchestrator.class);
	private final String executorId;
	private final JobRepository jobRepository;
	private final ExecutionRepository executionRepository;
	private final JobExecutorFactory jobExecutorFactory;
	private final RetryPolicy retryPolicy;
	private final RedisClientWrapper redisClient;
	private final KafkaProducerManager kafkaProducerManager;
	private final ExecutorService executorPool;
	private final ScheduleCalculator scheduleCalculator;
	private final JacksonJsonSerializer json = new JacksonJsonSerializer();
	private final AtomicInteger active = new AtomicInteger();

	public ExecutionOrchestrator(String executorId, JobRepository jobRepository,
			ExecutionRepository executionRepository, JobExecutorFactory jobExecutorFactory, RetryPolicy retryPolicy,
			RedisClientWrapper redisClient, KafkaProducerManager kafkaProducerManager, ExecutorService executorPool) {
		this(executorId, jobRepository, executionRepository, jobExecutorFactory, retryPolicy, redisClient,
				kafkaProducerManager, executorPool, new DefaultScheduleCalculator());
	}

	public ExecutionOrchestrator(String executorId, JobRepository jobRepository,
			ExecutionRepository executionRepository, JobExecutorFactory jobExecutorFactory, RetryPolicy retryPolicy,
			RedisClientWrapper redisClient, KafkaProducerManager kafkaProducerManager, ExecutorService executorPool,
			ScheduleCalculator scheduleCalculator) {
		this.executorId = executorId;
		this.jobRepository = jobRepository;
		this.executionRepository = executionRepository;
		this.jobExecutorFactory = jobExecutorFactory;
		this.retryPolicy = retryPolicy;
		this.redisClient = redisClient;
		this.kafkaProducerManager = kafkaProducerManager;
		this.executorPool = executorPool;
		this.scheduleCalculator = scheduleCalculator;
	}

	public CompletableFuture<Void> executeAsync(UUID jobId, int retryNumber) {
		return CompletableFuture.runAsync(() -> execute(jobId, retryNumber), executorPool);
	}

	public void heartbeat(Duration ttl) {
		redisClient.heartbeat(executorId, ttl);
	}

	public int activeJobs() {
		return active.get();
	}

	void execute(UUID jobId, int retryNumber) {
		log.info("Executing job {} retry {}", jobId, retryNumber);

		Job job = jobRepository.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
		log.info("Loaded job {}", job.id());
		if (redisClient.isCancellationRequested(jobId)) {
			recordCancelled(job, retryNumber);
			return;
		}
		active.incrementAndGet();
		redisClient.markRunning(job.id(), executorId, Duration.ofMinutes(30));
		Execution execution = executionRepository
				.save(Execution.builder().jobId(job.id()).status(ExecutionStatus.RUNNING).startedAt(Instant.now())
						.executorId(executorId).retryNumber(retryNumber).build());
		jobRepository.updateStatus(job.id(), JobStatus.RUNNING);
		try {
			JobPayload jobPayload = json.fromJson(job.payload(), JobPayload.class);
			log.info("Payload type = {}", jobPayload.type());

			ExecutionResult executionResult = jobExecutorFactory.get(jobPayload.type()).execute(job);
			Instant completedAt = Instant.now();
			executionRepository.update(Execution.builder().id(execution.id()).jobId(job.id())
					.status(ExecutionStatus.SUCCEEDED).startedAt(execution.startedAt()).completedAt(completedAt)
					.executorId(executorId).retryNumber(retryNumber).logs(executionResult.logs()).build());
			completeJob(job, completedAt);
			log.info("Execution completed successfully");
		} catch (Exception exception) {
			handleFailure(job, execution, retryNumber, exception);
		} finally {
			redisClient.clearRunning(job.id());
			active.decrementAndGet();
		}
	}

	void completeJob(Job job, Instant completedAt) {
		if (job.scheduleType() == ScheduleType.ONE_TIME) {
			jobRepository.updateStatus(job.id(), JobStatus.COMPLETED);
			return;
		}
		Instant nextExecutionTime = job.nextExecutionTime();
		if (job.scheduleType() == ScheduleType.FIXED_DELAY) {
			Job completed = Job.builder().scheduleType(job.scheduleType()).cronExpression(job.cronExpression())
					.lastExecutionTime(completedAt).build();
			nextExecutionTime = scheduleCalculator.nextExecutionTime(completed, completedAt);
		}
		jobRepository.update(Job.builder().id(job.id()).name(job.name()).description(job.description())
				.payload(job.payload()).scheduleType(job.scheduleType()).cronExpression(job.cronExpression())
				.priority(job.priority()).status(JobStatus.SCHEDULED).retryCount(job.retryCount())
				.maxRetries(job.maxRetries()).nextExecutionTime(nextExecutionTime).lastExecutionTime(completedAt)
				.createdAt(job.createdAt()).updatedAt(completedAt).build());
	}

	void recordCancelled(Job job, int retryNumber) {
		executionRepository.save(Execution.builder().jobId(job.id()).status(ExecutionStatus.CANCELLED)
				.startedAt(Instant.now()).completedAt(Instant.now()).executorId(executorId).retryNumber(retryNumber)
				.logs("Cancellation requested before execution").build());
		jobRepository.updateStatus(job.id(), JobStatus.CANCELLED);
	}

	void handleFailure(Job job, Execution execution, int retryNumber, Exception exception) {
		log.error("Execution failed for job {} retry {}", job.id(), retryNumber, exception);
		executionRepository.update(Execution.builder().id(execution.id()).jobId(job.id()).status(ExecutionStatus.FAILED)
				.startedAt(execution.startedAt()).completedAt(Instant.now()).executorId(executorId)
				.errorMessage(exception.getMessage()).retryNumber(retryNumber).logs(exception.toString()).build());
		if (retryPolicy.shouldRetry(retryNumber, job.maxRetries())) {
			Duration retryBackoff = retryPolicy.backoff(retryNumber);
			kafkaProducerManager.publish(
					JobLifecycleEvent.of(JobEventType.JOB_RETRY_REQUESTED, job.id(), job.name(), Map.of("retryNumber",
							String.valueOf(retryNumber + 1), "backoffMillis", String.valueOf(retryBackoff.toMillis()))))
					.join();
			jobRepository.updateStatus(job.id(), JobStatus.FAILED);
			return;
		}
		executionRepository.save(Execution.builder().jobId(job.id()).status(ExecutionStatus.DEAD_LETTERED)
				.startedAt(Instant.now()).completedAt(Instant.now()).executorId(executorId)
				.errorMessage(exception.getMessage()).retryNumber(retryNumber).logs("Max retries exhausted").build());
		jobRepository.updateStatus(job.id(), JobStatus.DEAD);
		kafkaProducerManager
				.publish(JobLifecycleEvent.of(JobEventType.JOB_DEAD_LETTERED, job.id(), job.name(), Map.of("error",
						exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage())))
				.join();
	}
}
