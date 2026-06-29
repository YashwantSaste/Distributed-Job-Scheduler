CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    schedule_type VARCHAR(32) NOT NULL,
    cron_expression VARCHAR(255),
    priority INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_execution_time TIMESTAMPTZ,
    last_execution_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT jobs_schedule_type_check CHECK (schedule_type IN ('ONE_TIME', 'CRON', 'FIXED_DELAY', 'FIXED_RATE')),
    CONSTRAINT jobs_status_check CHECK (status IN ('CREATED', 'SCHEDULED', 'RUNNING', 'PAUSED', 'CANCELLED', 'COMPLETED', 'FAILED', 'DEAD')),
    CONSTRAINT jobs_retry_count_check CHECK (retry_count >= 0),
    CONSTRAINT jobs_max_retries_check CHECK (max_retries >= 0)
);

CREATE INDEX IF NOT EXISTS idx_jobs_status_next_execution_time
    ON jobs (status, next_execution_time)
    WHERE next_execution_time IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_jobs_priority_next_execution_time
    ON jobs (priority DESC, next_execution_time ASC)
    WHERE next_execution_time IS NOT NULL;

CREATE TABLE IF NOT EXISTS executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    executor_id VARCHAR(128),
    error_message TEXT,
    retry_number INTEGER NOT NULL DEFAULT 0,
    logs TEXT NOT NULL DEFAULT '',
    CONSTRAINT executions_status_check CHECK (status IN ('QUEUED', 'RUNNING', 'CANCELLED', 'SUCCEEDED', 'FAILED', 'DEAD_LETTERED')),
    CONSTRAINT executions_retry_number_check CHECK (retry_number >= 0)
);

CREATE INDEX IF NOT EXISTS idx_executions_job_id_started_at
    ON executions (job_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_executions_status
    ON executions (status);
