package com.ontop.wallet.adapters.jpa.repository;

import com.ontop.wallet.adapters.jpa.entities.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {
}
