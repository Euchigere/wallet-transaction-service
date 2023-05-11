package com.ontop.wallet.adapters;

import com.ontop.wallet.adapters.jpa.entities.TransferRecord;
import com.ontop.wallet.adapters.jpa.repository.TransferRecordRepository;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.service.TransferRepository;
import com.ontop.wallet.domain.valueobject.Id;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
class TransferRepositoryImpl implements TransferRepository {
    private final static String TRANSFER_NOT_FOUND = "transfer with id=%d not found";

    private final TransferRecordRepository transferRecordRepository;

    @Override
    public Transfer save(Transfer transfer) {
        TransferRecord transferRecord = transferRecordRepository.save(TransferRecord.of(transfer));
        return transferRecord.toDomain();
    }

    @Override
    public Transfer findById(Id<Transfer> transferId) throws TransferNotFoundException {
        TransferRecord transferRecord = transferRecordRepository.findById(transferId.value()).orElseThrow( () -> {
            final String message = String.format(TRANSFER_NOT_FOUND, transferId.value());
            log.error(message);
            return new TransferNotFoundException(message);
        });
        return transferRecord.toDomain();
    }
}
