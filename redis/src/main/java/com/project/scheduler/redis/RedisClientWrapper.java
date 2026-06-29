package com.project.scheduler.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

public final class RedisClientWrapper implements AutoCloseable {

	private final JedisPool pool;

	public RedisClientWrapper(String host, int port, String password) {
		pool = (password == null || password.isBlank()) ? new JedisPool(host, port)
				: new JedisPool(host, port, null, password);
	}

	public Optional<String> get(String key) {
		try (Jedis jedis = pool.getResource()) {
			return Optional.ofNullable(jedis.get(key));
		}
	}

	public void set(String key, String value, Duration timeToLive) {
		try (Jedis jedis = pool.getResource()) {
			jedis.set(key, value, SetParams.setParams().ex(timeToLive.toSeconds()));
		}
	}

	public boolean setIfAbsent(String key, String value, Duration timeToLive) {
		try (Jedis jedis = pool.getResource()) {
			return "OK".equals(jedis.set(key, value, SetParams.setParams().nx().ex(timeToLive.toSeconds())));
		}
	}

	public void delete(String key) {
		try (Jedis jedis = pool.getResource()) {
			jedis.del(key);
		}
	}

	public boolean acquireJobLock(UUID jobId, String owner, Duration timeToLive) {
		return setIfAbsent("lock:job:" + jobId, owner, timeToLive);
	}

	public void releaseJobLock(UUID jobId, String owner) {
		String lockKey = "lock:job:" + jobId;
		try (Jedis jedis = pool.getResource()) {
			if (owner.equals(jedis.get(lockKey))) {
				jedis.del(lockKey);
			}
		}
	}

	public void markRunning(UUID jobId, String executorId, Duration timeToLive) {
		set("job:" + jobId, executorId, timeToLive);
	}

	public void clearRunning(UUID jobId) {
		delete("job:" + jobId);
	}

	public void requestCancellation(UUID jobId, Duration timeToLive) {
		set("cancel:" + jobId, Instant.now().toString(), timeToLive);
	}

	public boolean isCancellationRequested(UUID jobId) {
		return get("cancel:" + jobId).isPresent();
	}

	public void heartbeat(String executorId, Duration timeToLive) {
		set("executor:" + executorId, Instant.now().toString(), timeToLive);
	}

	public void close() {
		pool.close();
	}
}
