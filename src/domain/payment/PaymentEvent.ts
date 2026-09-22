type PaymentEvent =
    | 'AUTHORIZE'
    | 'CAPTURE'
    | 'FAIL'
    | 'CANCEL';

export default PaymentEvent;