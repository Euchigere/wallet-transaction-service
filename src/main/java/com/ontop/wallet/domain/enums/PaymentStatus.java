package com.ontop.wallet.domain.enums;

public enum PaymentStatus {
    PROCESSING, FAILED, UNKNOWN;

    public TransferStatus toTransferStatus() {
        return TransferStatus.valueOf(toString());
    }
}
