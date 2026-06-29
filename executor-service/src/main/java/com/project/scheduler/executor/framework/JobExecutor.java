package com.project.scheduler.executor.framework;

import com.project.scheduler.common.model.Job;

public interface JobExecutor {

	ExecutionResult execute(Job job) throws Exception;
}
