package com.jcoppede.fsm.payment;

/** Se lanza cuando se despacha un evento que el estado actual no acepta. */
public class IllegalTransitionException extends RuntimeException {

    private final PaymentState from;
    private final PaymentEvent event;

    public IllegalTransitionException(PaymentState from, PaymentEvent event) {
        super("Transición ilegal: no se puede aplicar \"" + event
                + "\" mientras se encuentra en estado \"" + from + "\".");
        this.from = from;
        this.event = event;
    }

    public PaymentState from() {
        return from;
    }

    public PaymentEvent event() {
        return event;
    }
}
