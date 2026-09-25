package payment

type Event string

const (
	Authorize Event = "AUTHORIZE"
	Capture   Event = "CAPTURE"
	Fail      Event = "FAIL"
	Cancel    Event = "CANCEL"
)
