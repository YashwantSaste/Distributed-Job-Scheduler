# Design a Distributed Job Scheduler

A job scheduler is a system that automatically schedules and executes jobs at a specific time or at regular intervals.

It is used to automate repetitive tasks, run scheduled maintenance, and execute batch processes in near real time.

## Functional Requirements

1. Users can create and schedule jobs for immediate execution or future execution using cron expressions.
2. Users can monitor the status of a job.
3. Users can update or cancel scheduled jobs.
4. *(Nice to have)* Support jobs with dependencies.

## Non-Functional Requirements

- **Scale:** 10,000 jobs per minute
- **CAP Theorem:** Availability > Consistency
- **Execution Guarantee:** At-least-once execution
- **Latency:** Jobs should be executed within 2 seconds of their scheduled time.

## Core Entities

- Job
- Scheduler
- Executor

## API Design

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/jobs` | Create and schedule a job. |
| GET | `/jobs/{jobId}` | Retrieve the metadata of a job. |
| GET | `/jobs/{jobId}/status` | Get the current status of a job (Pending, Running, Failed, etc.). |
| PUT | `/jobs/{jobId}` | Update the job metadata or schedule. |
| POST | `/jobs/{jobId}/cancel` | Cancel a scheduled job. |
| POST | `/jobs/{jobId}/pause` | Pause a scheduled or running job. |
| POST | `/jobs/{jobId}/resume` | Resume a paused job. |
| POST | `/jobs/{jobId}/runNow` | Execute a scheduled job immediately. |

