package com.jcoppede.fsm.payment;

public enum PaymentState {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED
}
