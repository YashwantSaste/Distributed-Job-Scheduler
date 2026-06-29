package com.project.scheduler.database;

import java.sql.Connection;
import java.sql.SQLException;

public final class TransactionManager {
	private final ConnectionManager connectionManager;

	public TransactionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public <T> T execute(TransactionCallback<T> cb) {
		try (Connection connection = connectionManager.getConnection()) {
			boolean ac = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				T r = cb.execute(connection);
				connection.commit();
				return r;
			} catch (SQLException | RuntimeException exception) {
				connection.rollback();
				throw exception;
			} finally {
				connection.setAutoCommit(ac);
			}
		} catch (SQLException exception) {
			throw new DatabaseException("Transaction failed", exception);
		}
	}
}
