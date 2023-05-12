package com.ontop.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;

import javax.sql.DataSource;

@Configuration
public class JdbcLockConfiguration {
    @Value("${jdbc.lock.release-time-seconds}")
    private int releaseTimeSeconds;

    @Bean
    public LockRepository lockRepository(DataSource datasource) {
        final DefaultLockRepository defaultLockRepository = new DefaultLockRepository(datasource);
        defaultLockRepository.setTimeToLive(releaseTimeSeconds);
        return defaultLockRepository;
    }

    @Bean
    public LockRegistry jdbcLockRegistry(LockRepository repository) {
        return new JdbcLockRegistry(repository);
    }
}
