package com.project.scheduler.kafka;

import com.project.scheduler.common.event.JobLifecycleEvent;

@FunctionalInterface
public interface JobEventHandler {

	void handle(JobLifecycleEvent event);
}
