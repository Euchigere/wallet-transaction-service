package com.ontop.wallet.adapters.jpa.entities;

import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "transfer")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class TransferRecord extends BaseEntity {
    private UUID paymentTransactionId;

    @Column(nullable = false)
    private String ontopAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(nullable = false)
    private BigDecimal transferCharge;

    @Column(nullable = false)
    private BigDecimal transferAmount;

    @Column(nullable = false)
    private Currency currency;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_account_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private AccountRecord targetAccount;

    @OneToMany(mappedBy = "transfer", fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @ToString.Exclude
    private List<WalletTransactionRecord> walletTransactions = Collections.emptyList();

    public void walletTransactions(List<WalletTransactionRecord> walletTransactions) {
        walletTransactions.forEach(wt -> wt.transfer(this));
        this.walletTransactions = walletTransactions;
    }

    public Transfer toDomain() {
        return Transfer.transfer()
                .id(new Id<>(this.id))
                .created(this.created)
                .updated(this.updated)
                .paymentTransactionId(this.paymentTransactionId == null ? null : new PaymentTransactionId(this.paymentTransactionId))
                .status(this.status)
                .transferCharge(Money.of(this.transferCharge))
                .currency(this.currency)
                .transferAmount(Money.of(this.transferAmount))
                .ontopAccountNumber(new AccountNumber(this.ontopAccountNumber))
                .targetAccount(this.targetAccount.toDomain())
                .walletTransactions(this.walletTransactions.stream().map(WalletTransactionRecord::toDomain)
                        .collect(Collectors.toList()))
                .build();
    }

    public static TransferRecord of(@NonNull Transfer transfer) {
        final TransferRecord record = new TransferRecord();
        if (transfer.id() != null) {
            record.id(transfer.id().value());
        }
        if (transfer.paymentTransactionId() != null) {
            record.paymentTransactionId(transfer.paymentTransactionId().value());
        }
        record.created(transfer.created());
        record.updated(transfer.updated());
        record.ontopAccountNumber(transfer.ontopAccountNumber().value());
        record.status(transfer.status());
        record.transferCharge(transfer.transferCharge().value());
        record.transferAmount(transfer.transferAmount().value());
        record.currency(transfer.currency());
        record.targetAccount(AccountRecord.of(transfer.targetAccount()));
        final List<WalletTransactionRecord> transactions = transfer.walletTransactions().stream()
                .map(WalletTransactionRecord::of).toList();
        record.walletTransactions(transactions);
        return record;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        TransferRecord that = (TransferRecord) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
