package com.ontop.wallet.adapters.jpa.repository;

import com.ontop.wallet.adapters.jpa.entities.AccountRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRecordRepository extends JpaRepository<AccountRecord, Long> {
    Optional<AccountRecord> findByUserId(Long userId);
}
