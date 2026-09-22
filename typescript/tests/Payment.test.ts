import Payment, { PaymentProps } from '../src/domain/payment/Payment';
import { IllegalTransitionError } from '../src/domain/payment/PaymentStateMachine';

const props = (): PaymentProps => ({ id: 'pay_1', amount: 1000, currency: 'ARS' });

describe('Payment', () => {
    describe('construcción', () => {
        it('expone id, monto y moneda y arranca en PENDING', () => {
            const p = new Payment(props());
            expect(p.id).toBe('pay_1');
            expect(p.amount).toBe(1000);
            expect(p.currency).toBe('ARS');
            expect(p.state).toBe('PENDING');
            expect(p.isSettled).toBe(false);
        });

        it('rechazo: id vacío', () => {
            expect(() => new Payment({ ...props(), id: '' })).toThrow();
        });

        it.each([0, -5, NaN])('rechazo: monto no positivo: %p', (amount) => {
            expect(() => new Payment({ ...props(), amount })).toThrow();
        });

        it('rechazo: moneda vacía', () => {
            expect(() => new Payment({ ...props(), currency: '' })).toThrow();
        });
    });

    describe('ciclo de vida', () => {
        it('authorize luego capture liquida el pago', () => {
            const p = new Payment(props());
            p.authorize().capture();
            expect(p.state).toBe('CAPTURED');
            expect(p.isSettled).toBe(true);
        });

        it('permite cancelar desde PENDING', () => {
            const p = new Payment(props());
            p.cancel();
            expect(p.state).toBe('CANCELLED');
            expect(p.isSettled).toBe(true);
        });

        it('permite fallar desde AUTHORIZED', () => {
            const p = new Payment(props());
            p.authorize().fail();
            expect(p.state).toBe('FAILED');
        });
    });

    describe('transiciones ilegales', () => {
        it('no se puede capturar un pago sin autorizar', () => {
            const p = new Payment(props());
            expect(() => p.capture()).toThrow(IllegalTransitionError);
            expect(p.state).toBe('PENDING');
        });

        it('no se puede operar sobre un pago ya liquidado', () => {
            const p = new Payment(props());
            p.authorize().capture();
            expect(() => p.cancel()).toThrow(IllegalTransitionError);
        });
    });
});
