package com.ontop.wallet.domain.valueobject;


import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {
    @Nested
    class FractionOf {
        @Test
        void shouldReturnExpectedFractionOf() {
            final Money amount = Money.of(1000L);
            final Money actual = amount.fractionOf(BigDecimal.valueOf(0.1));
            final Money expected = Money.of(100L);
            assertEquals(0, expected.value().compareTo(actual.value()));
            assertEquals(expected.currency(), actual.currency());

        }
    }

    @Nested
    class DeductFrom {
        @Test
        void shouldReturnExpectedDeduction() {
            final Money amount = Money.of(100L);
            final Money actual = amount.deductFrom(BigDecimal.TEN);
            final Money expected = Money.of(90L);
            assertEquals(0, expected.value().compareTo(actual.value()));
            assertEquals(expected.currency(), actual.currency());
        }

        @Test
        void shouldThrowIfArgumentIsNull() {
            final Money amount = Money.of(100L);
            assertThrows(IllegalArgumentException.class, () -> amount.deductFrom(null));
        }
    }

    @Nested
    class Negate {
        @Test
        void shouldReturnExpectedValue() {
            final Money amount = Money.of(BigDecimal.TEN);
            final Money actual = amount.negate();
            final Money expected = Money.of(BigDecimal.TEN.negate());
            assertEquals(expected.value(), actual.value());
            assertEquals(expected.currency(), actual.currency());
        }
    }

    @Nested
    class IsSameCurrencyAndIsGreaterOrEqualTo {
        @Test
        void shouldReturnFalseIfCompareValueNull(){
            final Money amount = Money.of(1010L);
            assertFalse(amount.isSameCurrencyAndIsGreaterOrEqualTo(null));
        }

        @Test
        void shouldReturnFalseIfCompareValueIsLess() {
            final Money amount = Money.of(1000L);
            final Money compareValue = Money.of(1001L);
            assertFalse(amount.isSameCurrencyAndIsGreaterOrEqualTo(compareValue));
        }

        @Test
        void shouldReturnTrueIfCompareValueIsGreater() {
            final Money amount = Money.of(1010L);
            final Money compareValue = Money.of(1001L);
            assertTrue(amount.isSameCurrencyAndIsGreaterOrEqualTo(compareValue));
        }

        @Test
        void shouldReturnTrueIfCompareValueIsEqual(){
            final Money amount = Money.of(1010L);
            final Money compareValue = Money.of(1010L);
            assertTrue(amount.isSameCurrencyAndIsGreaterOrEqualTo(compareValue));
        }

        @Test
        void shouldReturnFalseIfCompareValueCurrencyIsDifferent(){
            final Money amount = Money.of(101000L);
            final Money compareValue = new Money(BigDecimal.valueOf(1010), Currency.getInstance("EUR"));
            assertFalse(amount.isSameCurrencyAndIsGreaterOrEqualTo(compareValue));
        }
    }
}
