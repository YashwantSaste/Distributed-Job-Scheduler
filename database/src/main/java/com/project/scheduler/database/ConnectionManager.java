package com.project.scheduler.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionManager extends AutoCloseable {

	Connection getConnection() throws SQLException;

	void close();
}
