package payment

import (
	"errors"
	"math/big"
	"strings"
)

// Devuelve ErrInvalidPayment cuando los datos de construcción de un Payment no son válidos.
var ErrInvalidPayment = errors.New("Pago inválido")

//	Agregado de pago.
//
// Compone la StateMachine con los datos del pago (id, monto, moneda) y expone el
// ciclo de vida como métodos de dominio.
type Payment struct {
	id       string
	currency string
	amount   *big.Rat
	machine  *StateMachine
}

func New(id string, amount *big.Rat, currency string) (*Payment, error) {
	return NewAt(id, amount, currency, Pending)
}

func NewAt(id string, amount *big.Rat, currency string, initial State) (*Payment, error) {
	if strings.TrimSpace(id) == "" {
		return nil, errors.Join(ErrInvalidPayment, errors.New("Requiere un id."))
	}
	if strings.TrimSpace(currency) == "" {
		return nil, errors.Join(ErrInvalidPayment, errors.New("Requiere una moneda."))
	}
	if amount == nil || amount.Sign() <= 0 {
		return nil, errors.Join(ErrInvalidPayment, errors.New("Requiere un monto positivo."))
	}

	return &Payment{
		id:       id,
		currency: currency,
		amount:   new(big.Rat).Set(amount),
		machine:  NewStateMachineAt(initial),
	}, nil
}

func (p *Payment) ID() string {
	return p.id
}

func (p *Payment) Amount() *big.Rat {
	return new(big.Rat).Set(p.amount)
}

func (p *Payment) Currency() string {
	return p.currency
}

func (p *Payment) State() State {
	return p.machine.State()
}

func (p *Payment) IsSettled() bool {
	return p.machine.IsTerminal()
}

func (p *Payment) Authorize() error {
	return p.apply(Authorize)
}

func (p *Payment) Capture() error {
	return p.apply(Capture)
}

func (p *Payment) Fail() error {
	return p.apply(Fail)
}

func (p *Payment) Cancel() error {
	return p.apply(Cancel)
}

func (p *Payment) apply(event Event) error {
	_, err := p.machine.Dispatch(event)
	return err
}
