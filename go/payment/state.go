package payment

type State string

const (
	Pending    State = "PENDING"
	Authorized State = "AUTHORIZED"
	Captured   State = "CAPTURED"
	Failed     State = "FAILED"
	Cancelled  State = "CANCELLED"
)
