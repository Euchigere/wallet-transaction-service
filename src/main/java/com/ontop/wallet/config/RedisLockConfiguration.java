package com.ontop.wallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.ExpirableLockRegistry;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisLockConfiguration {
    private static final String LOCK_NAME = "transaction_lock";
    @Value("${redis.lock.release-time-seconds}")
    private int releaseTimeSeconds;

    @Bean
    public ExpirableLockRegistry lockRegistry(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockRegistry(redisConnectionFactory, LOCK_NAME, Duration.ofSeconds(releaseTimeSeconds).toMillis());
    }
}
