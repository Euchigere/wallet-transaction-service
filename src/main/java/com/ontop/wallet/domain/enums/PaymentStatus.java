package com.ontop.wallet.domain.enums;

public enum PaymentStatus {
    PROCESSING, FAILED;

    public TransferStatus toTransferStatus() {
        return TransferStatus.valueOf(toString());
    }
}
