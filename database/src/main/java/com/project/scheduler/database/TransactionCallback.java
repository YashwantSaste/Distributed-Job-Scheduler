package com.project.scheduler.database;

import java.sql.*;

@FunctionalInterface
public interface TransactionCallback<T> {
  T execute(Connection connection) throws SQLException;
}
