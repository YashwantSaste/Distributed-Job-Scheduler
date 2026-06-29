package com.project.scheduler.database;

import java.sql.*;

@FunctionalInterface
public interface JdbcRowMapper<T> {
  T map(ResultSet resultSet) throws SQLException;
}
