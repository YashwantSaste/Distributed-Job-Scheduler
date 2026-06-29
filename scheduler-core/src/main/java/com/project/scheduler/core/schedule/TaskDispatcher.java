package com.project.scheduler.core.schedule;

import com.project.scheduler.common.model.Job;

@FunctionalInterface
public interface TaskDispatcher {

	void dispatch(Job job);
}
