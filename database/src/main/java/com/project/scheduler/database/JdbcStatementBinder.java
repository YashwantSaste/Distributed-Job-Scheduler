package com.project.scheduler.database;

import java.sql.*;

@FunctionalInterface
public interface JdbcStatementBinder {
  void bind(PreparedStatement statement) throws SQLException;
}
