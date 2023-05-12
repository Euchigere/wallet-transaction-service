package com.ontop.wallet.domain.valueobject;

import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(@NonNull BigDecimal value, @NonNull Currency currency) {
    private static final String DEFAULT_CURRENCY_CODE = "USD";
    public static final Currency DEFAULT_CURRENCY = Currency.getInstance(DEFAULT_CURRENCY_CODE);

    public static Money of(@NonNull BigDecimal value) {
        return new Money(value, DEFAULT_CURRENCY);
    }

    public static Money of(@NonNull Long value) {
        return of(BigDecimal.valueOf(value));
    }

    public static Money of(@NonNull Double value) {
        return of(BigDecimal.valueOf(value));
    }

    public boolean isSameCurrencyAndIsGreaterOrEqualTo(Money money) {
        if (money == null) {
            return false;
        }
        return hasTheSameCurrency(money.currency) &&
                (this.value.compareTo(money.value) >= 0);
    }

    public Money fractionOf(@NonNull BigDecimal percentage) {
        final BigDecimal product = value.abs().multiply(percentage);
        return Money.of(product);
    }

    public Money deductFrom(@NonNull BigDecimal amount) {
        final BigDecimal result =  this.value.abs().subtract(amount);
        return Money.of(result);
    }

    public Money negate() {
        return Money.of(this.value.negate());
    }

    private boolean hasTheSameCurrency(Currency currency) {
        return this.currency.equals(currency);
    }
}
