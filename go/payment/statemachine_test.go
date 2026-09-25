package payment

import (
	"errors"
	"slices"
	"testing"
)

func TestStartsPending(t *testing.T) {
	if got := NewStateMachine().State(); got != Pending {
		t.Fatalf("esperaba %q, obtuve %q", Pending, got)
	}
}

func TestAcceptsInitialState(t *testing.T) {
	if got := NewStateMachineAt(Authorized).State(); got != Authorized {
		t.Fatalf("esperaba %q, obtuve %q", Authorized, got)
	}
}

func TestHappyPath(t *testing.T) {
	m := NewStateMachine()

	if got, err := m.Dispatch(Authorize); err != nil || got != Authorized {
		t.Fatalf("AUTHORIZE: esperaba %q, obtuve %q (err=%v)", Authorized, got, err)
	}
	if got, err := m.Dispatch(Capture); err != nil || got != Captured {
		t.Fatalf("CAPTURE: esperaba %q, obtuve %q (err=%v)", Captured, got, err)
	}
}

func TestFromPending(t *testing.T) {
	cases := map[Event]State{
		Authorize: Authorized,
		Fail:      Failed,
		Cancel:    Cancelled,
	}
	for event, want := range cases {
		got, err := NewStateMachineAt(Pending).Dispatch(event)
		if err != nil || got != want {
			t.Errorf("%q: esperaba %q, obtuve %q (err=%v)", event, want, got, err)
		}
	}

	if NewStateMachineAt(Pending).Can(Capture) {
		t.Error("PENDING no debería permitir CAPTURE")
	}
}

func TestFromAuthorized(t *testing.T) {
	cases := map[Event]State{
		Capture: Captured,
		Fail:    Failed,
		Cancel:  Cancelled,
	}
	for event, want := range cases {
		got, err := NewStateMachineAt(Authorized).Dispatch(event)
		if err != nil || got != want {
			t.Errorf("%q: esperaba %q, obtuve %q (err=%v)", event, want, got, err)
		}
	}

	if NewStateMachineAt(Authorized).Can(Authorize) {
		t.Error("AUTHORIZED no debería permitir reautorizar")
	}
}

func TestTerminalStates(t *testing.T) {
	for _, state := range []State{Captured, Failed, Cancelled} {
		m := NewStateMachineAt(state)
		if !m.IsTerminal() {
			t.Errorf("%q debería ser terminal", state)
		}
		if len(m.AllowedEvents()) != 0 {
			t.Errorf("%q no debería aceptar eventos, obtuve %v", state, m.AllowedEvents())
		}
	}
}

func TestRejectsEventOnTerminal(t *testing.T) {
	m := NewStateMachineAt(Captured)
	if _, err := m.Dispatch(Cancel); err == nil {
		t.Fatal("esperaba error al despachar sobre un estado terminal")
	}
}

func TestIllegalDispatchCarriesContext(t *testing.T) {
	m := NewStateMachineAt(Pending)

	_, err := m.Dispatch(Capture)

	var ill *IllegalTransitionError
	if !errors.As(err, &ill) {
		t.Fatalf("esperaba *IllegalTransitionError, obtuve %T", err)
	}
	if ill.From != Pending || ill.Event != Capture {
		t.Fatalf("esperaba from=%q event=%q, obtuve from=%q event=%q",
			Pending, Capture, ill.From, ill.Event)
	}
}

func TestDoesNotMutateOnIllegal(t *testing.T) {
	m := NewStateMachineAt(Pending)
	if _, err := m.Dispatch(Capture); err == nil {
		t.Fatal("esperaba error")
	}
	if m.State() != Pending {
		t.Fatalf("el estado no debería cambiar, obtuve %q", m.State())
	}
}

func TestPeekIsSideEffectFree(t *testing.T) {
	m := NewStateMachineAt(Pending)

	next, ok := m.Peek(Authorize)
	if !ok || next != Authorized {
		t.Fatalf("esperaba %q, obtuve %q (ok=%v)", Authorized, next, ok)
	}
	if m.State() != Pending {
		t.Fatalf("peek no debería transicionar, obtuve %q", m.State())
	}
}

func TestPeekFalseWhenForbidden(t *testing.T) {
	if _, ok := NewStateMachineAt(Pending).Peek(Capture); ok {
		t.Error("peek debería devolver ok=false para un evento no permitido")
	}
}

func TestAllowedEventsListsAccepted(t *testing.T) {
	want := []Event{Authorize, Cancel, Fail}
	got := NewStateMachineAt(Pending).AllowedEvents()
	if !slices.Equal(got, want) {
		t.Fatalf("esperaba %v, obtuve %v", want, got)
	}
}
