package com.project.scheduler.kafka;

import java.util.EnumMap;
import java.util.Map;

import com.project.scheduler.common.event.JobEventType;

public final class JobTopics {
	public static final String JOB_CREATE = "job-create", JOB_UPDATE = "job-update", JOB_DELETE = "job-delete",
			JOB_RUN = "job-run", JOB_RETRY = "job-retry", JOB_CANCEL = "job-cancel", JOB_DEAD = "job-dead";
	private static final Map<JobEventType, String> JOB_EVENT_MAP = new EnumMap<>(JobEventType.class);

	static {
		JOB_EVENT_MAP.put(JobEventType.JOB_CREATED, JOB_CREATE);
		JOB_EVENT_MAP.put(JobEventType.JOB_UPDATED, JOB_UPDATE);
		JOB_EVENT_MAP.put(JobEventType.JOB_DELETED, JOB_DELETE);
		JOB_EVENT_MAP.put(JobEventType.JOB_RUN_REQUESTED, JOB_RUN);
		JOB_EVENT_MAP.put(JobEventType.JOB_RETRY_REQUESTED, JOB_RETRY);
		JOB_EVENT_MAP.put(JobEventType.JOB_CANCEL_REQUESTED, JOB_CANCEL);
		JOB_EVENT_MAP.put(JobEventType.JOB_DEAD_LETTERED, JOB_DEAD);
	}

	public static String forType(JobEventType eventType) {
		return JOB_EVENT_MAP.get(eventType);
	}
}
