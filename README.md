# Finite State Machine

Un ejemplo pequeño para explicar como modelar un dominio con una **máquina de estados finita (FSM)** explícita. 
Para este caso, el ciclo de vida de un pago, donde pueden suceder transiciones ilegales y esto es un riesgo de negocio real y no solo teórico.

En la práctica, el dominio impone reglas simples: no puedes capturar un pago que nunca se autorizó, ni cancelar uno que ya quedó liquidado. La máquina de estados las centraliza en un solo lugar, en lugar de ir repitiendo `if` por el código.

## Estructura del proyecto

```
src/domain/payment/
  PaymentState.ts          # los estados
  PaymentEvent.ts          # los eventos
  PaymentStateMachine.ts   # tabla de transiciones + máquina
  Payment.ts               # agregado
tests/
  PaymentStateMachine.test.ts
  Payment.test.ts
```

## Primeros pasos

```bash
pnpm install
pnpm test
```