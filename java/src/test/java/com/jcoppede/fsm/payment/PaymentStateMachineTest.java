package com.jcoppede.fsm.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaymentStateMachineTest {

    @Test
    @DisplayName("arranca en PENDING por defecto")
    void startsPending() {
        assertEquals(PaymentState.PENDING, new PaymentStateMachine().state());
    }

    @Test
    @DisplayName("acepta un estado inicial explícito")
    void acceptsInitialState() {
        assertEquals(PaymentState.AUTHORIZED,
                new PaymentStateMachine(PaymentState.AUTHORIZED).state());
    }

    @Test
    @DisplayName("flujo normal: PENDING -> AUTHORIZED -> CAPTURED")
    void happyPath() {
        PaymentStateMachine m = new PaymentStateMachine();
        assertEquals(PaymentState.AUTHORIZED, m.dispatch(PaymentEvent.AUTHORIZE));
        assertEquals(PaymentState.CAPTURED, m.dispatch(PaymentEvent.CAPTURE));
    }

    @Nested
    @DisplayName("transiciones desde PENDING")
    class FromPending {
        @Test
        void allowsAuthorizeFailCancel() {
            assertEquals(PaymentState.AUTHORIZED,
                    new PaymentStateMachine(PaymentState.PENDING).dispatch(PaymentEvent.AUTHORIZE));
            assertEquals(PaymentState.FAILED,
                    new PaymentStateMachine(PaymentState.PENDING).dispatch(PaymentEvent.FAIL));
            assertEquals(PaymentState.CANCELLED,
                    new PaymentStateMachine(PaymentState.PENDING).dispatch(PaymentEvent.CANCEL));
        }

        @Test
        void forbidsCapture() {
            assertFalse(new PaymentStateMachine(PaymentState.PENDING).can(PaymentEvent.CAPTURE));
        }
    }

    @Nested
    @DisplayName("transiciones desde AUTHORIZED")
    class FromAuthorized {
        @Test
        void allowsCaptureFailCancel() {
            assertEquals(PaymentState.CAPTURED,
                    new PaymentStateMachine(PaymentState.AUTHORIZED).dispatch(PaymentEvent.CAPTURE));
            assertEquals(PaymentState.FAILED,
                    new PaymentStateMachine(PaymentState.AUTHORIZED).dispatch(PaymentEvent.FAIL));
            assertEquals(PaymentState.CANCELLED,
                    new PaymentStateMachine(PaymentState.AUTHORIZED).dispatch(PaymentEvent.CANCEL));
        }

        @Test
        void forbidsReauthorize() {
            assertFalse(new PaymentStateMachine(PaymentState.AUTHORIZED).can(PaymentEvent.AUTHORIZE));
        }
    }

    @ParameterizedTest
    @EnumSource(value = PaymentState.class, names = {"CAPTURED", "FAILED", "CANCELLED"})
    @DisplayName("los estados terminales no aceptan eventos")
    void terminalStates(PaymentState state) {
        PaymentStateMachine m = new PaymentStateMachine(state);
        assertTrue(m.isTerminal());
        assertTrue(m.allowedEvents().isEmpty());
    }

    @Test
    @DisplayName("rechaza cualquier evento en un estado terminal")
    void rejectsEventOnTerminal() {
        PaymentStateMachine m = new PaymentStateMachine(PaymentState.CAPTURED);
        assertThrows(IllegalTransitionException.class, () -> m.dispatch(PaymentEvent.CANCEL));
    }

    @Test
    @DisplayName("dispatch ilegal lanza IllegalTransitionException con from y event")
    void illegalDispatchCarriesContext() {
        PaymentStateMachine m = new PaymentStateMachine(PaymentState.PENDING);
        IllegalTransitionException ex = assertThrows(IllegalTransitionException.class,
                () -> m.dispatch(PaymentEvent.CAPTURE));
        assertEquals(PaymentState.PENDING, ex.from());
        assertEquals(PaymentEvent.CAPTURE, ex.event());
    }

    @Test
    @DisplayName("no muta el estado cuando la transición es ilegal")
    void doesNotMutateOnIllegal() {
        PaymentStateMachine m = new PaymentStateMachine(PaymentState.PENDING);
        assertThrows(IllegalTransitionException.class, () -> m.dispatch(PaymentEvent.CAPTURE));
        assertEquals(PaymentState.PENDING, m.state());
    }

    @Test
    @DisplayName("peek devuelve el destino sin transicionar")
    void peekIsSideEffectFree() {
        PaymentStateMachine m = new PaymentStateMachine(PaymentState.PENDING);
        assertEquals(PaymentState.AUTHORIZED, m.peek(PaymentEvent.AUTHORIZE).orElseThrow());
        assertEquals(PaymentState.PENDING, m.state());
    }

    @Test
    @DisplayName("peek vacío para un evento no permitido")
    void peekEmptyWhenForbidden() {
        assertTrue(new PaymentStateMachine(PaymentState.PENDING).peek(PaymentEvent.CAPTURE).isEmpty());
    }

    @Test
    @DisplayName("allowedEvents lista solo los eventos permitidos")
    void allowedEventsListsAccepted() {
        assertEquals(
                Set.of(PaymentEvent.AUTHORIZE, PaymentEvent.FAIL, PaymentEvent.CANCEL),
                new PaymentStateMachine(PaymentState.PENDING).allowedEvents());
    }
}
