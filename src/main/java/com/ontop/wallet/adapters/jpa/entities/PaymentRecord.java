package com.ontop.wallet.adapters.jpa.entities;

import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.PaymentError;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Getter
@Setter
@ToString
public class PaymentRecord extends BaseEntity {
    @Column(nullable = false, unique = true)
    private UUID transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false)
    private boolean isCurrent;

    private String error;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", referencedColumnName = "id")
    private TransferRecord transfer;

    Payment toDomain() {
        return Payment.payment()
                .id(new Id<>(this.id))
                .created(this.created)
                .updated(this.updated)
                .error(this.error == null ? null : new PaymentError(this.error))
                .amount(Money.of(this.amount))
                .transactionId(new PaymentTransactionId(this.transactionId))
                .status(this.status)
                .isCurrent(this.isCurrent)
                .build();
    }

    static PaymentRecord of(@NonNull final Payment payment) {
        final PaymentRecord paymentRecord = new PaymentRecord();
        if (payment.id() != null) {
            paymentRecord.id(payment.id().value());
        }
        paymentRecord.created(payment.created());
        paymentRecord.updated(payment.updated());
        paymentRecord.error(payment.error() == null ? null : payment.error().value());
        paymentRecord.amount(payment.amount().value());
        paymentRecord.transactionId(payment.transactionId().value());
        paymentRecord.status(payment.status());
        paymentRecord.isCurrent(payment.isCurrent());
        return paymentRecord;
    }
}
