# Distributed Job Scheduler

## High-Level Architecture

The high-level architecture provides an overview of the distributed job scheduler and its core components. Client requests are routed through the API Gateway to the Job Service, which manages job creation, updates, and cancellations. Scheduled jobs are persisted in PostgreSQL, while the Scheduler identifies jobs ready for execution and dispatches them through Kafka. Multiple Executor Services consume these jobs, execute them independently, and update the execution status, enabling a scalable, fault-tolerant, and horizontally scalable scheduling system.

<img width="4220" height="2060" alt="image" src="https://github.com/user-attachments/assets/3d68b9c9-2368-4faa-97e3-acdd1fd8db57" />

---

## Low-Level Architecture

The low-level architecture illustrates the internal interactions between services. The Scheduler periodically polls the database for due jobs, acquires distributed locks, and publishes execution events to Kafka. Job Consumers distribute work among Executor Services, which execute jobs and report status updates back to PostgreSQL. Redis is used for distributed locking, job cancellation, heartbeats, and runtime coordination, while retry and dead-letter mechanisms ensure reliable execution even in the presence of failures.

<img width="2800" height="4009" alt="image" src="https://github.com/user-attachments/assets/32a582d6-f80f-4ccd-ba80-b5062d982576" />

---

## Job Execution Flow

This diagram depicts the complete lifecycle of a job. After a job is created and scheduled, the Scheduler detects when it becomes eligible for execution and publishes an event to Kafka. A Job Consumer processes the event and assigns it to an available Executor Service. Upon completion, the executor updates the job status in the database. Failed executions are retried according to the configured retry policy, while permanently failed jobs are moved to the dead-letter queue for further inspection.

<img width="4633" height="4064" alt="image" src="https://github.com/user-attachments/assets/6e4d53a1-0002-4190-a717-b5b80fff1e9d" />


---

## Job State Flow

The state flow defines the lifecycle of every job in the scheduler. A job transitions from **Created** to **Scheduled**, then moves to **Queued** once selected 
<img width="2140" height="2334" alt="image" src="https://github.com/user-attachments/assets/a844e04b-cc1d-4dd1-88cc-4f08fc9652ea" />

by the Scheduler. During execution, it enters the **Running** state before reaching **Succeeded** or **Failed**. Failed jobs may transition to **Retrying** and return to **Running**, while jobs exceeding the maximum retry limit are marked as **Dead Letter**. Jobs can also transition to **Cancelled** at any point before successful completion, ensuring predictable and reliable state management.
