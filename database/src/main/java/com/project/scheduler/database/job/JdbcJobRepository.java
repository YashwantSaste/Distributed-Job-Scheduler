package com.project.scheduler.database.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.project.scheduler.common.model.Job;
import com.project.scheduler.common.model.JobStatus;
import com.project.scheduler.common.model.ScheduleType;
import com.project.scheduler.database.ConnectionManager;
import com.project.scheduler.database.DatabaseException;
import com.project.scheduler.database.JdbcStatementBinder;
import com.project.scheduler.database.PreparedStatements;

public final class JdbcJobRepository implements JobRepository {
	private static final String JOB_COLUMNS = "id,name,description,payload::text as payload,schedule_type,cron_expression,priority,status,retry_count,max_retries,next_execution_time,last_execution_time,created_at,updated_at";
	private final ConnectionManager connectionManager;

	public JdbcJobRepository(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public Job save(Job job) {
		String sql = "INSERT INTO jobs(id,name,description,payload,schedule_type,cron_expression,priority,status,retry_count,max_retries,next_execution_time,last_execution_time,created_at,updated_at) VALUES(?,?,?,?::jsonb,?,?,?,?,?,?,?,?,?,?) RETURNING "
				+ JOB_COLUMNS;
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection, sql, ps -> bind(ps, job))) {
			ResultSet resultSet = statement.executeQuery();
			resultSet.next();
			return map(resultSet);
		} catch (SQLException exception) {
			throw new DatabaseException("save job", exception);
		}
	}

	public Optional<Job> findById(UUID id) {
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection,
						"SELECT " + JOB_COLUMNS + " FROM jobs WHERE id=?",
						ps -> PreparedStatements.setUuid(ps, 1, id))) {
			ResultSet resultSet = statement.executeQuery();
			return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
		} catch (SQLException exception) {
			throw new DatabaseException("find job", exception);
		}
	}

	public List<Job> findAll(int limit, int offset) {
		return query("SELECT " + JOB_COLUMNS + " FROM jobs ORDER BY created_at DESC LIMIT ? OFFSET ?", ps -> {
			ps.setInt(1, limit);
			ps.setInt(2, offset);
		});
	}

	public Job update(Job job) {
		String sql = "UPDATE jobs SET name=?,description=?,payload=?::jsonb,schedule_type=?,cron_expression=?,priority=?,status=?,retry_count=?,max_retries=?,next_execution_time=?,last_execution_time=?,updated_at=? WHERE id=? RETURNING "
				+ JOB_COLUMNS;
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection, sql, ps -> {
					ps.setString(1, job.name());
					ps.setString(2, job.description());
					ps.setString(3, job.payload());
					ps.setString(4, job.scheduleType().name());
					ps.setString(5, job.cronExpression());
					ps.setInt(6, job.priority());
					ps.setString(7, job.status().name());
					ps.setInt(8, job.retryCount());
					ps.setInt(9, job.maxRetries());
					PreparedStatements.setInstant(ps, 10, job.nextExecutionTime());
					PreparedStatements.setInstant(ps, 11, job.lastExecutionTime());
					PreparedStatements.setInstant(ps, 12, job.updatedAt());
					PreparedStatements.setUuid(ps, 13, job.id());
				})) {
			ResultSet resultSet = statement.executeQuery();
			if (!resultSet.next())
				throw new SQLException("not found");
			return map(resultSet);
		} catch (SQLException exception) {
			throw new DatabaseException("update job", exception);
		}
	}

	public boolean deleteById(UUID id) {
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection, "DELETE FROM jobs WHERE id=?",
						ps -> PreparedStatements.setUuid(ps, 1, id))) {
			return statement.executeUpdate() > 0;
		} catch (SQLException exception) {
			throw new DatabaseException("delete job", exception);
		}
	}

	public boolean updateStatus(UUID id, JobStatus status) {
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection,
						"UPDATE jobs SET status=?,updated_at=? WHERE id=?", ps -> {
							ps.setString(1, status.name());
							PreparedStatements.setInstant(ps, 2, Instant.now());
							PreparedStatements.setUuid(ps, 3, id);
						})) {
			return statement.executeUpdate() > 0;
		} catch (SQLException exception) {
			throw new DatabaseException("status", exception);
		}
	}

	public List<Job> findDueJobs(Instant now, int limit) {
		return query("SELECT " + JOB_COLUMNS
				+ " FROM jobs WHERE status='SCHEDULED' AND next_execution_time <= ? ORDER BY priority DESC,next_execution_time ASC LIMIT ?",
				ps -> {
					PreparedStatements.setInstant(ps, 1, now);
					ps.setInt(2, limit);
				});
	}

	List<Job> query(String sql, JdbcStatementBinder statementBinder) {
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection, sql, statementBinder)) {
			List<Job> jobs = new ArrayList<>();
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next())
				jobs.add(map(resultSet));
			return jobs;
		} catch (SQLException exception) {
			throw new DatabaseException("query jobs", exception);
		}
	}

	void bind(PreparedStatement statement, Job job) throws SQLException {
		PreparedStatements.setUuid(statement, 1, job.id());
		statement.setString(2, job.name());
		statement.setString(3, job.description());
		statement.setString(4, job.payload());
		statement.setString(5, job.scheduleType().name());
		statement.setString(6, job.cronExpression());
		statement.setInt(7, job.priority());
		statement.setString(8, job.status().name());
		statement.setInt(9, job.retryCount());
		statement.setInt(10, job.maxRetries());
		PreparedStatements.setInstant(statement, 11, job.nextExecutionTime());
		PreparedStatements.setInstant(statement, 12, job.lastExecutionTime());
		PreparedStatements.setInstant(statement, 13, job.createdAt());
		PreparedStatements.setInstant(statement, 14, job.updatedAt());
	}

	Job map(ResultSet resultSet) throws SQLException {
		return Job.builder().id(resultSet.getObject("id", UUID.class)).name(resultSet.getString("name"))
				.description(resultSet.getString("description")).payload(resultSet.getString("payload"))
				.scheduleType(ScheduleType.valueOf(resultSet.getString("schedule_type")))
				.cronExpression(resultSet.getString("cron_expression")).priority(resultSet.getInt("priority"))
				.status(JobStatus.valueOf(resultSet.getString("status"))).retryCount(resultSet.getInt("retry_count"))
				.maxRetries(resultSet.getInt("max_retries"))
				.nextExecutionTime(inst(resultSet.getTimestamp("next_execution_time")))
				.lastExecutionTime(inst(resultSet.getTimestamp("last_execution_time")))
				.createdAt(inst(resultSet.getTimestamp("created_at")))
				.updatedAt(inst(resultSet.getTimestamp("updated_at"))).build();
	}

	Instant inst(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}
}
