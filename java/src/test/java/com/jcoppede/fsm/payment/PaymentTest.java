package com.jcoppede.fsm.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentTest {

    private static Payment newPayment() {
        return new Payment("pay_1", new BigDecimal("1000"), "ARS");
    }

    @Nested
    @DisplayName("construcción")
    class Construction {
        @Test
        void exposesDataAndStartsPending() {
            Payment p = newPayment();
            assertEquals("pay_1", p.id());
            assertEquals(new BigDecimal("1000"), p.amount());
            assertEquals("ARS", p.currency());
            assertEquals(PaymentState.PENDING, p.state());
            assertFalse(p.isSettled());
        }

        @Test
        void rejectsBlankId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment("", new BigDecimal("1000"), "ARS"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-5"})
        void rejectsNonPositiveAmount(String amount) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment("pay_1", new BigDecimal(amount), "ARS"));
        }

        @Test
        void rejectsBlankCurrency() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment("pay_1", new BigDecimal("1000"), ""));
        }
    }

    @Nested
    @DisplayName("ciclo de vida")
    class Lifecycle {
        @Test
        void authorizeThenCaptureSettles() {
            Payment p = newPayment().authorize().capture();
            assertEquals(PaymentState.CAPTURED, p.state());
            assertTrue(p.isSettled());
        }

        @Test
        void cancelFromPending() {
            Payment p = newPayment().cancel();
            assertEquals(PaymentState.CANCELLED, p.state());
            assertTrue(p.isSettled());
        }

        @Test
        void failFromAuthorized() {
            Payment p = newPayment().authorize().fail();
            assertEquals(PaymentState.FAILED, p.state());
        }

        @Test
        void domainMethodsAreFluent() {
            Payment p = newPayment();
            assertSame(p, p.authorize());
        }
    }

    @Nested
    @DisplayName("transiciones ilegales")
    class IllegalTransitions {
        @Test
        void cannotCaptureWithoutAuthorize() {
            Payment p = newPayment();
            assertThrows(IllegalTransitionException.class, p::capture);
            assertEquals(PaymentState.PENDING, p.state());
        }

        @Test
        void cannotOperateOnSettledPayment() {
            Payment p = newPayment().authorize().capture();
            assertThrows(IllegalTransitionException.class, p::cancel);
        }
    }
}
