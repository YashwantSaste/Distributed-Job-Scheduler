package com.project.scheduler.database;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnectionPool implements ConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);
    private final String jdbcUrl;
    private final Properties props = new Properties();
    private final int max;
    private final long connectionTimeout;
    private final Queue<Connection> idle = new ArrayDeque<>();
    private final Set<Connection> all = new HashSet<>();
    private boolean closed;

    public ConnectionPool(String jdbcUrl, String databaseUser, String databasePassword, int maxConnections,
	    Duration connectionTimeout) {
	this.jdbcUrl = jdbcUrl;
	props.setProperty("user", databaseUser);
	props.setProperty("password", databasePassword);
	this.max = Math.max(1, maxConnections);
	this.connectionTimeout = Math.max(1, connectionTimeout.toMillis());
    }

    public synchronized Connection getConnection() throws SQLException {
	long end = System.currentTimeMillis() + connectionTimeout;
	while (true) {

	    if (closed) {
		throw new SQLException("closed");
	    }

	    Connection connection = poll();

	    if (connection != null) {
		return proxy(connection);
	    }

	    if (all.size() < max) {
		Connection newConnection = DriverManager.getConnection(jdbcUrl, props);
		all.add(newConnection);
		log.info("Opened JDBC connection {}/{}", all.size(), max);
		return proxy(newConnection);
	    }

	    long waitMillis = end - System.currentTimeMillis();

	    if (waitMillis <= 0) {
		throw new SQLException("Timed out waiting for JDBC connection");
	    }

	    try {
		wait(waitMillis);
	    } catch (InterruptedException exception) {
		Thread.currentThread().interrupt();
		throw new SQLException(exception);
	    }
	}
    }

    private Connection poll() throws SQLException {
	while (!idle.isEmpty()) {

	    Connection idleConnection = idle.poll();

	    if (!idleConnection.isClosed() && idleConnection.isValid(2)) {
		return idleConnection;
	    }

	    all.remove(idleConnection);
	}
	return null;
    }

    private Connection proxy(Connection connection) {
	return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class },
		new PooledConnectionInvocationHandler(this, connection));
    }

    synchronized void release(Connection connection) throws SQLException {
	if (closed || connection.isClosed())
	    all.remove(connection);
	else {

	    if (!connection.getAutoCommit()) {
		connection.setAutoCommit(true);
	    }

	    idle.offer(connection);
	}
	notifyAll();
    }

    public synchronized void close() {
	closed = true;
	for (Connection connection : all)
	    try {
		connection.close();
	    } catch (SQLException exception) {
		log.warn("close failed", exception);
	    }
	idle.clear();
	all.clear();
	notifyAll();
    }

    static final class PooledConnectionInvocationHandler implements InvocationHandler {

	final ConnectionPool connectionPool;
	final Connection connection;
	boolean released;

	PooledConnectionInvocationHandler(ConnectionPool connectionPool, Connection connection) {
	    this.connectionPool = connectionPool;
	    this.connection = connection;
	}

	public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
	    if (method.getName().equals("close")) {
		if (!released) {
		    released = true;
		    connectionPool.release(connection);
		}
		return null;
	    }
	    if (method.getName().equals("isClosed"))
		return released || connection.isClosed();
	    if (released)
		throw new SQLException("Connection returned to pool");
	    return method.invoke(connection, arguments);
	}
    }
}
