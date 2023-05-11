package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;

public interface TransferRepository {
    Transfer save(Transfer transfer);

    Transfer findById(Id<Transfer> transferId) throws TransferNotFoundException;
}
