package com.ontop.wallet.adapters.jpa.entities;

import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.NationalIdNumber;
import com.ontop.wallet.domain.valueobject.PersonName;
import com.ontop.wallet.domain.valueobject.RoutingNumber;
import com.ontop.wallet.domain.valueobject.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.util.Currency;
import java.util.Objects;

@Entity
@Table(name = "account")
@Getter
@Setter
@ToString
public class AccountRecord extends BaseEntity {
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String routingNumber;

    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false, unique = true)
    private String nationalIdNumber;

    public UserAccount toDomain() {
        return UserAccount.userAccount()
                .id(new Id<>(this.id))
                .created(this.created)
                .updated(this.updated)
                .userId(new UserId(this.userId))
                .name(new PersonName(this.firstName, this.lastName))
                .accountNumber(new AccountNumber(this.accountNumber))
                .routingNumber(new RoutingNumber(this.routingNumber))
                .currency(this.currency)
                .nationalIdNumber(new NationalIdNumber(this.nationalIdNumber))
                .build();
    }

    public static AccountRecord of(@NonNull UserAccount userAccount) {
        final AccountRecord accountRecord = new AccountRecord();
        if (userAccount.id() != null) {
            accountRecord.id(userAccount.id().value());
        }
        accountRecord.created(userAccount.created());
        accountRecord.updated(userAccount.updated());
        accountRecord.userId(userAccount.userId().value());
        accountRecord.firstName(userAccount.name().firstName());
        accountRecord.lastName(userAccount.name().lastName());
        accountRecord.accountNumber(userAccount.accountNumber().value());
        accountRecord.routingNumber(userAccount.routingNumber().value());
        accountRecord.currency(userAccount.currency());
        accountRecord.nationalIdNumber(userAccount.nationalIdNumber().value());
        return accountRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        final AccountRecord that = (AccountRecord) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
