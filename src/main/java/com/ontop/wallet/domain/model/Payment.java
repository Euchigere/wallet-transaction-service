package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.PaymentError;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;

import java.time.Instant;

@Getter
public class Payment extends BaseModel<Payment> {
    private final PaymentTransactionId transactionId;
    private final PaymentStatus status;
    private final Money amount;

    private final PaymentError error;

    private Boolean isCurrent;

    @Builder(builderMethodName = "payment")
    private Payment(
            Id<Payment> id,
            Instant created,
            Instant updated,
            PaymentError error,
            @NonNull Money amount,
            @NonNull PaymentTransactionId transactionId,
            @NonNull PaymentStatus status,
            @NonNull Boolean isCurrent
    ) {
        super(id, created, updated);
        this.amount = amount;
        this.error = error;
        this.transactionId = transactionId;
        this.status = status;
        this.isCurrent = isCurrent;
        validateSelf();
    }

    private boolean isNewPayment() {
        return this.id() == null;
    }

    private void validateSelf() {
        if (isNewPayment()) {
            Assert.isTrue(isCurrent, "expected new payment to be current");
        }
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(this.status);
    }

    public boolean isRetryable() {
        return isFailed() && error != null && error.isTimeout();
    }

    void notCurrent() {
        validateSelf();
        this.isCurrent = false;
    }
}
