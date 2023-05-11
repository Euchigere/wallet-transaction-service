package com.ontop.wallet.adapters;

import com.ontop.wallet.domain.service.LockService;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;

@Component
class RedisLockServiceImpl implements LockService {
    private final LockRegistry lockRegistry;

    RedisLockServiceImpl(LockRegistry lockRegistry) {
        this.lockRegistry = lockRegistry;
    }

    @Override
    public Lock getLock(String lockKey) {
        return lockRegistry.obtain(lockKey);
    }
}
