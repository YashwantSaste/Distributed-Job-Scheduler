package com.project.scheduler.database;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class PreparedStatements {
  private PreparedStatements() {}

  public static PreparedStatement prepare(
      Connection connection, String sql, JdbcStatementBinder statementBinder) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql);
    statementBinder.bind(statement);
    return statement;
  }

  public static void setUuid(PreparedStatement statement, int parameterIndex, UUID value)
      throws SQLException {
    statement.setObject(parameterIndex, value);
  }

  public static void setInstant(PreparedStatement statement, int parameterIndex, Instant value)
      throws SQLException {
    statement.setTimestamp(parameterIndex, value == null ? null : Timestamp.from(value));
  }
}
