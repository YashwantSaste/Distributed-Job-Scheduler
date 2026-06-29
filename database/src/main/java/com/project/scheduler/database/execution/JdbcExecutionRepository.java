package com.project.scheduler.database.execution;

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

import com.project.scheduler.common.model.Execution;
import com.project.scheduler.common.model.ExecutionStatus;
import com.project.scheduler.database.ConnectionManager;
import com.project.scheduler.database.DatabaseException;
import com.project.scheduler.database.JdbcStatementBinder;
import com.project.scheduler.database.PreparedStatements;

public final class JdbcExecutionRepository implements ExecutionRepository {
	private static final String EXECUTION_COLUMNS = "id,job_id,status,started_at,completed_at,executor_id,error_message,retry_number,logs";
	private final ConnectionManager connectionManager;

	public JdbcExecutionRepository(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public Execution save(Execution execution) {
		String sql = "INSERT INTO executions(id,job_id,status,started_at,completed_at,executor_id,error_message,retry_number,logs) VALUES(?,?,?,?,?,?,?,?,?) RETURNING "
				+ EXECUTION_COLUMNS;
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection, sql, ps -> bind(ps, execution))) {
			ResultSet resultSet = statement.executeQuery();
			resultSet.next();
			return map(resultSet);
		} catch (SQLException exception) {
			throw new DatabaseException("save execution", exception);
		}
	}

	public Optional<Execution> findById(UUID id) {
		List<Execution> executions = query("SELECT " + EXECUTION_COLUMNS + " FROM executions WHERE id=?",
				ps -> PreparedStatements.setUuid(ps, 1, id));
		return executions.stream().findFirst();
	}

	public List<Execution> findAll(int limit, int offset) {
		return query(
				"SELECT " + EXECUTION_COLUMNS + " FROM executions ORDER BY started_at DESC NULLS LAST LIMIT ? OFFSET ?",
				ps -> {
					ps.setInt(1, limit);
					ps.setInt(2, offset);
				});
	}

	public List<Execution> findByJobId(UUID id, int limit, int offset) {
		return query(
				"SELECT " + EXECUTION_COLUMNS
						+ " FROM executions WHERE job_id=? ORDER BY started_at DESC NULLS LAST LIMIT ? OFFSET ?",
				ps -> {
					PreparedStatements.setUuid(ps, 1, id);
					ps.setInt(2, limit);
					ps.setInt(3, offset);
				});
	}

	public Execution update(Execution execution) {
		String sql = "UPDATE executions SET status=?,started_at=?,completed_at=?,executor_id=?,error_message=?,retry_number=?,logs=? WHERE id=? RETURNING "
				+ EXECUTION_COLUMNS;
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection, sql, preparedStatement -> {
					preparedStatement.setString(1, execution.status().name());
					PreparedStatements.setInstant(preparedStatement, 2, execution.startedAt());
					PreparedStatements.setInstant(preparedStatement, 3, execution.completedAt());
					preparedStatement.setString(4, execution.executorId());
					preparedStatement.setString(5, execution.errorMessage());
					preparedStatement.setInt(6, execution.retryNumber());
					preparedStatement.setString(7, execution.logs());
					PreparedStatements.setUuid(preparedStatement, 8, execution.id());
				})) {
			ResultSet resultSet = statement.executeQuery();
			resultSet.next();
			return map(resultSet);
		} catch (SQLException exception) {
			throw new DatabaseException("update execution", exception);
		}
	}

	public boolean deleteById(UUID id) {
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection,
						"DELETE FROM executions WHERE id=?", ps -> PreparedStatements.setUuid(ps, 1, id))) {
			return statement.executeUpdate() > 0;
		} catch (SQLException exception) {
			throw new DatabaseException("delete execution", exception);
		}
	}

	List<Execution> query(String sql, JdbcStatementBinder statementBinder) {
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement statement = PreparedStatements.prepare(connection, sql, statementBinder)) {
			List<Execution> executions = new ArrayList<>();
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next())
				executions.add(map(resultSet));
			return executions;
		} catch (SQLException exception) {
			throw new DatabaseException("query execution", exception);
		}
	}

	void bind(PreparedStatement statement, Execution execution) throws SQLException {
		PreparedStatements.setUuid(statement, 1, execution.id());
		PreparedStatements.setUuid(statement, 2, execution.jobId());
		statement.setString(3, execution.status().name());
		PreparedStatements.setInstant(statement, 4, execution.startedAt());
		PreparedStatements.setInstant(statement, 5, execution.completedAt());
		statement.setString(6, execution.executorId());
		statement.setString(7, execution.errorMessage());
		statement.setInt(8, execution.retryNumber());
		statement.setString(9, execution.logs());
	}

	Execution map(ResultSet resultSet) throws SQLException {
		return Execution.builder().id(resultSet.getObject("id", UUID.class))
				.jobId(resultSet.getObject("job_id", UUID.class))
				.status(ExecutionStatus.valueOf(resultSet.getString("status")))
				.startedAt(inst(resultSet.getTimestamp("started_at")))
				.completedAt(inst(resultSet.getTimestamp("completed_at")))
				.executorId(resultSet.getString("executor_id")).errorMessage(resultSet.getString("error_message"))
				.retryNumber(resultSet.getInt("retry_number")).logs(resultSet.getString("logs")).build();
	}

	Instant inst(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}
}
