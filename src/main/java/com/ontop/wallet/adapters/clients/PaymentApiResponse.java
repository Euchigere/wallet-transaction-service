package com.ontop.wallet.adapters.clients;

import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.PaymentError;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

record PaymentApiResponse(@NonNull RequestInfo requestInfo, @NonNull PaymentInfo paymentInfo) {
    record RequestInfo(String status, String error) {}
    record PaymentInfo(UUID id, BigDecimal amount) {}

    Payment toPayment() {
        return Payment.payment()
                .isCurrent(true)
                .transactionId(new PaymentTransactionId(paymentInfo.id))
                .amount(Money.of(paymentInfo.amount))
                .status(PaymentStatus.valueOf(requestInfo.status.toUpperCase()))
                .error(getPaymentError(requestInfo.error))
                .build();
    }

    private @Nullable PaymentError getPaymentError(@Nullable final String error) {
        if (error == null) {
            return null;
        }
        return error.isBlank() ? null : new PaymentError(error);
    }
}
