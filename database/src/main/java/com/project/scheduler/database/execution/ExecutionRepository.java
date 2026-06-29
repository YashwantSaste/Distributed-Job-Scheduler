package com.project.scheduler.database.execution;

import java.util.List;
import java.util.UUID;

import com.project.scheduler.common.model.Execution;
import com.project.scheduler.database.Repository;

public interface ExecutionRepository extends Repository<UUID, Execution> {
    List<Execution> findByJobId(UUID jobId, int limit, int offset);
}
