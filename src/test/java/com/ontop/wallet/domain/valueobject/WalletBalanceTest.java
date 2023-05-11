package com.ontop.wallet.domain.valueobject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class WalletBalanceTest {
    private static final WalletBalance balance = new WalletBalance(new UserId(100L), Money.of(BigDecimal.TEN));
    @Nested
    class MayWithdraw {
        @Test
        void shouldReturnTrueIfGreaterThanCompareValue() {
            assertTrue(balance.mayWithdraw(Money.of(BigDecimal.ONE)));
        }

        @Test
        void shouldReturnTrueIfEqualToCompareValue() {
            assertTrue(balance.mayWithdraw(Money.of(BigDecimal.TEN)));
        }

        @Test
        void shouldReturnFalseIfLessThanCompareValue() {
            assertFalse(balance.mayWithdraw(Money.of(BigDecimal.valueOf(11L))));
        }

        @Test
        void shouldReturnFalseIfCompareValueIsFalse() {
            assertFalse(balance.mayWithdraw(null));
        }
    }
}
