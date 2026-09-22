package com.jcoppede.fsm.payment;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Agregado de pago.
 *
 * Compone la {@link PaymentStateMachine} con los datos del pago (id, monto,
 * moneda) y expone el ciclo de vida como métodos de dominio.
 * 
 */
public class Payment {

    private final String id;
    private final String currency;
    private final BigDecimal amount;
    private final PaymentStateMachine machine;

    public Payment(String id, BigDecimal amount, String currency) {
        this(id, amount, currency, PaymentState.PENDING);
    }

    public Payment(String id, BigDecimal amount, String currency, PaymentState initial) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Payment requiere un id.");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("Payment requiere una moneda.");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Payment requiere un monto positivo.");
        
        this.id = id;
        this.currency = currency;
        this.amount = amount;
        this.machine = new PaymentStateMachine(Objects.requireNonNull(initial));
    }

    public String id() {
        return id;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public PaymentState state() {
        return machine.state();
    }

    public boolean isSettled() {
        return machine.isTerminal();
    }

    public Payment authorize() {
        return apply(PaymentEvent.AUTHORIZE);
    }

    public Payment capture() {
        return apply(PaymentEvent.CAPTURE);
    }

    public Payment fail() {
        return apply(PaymentEvent.FAIL);
    }

    public Payment cancel() {
        return apply(PaymentEvent.CANCEL);
    }

    private Payment apply(PaymentEvent event) {
        machine.dispatch(event);
        return this;
    }
}
