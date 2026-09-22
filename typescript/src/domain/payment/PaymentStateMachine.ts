import PaymentState from './PaymentState';
import PaymentEvent from './PaymentEvent';

/**
 * Tabla explícita de transiciones del ciclo de vida del pago.
 *
 * Un estado omitido del mapa (o un evento ausente para un estado dado) implica
 * una transición terminal / ilegal y la máquina la rechaza.
 */
const TRANSITIONS: Partial<Record<PaymentState, Partial<Record<PaymentEvent, PaymentState>>>> = {
    PENDING: {
        AUTHORIZE: 'AUTHORIZED',
        FAIL: 'FAILED',
        CANCEL: 'CANCELLED',
    },
    AUTHORIZED: {
        CAPTURE: 'CAPTURED',
        FAIL: 'FAILED',
        CANCEL: 'CANCELLED',
    },
};

/** Se lanza cuando se despacha un evento que el estado actual no acepta. */
export class IllegalTransitionError extends Error {
    constructor(
        readonly from: PaymentState,
        readonly event: PaymentEvent,
    ) {
        super(`Transición ilegal: no se puede aplicar "${event}" mientras se encuentra en estado "${from}".`);
        this.name = 'IllegalTransitionError';
    }
}

/**
 * Máquina de estados finita determinista para un pago.
 *
 * La máquina solo posee el estado actual y las reglas para cambiarlo; a propósito
 * no incluye persistencia, montos ni efectos secundarios para poder razonar
 * sobre el ciclo de vida de forma aislada.
 */
class PaymentStateMachine {
    private current: PaymentState;

    constructor(initial: PaymentState = 'PENDING') {
        this.current = initial;
    }

    /** Estado en el que se encuentra la máquina actualmente. */
    get state(): PaymentState {
        return this.current;
    }

    /** @returns `true` si el pago no puede transicionar. */
    get isTerminal(): boolean {
        return this.allowedEvents().length === 0;
    }

    /** Eventos aceptados. */
    allowedEvents(): PaymentEvent[] {
        return Object.keys(TRANSITIONS[this.current] ?? {}) as PaymentEvent[];
    }

    /** Indica si `event` sería aceptado desde el estado actual. */
    can(event: PaymentEvent): boolean {
        return TRANSITIONS[this.current]?.[event] !== undefined;
    }

    /** Estado al que llevaría `event`, o undefined si no está permitido. */
    peek(event: PaymentEvent): PaymentState | undefined {
        return TRANSITIONS[this.current]?.[event];
    }

    /**
     * Aplica `event` y avanza al siguiente estado.
     * @throws {IllegalTransitionError} si el evento no está permitido en este estado.
     */
    dispatch(event: PaymentEvent): PaymentState {
        const next = this.peek(event);
        
        if (next === undefined) throw new IllegalTransitionError(this.current, event);
        this.current = next;
        
        return this.current;
    }
}

export default PaymentStateMachine;
