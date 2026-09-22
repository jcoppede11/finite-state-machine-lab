type PaymentState =
    | 'PENDING'
    | 'AUTHORIZED'
    | 'CAPTURED'
    | 'FAILED'
    | 'CANCELLED';

export default PaymentState;