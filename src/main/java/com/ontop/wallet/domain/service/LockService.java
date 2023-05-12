package com.ontop.wallet.domain.service;

import java.util.concurrent.locks.Lock;

public interface LockService {
    Lock getLock(String lockKey);
}
