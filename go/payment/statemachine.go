package payment

import (
	"fmt"
	"slices"
)

// transitions es la tabla explícita de transiciones: estado -> (evento -> estado destino).
//
// Un estado omitido del mapa (o un evento ausente para un estado dado) implica
// una transición terminal / ilegal y la máquina la rechaza.
var transitions = map[State]map[Event]State{
	Pending: {
		Authorize: Authorized,
		Fail:      Failed,
		Cancel:    Cancelled,
	},
	Authorized: {
		Capture: Captured,
		Fail:    Failed,
		Cancel:  Cancelled,
	},
}

// Se lanza cuando se despacha un evento que el estado actual no acepta.
type IllegalTransitionError struct {
	From  State
	Event Event
}

func (eventError *IllegalTransitionError) Error() string {
	return fmt.Sprintf(
		"Transición ilegal: no se puede aplicar %q mientras se encuentra en estado %q",
		eventError.Event,
		eventError.From,
	)
}

// Máquina de estados finita determinista para un pago.
//
// La máquina solo posee el estado actual y las reglas para cambiarlo.
// Un estado sin transiciones salientes es terminal.
type StateMachine struct {
	current State
}

// Crea una máquina en PENDING.
func NewStateMachine() *StateMachine {
	return NewStateMachineAt(Pending)
}

// Crea una máquina con un estado inicial explícito.
func NewStateMachineAt(initial State) *StateMachine {
	return &StateMachine{current: initial}
}

// Estado en el que se encuentra la máquina actualmente.
func (m *StateMachine) State() State {
	return m.current
}

// Indica si el pago no puede transicionar.
func (m *StateMachine) IsTerminal() bool {
	return len(transitions[m.current]) == 0
}

// Devuelve los eventos aceptados desde el estado actual, ordenados.
func (m *StateMachine) AllowedEvents() []Event {
	events := make([]Event, 0, len(transitions[m.current]))
	for e := range transitions[m.current] {
		events = append(events, e)
	}
	slices.Sort(events)
	return events
}

// Indica si event sería aceptado desde el estado actual.
func (m *StateMachine) Can(event Event) bool {
	_, ok := transitions[m.current][event]
	return ok
}

// Devuelve el estado al que llevaría event, sin transicionar.
// State == false si el evento no está permitido.
func (m *StateMachine) Peek(event Event) (State, bool) {
	next, ok := transitions[m.current][event]
	return next, ok
}

// Aplica event y avanza al siguiente estado.
// Devuelve *IllegalTransitionError si el evento no está permitido en este estado;
func (m *StateMachine) Dispatch(event Event) (State, error) {
	next, ok := transitions[m.current][event]
	if !ok {
		return m.current, &IllegalTransitionError{From: m.current, Event: event}
	}
	m.current = next
	return m.current, nil
}
