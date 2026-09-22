import PaymentStateMachine, {
    IllegalTransitionError,
} from '../src/domain/payment/PaymentStateMachine';

describe('PaymentStateMachine', () => {
    it('arranca en PENDING por defecto', () => {
        expect(new PaymentStateMachine().state).toBe('PENDING');
    });

    it('acepta un estado inicial explícito', () => {
        expect(new PaymentStateMachine('AUTHORIZED').state).toBe('AUTHORIZED');
    });

    describe('flujo normal', () => {
        it('PENDING -> AUTHORIZED -> CAPTURED', () => {
            const m = new PaymentStateMachine();
            expect(m.dispatch('AUTHORIZE')).toBe('AUTHORIZED');
            expect(m.dispatch('CAPTURE')).toBe('CAPTURED');
        });
    });

    describe('transiciones desde PENDING', () => {
        it('permite AUTHORIZE, FAIL y CANCEL', () => {
            expect(new PaymentStateMachine('PENDING').dispatch('AUTHORIZE')).toBe('AUTHORIZED');
            expect(new PaymentStateMachine('PENDING').dispatch('FAIL')).toBe('FAILED');
            expect(new PaymentStateMachine('PENDING').dispatch('CANCEL')).toBe('CANCELLED');
        });

        it('no permite CAPTURE (no se puede capturar un pago sin autorizar)', () => {
            expect(new PaymentStateMachine('PENDING').can('CAPTURE')).toBe(false);
        });
    });

    describe('transiciones desde AUTHORIZED', () => {
        it('permite CAPTURE, FAIL y CANCEL', () => {
            expect(new PaymentStateMachine('AUTHORIZED').dispatch('CAPTURE')).toBe('CAPTURED');
            expect(new PaymentStateMachine('AUTHORIZED').dispatch('FAIL')).toBe('FAILED');
            expect(new PaymentStateMachine('AUTHORIZED').dispatch('CANCEL')).toBe('CANCELLED');
        });

        it('no permite reautorizar', () => {
            expect(new PaymentStateMachine('AUTHORIZED').can('AUTHORIZE')).toBe(false);
        });
    });

    describe('estados terminales', () => {
        it.each(['CAPTURED', 'FAILED', 'CANCELLED'] as const)(
            '%s es terminal y no acepta eventos',
            (state) => {
                const m = new PaymentStateMachine(state);
                expect(m.isTerminal).toBe(true);
                expect(m.allowedEvents()).toEqual([]);
            },
        );

        it('rechaza cualquier evento en un estado terminal', () => {
            const m = new PaymentStateMachine('CAPTURED');
            expect(() => m.dispatch('CANCEL')).toThrow(IllegalTransitionError);
        });
    });

    describe('dispatch ilegal', () => {
        it('lanza IllegalTransitionError con from y event', () => {
            const m = new PaymentStateMachine('PENDING');
            try {
                m.dispatch('CAPTURE');
                fail('debería haber lanzado');
            } catch (err) {
                expect(err).toBeInstanceOf(IllegalTransitionError);
                const e = err as IllegalTransitionError;
                expect(e.from).toBe('PENDING');
                expect(e.event).toBe('CAPTURE');
            }
        });

        it('no muta el estado cuando la transición es ilegal', () => {
            const m = new PaymentStateMachine('PENDING');
            expect(() => m.dispatch('CAPTURE')).toThrow();
            expect(m.state).toBe('PENDING');
        });
    });

    describe('consultas sin efectos secundarios', () => {
        it('peek devuelve el destino sin transicionar', () => {
            const m = new PaymentStateMachine('PENDING');
            expect(m.peek('AUTHORIZE')).toBe('AUTHORIZED');
            expect(m.state).toBe('PENDING');
        });

        it('peek: devuelve undefined para un evento no permitido', () => {
            expect(new PaymentStateMachine('PENDING').peek('CAPTURE')).toBeUndefined();
        });

        it('allowedEvents: lista los eventos permitidos', () => {
            expect(new PaymentStateMachine('PENDING').allowedEvents().sort()).toEqual(
                ['AUTHORIZE', 'CANCEL', 'FAIL'],
            );
        });
    });
});
