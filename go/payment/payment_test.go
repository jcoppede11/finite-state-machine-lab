package payment

import (
	"errors"
	"math/big"
	"testing"
)

func newPayment(t *testing.T) *Payment {
	t.Helper()
	p, err := New("pay_1", big.NewRat(1000, 1), "ARS")
	if err != nil {
		t.Fatalf("New devolvió error: %v", err)
	}
	return p
}

func TestConstructionExposesData(t *testing.T) {
	p := newPayment(t)

	if p.ID() != "pay_1" {
		t.Errorf("id: obtuve %q", p.ID())
	}
	if p.Amount().Cmp(big.NewRat(1000, 1)) != 0 {
		t.Errorf("amount: obtuve %v", p.Amount())
	}
	if p.Currency() != "ARS" {
		t.Errorf("currency: obtuve %q", p.Currency())
	}
	if p.State() != Pending {
		t.Errorf("state: obtuve %q", p.State())
	}
	if p.IsSettled() {
		t.Error("un pago nuevo no debería estar liquidado")
	}
}

func TestConstructionRejectsBlankID(t *testing.T) {
	if _, err := New("", big.NewRat(1000, 1), "ARS"); !errors.Is(err, ErrInvalidPayment) {
		t.Fatalf("esperaba ErrInvalidPayment, obtuve %v", err)
	}
}

func TestConstructionRejectsNonPositiveAmount(t *testing.T) {
	for _, amount := range []*big.Rat{big.NewRat(0, 1), big.NewRat(-5, 1)} {
		if _, err := New("pay_1", amount, "ARS"); !errors.Is(err, ErrInvalidPayment) {
			t.Errorf("monto %v: esperaba ErrInvalidPayment, obtuve %v", amount, err)
		}
	}
	if _, err := New("pay_1", nil, "ARS"); !errors.Is(err, ErrInvalidPayment) {
		t.Errorf("monto nil: esperaba ErrInvalidPayment, obtuve %v", err)
	}
}

func TestConstructionRejectsBlankCurrency(t *testing.T) {
	if _, err := New("pay_1", big.NewRat(1000, 1), ""); !errors.Is(err, ErrInvalidPayment) {
		t.Fatalf("esperaba ErrInvalidPayment, obtuve %v", err)
	}
}

func TestAuthorizeThenCaptureSettles(t *testing.T) {
	p := newPayment(t)
	if err := p.Authorize(); err != nil {
		t.Fatalf("authorize: %v", err)
	}
	if err := p.Capture(); err != nil {
		t.Fatalf("capture: %v", err)
	}
	if p.State() != Captured {
		t.Fatalf("esperaba %q, obtuve %q", Captured, p.State())
	}
	if !p.IsSettled() {
		t.Error("el pago debería estar liquidado")
	}
}

func TestCancelFromPending(t *testing.T) {
	p := newPayment(t)
	if err := p.Cancel(); err != nil {
		t.Fatalf("cancel: %v", err)
	}
	if p.State() != Cancelled {
		t.Fatalf("esperaba %q, obtuve %q", Cancelled, p.State())
	}
	if !p.IsSettled() {
		t.Error("el pago debería estar liquidado")
	}
}

func TestFailFromAuthorized(t *testing.T) {
	p := newPayment(t)
	if err := p.Authorize(); err != nil {
		t.Fatalf("authorize: %v", err)
	}
	if err := p.Fail(); err != nil {
		t.Fatalf("fail: %v", err)
	}
	if p.State() != Failed {
		t.Fatalf("esperaba %q, obtuve %q", Failed, p.State())
	}
}

func TestCannotCaptureWithoutAuthorize(t *testing.T) {
	p := newPayment(t)

	var ill *IllegalTransitionError
	if err := p.Capture(); !errors.As(err, &ill) {
		t.Fatalf("esperaba *IllegalTransitionError, obtuve %v", err)
	}
	if p.State() != Pending {
		t.Fatalf("el estado no debería cambiar, obtuve %q", p.State())
	}
}

func TestCannotOperateOnSettledPayment(t *testing.T) {
	p := newPayment(t)
	if err := p.Authorize(); err != nil {
		t.Fatalf("authorize: %v", err)
	}
	if err := p.Capture(); err != nil {
		t.Fatalf("capture: %v", err)
	}

	var ill *IllegalTransitionError
	if err := p.Cancel(); !errors.As(err, &ill) {
		t.Fatalf("esperaba *IllegalTransitionError, obtuve %v", err)
	}
}
