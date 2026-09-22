import PaymentState from './PaymentState';
import PaymentEvent from './PaymentEvent';
import PaymentStateMachine from './PaymentStateMachine';

/** Datos inmutables */
export interface PaymentProps {
    readonly id: string;
    readonly currency: string;
    readonly amount: number;
}

/**
 * Agregado de pago.
 *
 * Compone la {@link PaymentStateMachine} con los datos del pago (id, monto,
 * moneda) y expone el ciclo de vida como métodos de dominio. 
 * 
 */
class Payment {
    private readonly props: PaymentProps;
    private readonly machine: PaymentStateMachine;

    constructor(
        props: PaymentProps,
        initial: PaymentState = 'PENDING'
    ) {
        if (!props.id) throw new Error('Payment requiere un id.');
        if (!props.currency) throw new Error('Payment requiere una moneda.');
        if (!Number.isFinite(props.amount) || props.amount <= 0) throw new Error('Payment requiere un monto positivo.');        

        this.props = props;
        this.machine = new PaymentStateMachine(initial);
    }

    get id(): string {
        return this.props.id;
    }

    get amount(): number {
        return this.props.amount;
    }

    get currency(): string {
        return this.props.currency;
    }

    get state(): PaymentState {
        return this.machine.state;
    }

    get isSettled(): boolean {
        return this.machine.isTerminal;
    }

    authorize(): this {
        return this.apply('AUTHORIZE');
    }

    capture(): this {
        return this.apply('CAPTURE');
    }

    fail(): this {
        return this.apply('FAIL');
    }

    cancel(): this {
        return this.apply('CANCEL');
    }

    private apply(event: PaymentEvent): this {
        this.machine.dispatch(event);
        return this;
    }
}

export default Payment;
