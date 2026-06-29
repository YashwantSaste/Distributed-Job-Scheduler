package com.project.scheduler.redis;

import java.time.Duration;
import java.util.UUID;

import com.project.scheduler.kafka.IdempotencyStore;

public final class RedisIdempotencyStore implements IdempotencyStore {

	private final RedisClientWrapper redis;

	public RedisIdempotencyStore(RedisClientWrapper redisClient) {
		redis = redisClient;
	}

	public boolean markIfNew(UUID id, Duration ttl) {
		return redis.setIfAbsent("event:" + id, "processed", ttl);
	}
}
