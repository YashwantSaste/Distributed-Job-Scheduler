package com.project.scheduler.database.job;

import com.project.scheduler.common.model.*;
import com.project.scheduler.database.Repository;
import java.time.*;
import java.util.*;

public interface JobRepository extends Repository<UUID, Job> {
  boolean updateStatus(UUID id, JobStatus status);

  List<Job> findDueJobs(Instant now, int limit);
}
