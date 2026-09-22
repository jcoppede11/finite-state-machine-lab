package com.jcoppede.fsm.payment;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Máquina de estados finita determinista para un pago.
 *
 * La máquina solo posee el estado actual y las reglas para cambiarlo.
 * Un estado sin transiciones salientes es terminal.
 */
public class PaymentStateMachine {

    /** Tabla explícita de transiciones: estado -> (evento -> estado destino). */
    private static final Map<PaymentState, Map<PaymentEvent, PaymentState>> TRANSITIONS =
            buildTransitions();

    private static Map<PaymentState, Map<PaymentEvent, PaymentState>> buildTransitions() {
        Map<PaymentState, Map<PaymentEvent, PaymentState>> t = new EnumMap<>(PaymentState.class);

        Map<PaymentEvent, PaymentState> pending = new EnumMap<>(PaymentEvent.class);
        pending.put(PaymentEvent.AUTHORIZE, PaymentState.AUTHORIZED);
        pending.put(PaymentEvent.FAIL, PaymentState.FAILED);
        pending.put(PaymentEvent.CANCEL, PaymentState.CANCELLED);
        t.put(PaymentState.PENDING, pending);

        Map<PaymentEvent, PaymentState> authorized = new EnumMap<>(PaymentEvent.class);
        authorized.put(PaymentEvent.CAPTURE, PaymentState.CAPTURED);
        authorized.put(PaymentEvent.FAIL, PaymentState.FAILED);
        authorized.put(PaymentEvent.CANCEL, PaymentState.CANCELLED);
        t.put(PaymentState.AUTHORIZED, authorized);

        return t;
    }

    private PaymentState current;

    public PaymentStateMachine() {
        this(PaymentState.PENDING);
    }

    public PaymentStateMachine(PaymentState initial) {
        this.current = initial;
    }

    /** Estado en el que se encuentra la máquina actualmente. */
    public PaymentState state() {
        return current;
    }

    /** {@code true} si el pago no puede transicionar. */
    public boolean isTerminal() {
        return allowedEvents().isEmpty();
    }

    /** Eventos aceptados desde el estado actual. */
    public Set<PaymentEvent> allowedEvents() {
        return Collections.unmodifiableSet(
                TRANSITIONS.getOrDefault(current, Map.of()).keySet());
    }

    /** Indica si {@code event} sería aceptado desde el estado actual. */
    public boolean can(PaymentEvent event) {
        return TRANSITIONS.getOrDefault(current, Map.of()).containsKey(event);
    }

    /** Estado al que llevaría {@code event}, o vacío si no está permitido. */
    public Optional<PaymentState> peek(PaymentEvent event) {
        return Optional.ofNullable(TRANSITIONS.getOrDefault(current, Map.of()).get(event));
    }

    /**
     * Aplica {@code event} y avanza al siguiente estado.
     *
     * @return el nuevo estado
     * @throws IllegalTransitionException si el evento no está permitido en este estado
     */
    public PaymentState dispatch(PaymentEvent event) {
        PaymentState next = TRANSITIONS.getOrDefault(current, Map.of()).get(event);
        if (next == null) throw new IllegalTransitionException(current, event);
        current = next;
        return current;
    }
}
