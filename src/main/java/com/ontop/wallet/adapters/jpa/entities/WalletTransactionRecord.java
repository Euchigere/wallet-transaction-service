package com.ontop.wallet.adapters.jpa.entities;

import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletTransactionId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_transaction")
@Setter
@ToString
public class WalletTransactionRecord extends BaseEntity {
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long walletTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionOperation operation;

    @Column(nullable = false)
    private BigDecimal amount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", referencedColumnName = "id")
    private TransferRecord transfer;

    WalletTransaction toDomain() {
        return WalletTransaction.walletTransaction()
                .id(new Id<>(this.id))
                .created(this.created)
                .updated(this.updated)
                .userId(new UserId(this.userId))
                .amount(Money.of(this.amount))
                .walletTransactionId(new WalletTransactionId(this.walletTransactionId))
                .operation(this.operation)
                .build();
    }

    static WalletTransactionRecord of(@NonNull final WalletTransaction walletTransaction) {
        final WalletTransactionRecord record = new WalletTransactionRecord();
        if (walletTransaction.id() != null) {
            record.id(walletTransaction.id().value());
        }
        record.created(walletTransaction.created());
        record.updated(walletTransaction.updated());
        record.userId(walletTransaction.userId().value());
        record.walletTransactionId(walletTransaction.walletTransactionId().value());
        record.operation(walletTransaction.operation());
        record.amount(walletTransaction.amount().value());
        return record;
    }
}
