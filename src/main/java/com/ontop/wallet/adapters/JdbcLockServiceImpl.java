package com.ontop.wallet.adapters;

import com.ontop.wallet.domain.service.LockService;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;

@Component
class JdbcLockServiceImpl implements LockService {
    private final LockRegistry jdbcLockRegistry;

    JdbcLockServiceImpl(LockRegistry lockRegistry) {
        this.jdbcLockRegistry = lockRegistry;
    }

    @Override
    public Lock getLock(String lockKey) {
        return jdbcLockRegistry.obtain(lockKey);
    }
}
