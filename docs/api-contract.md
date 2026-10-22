# Local booking API contract

All dates are ISO calendar dates, interpreted as half-open intervals `[start, end)`. Both must be real calendar dates and start must be before end. Room names contain 1–80 ASCII letters, digits or hyphens. Extra booking fields are rejected.

| Method / path | Authentication | Result |
|---|---|---|
| GET /health | None | 200: status |
| POST /auth/token | Demo username/password | 200: access_token, token_type, expires_in; 401 otherwise |
| POST /bookings | Bearer token + Idempotency-Key | 201 new; 200 replay; 400 key/JSON; 422 input; 409 conflict |
| GET /bookings?room=... | Bearer token | 200: caller's items, optionally filtered by exact room |
| GET /bookings/{id} | Bearer token | 200 own resource; 404 absent or owned by another user |
| DELETE /bookings/{id} | Bearer token | 200 cancelled resource, including repeated cancellation; 404 otherwise |

Demo users: `alice` and `bob`, password `demo-password`. Tokens expire after 900 seconds. Invalid or missing tokens return 401 on protected routes. The suite checks absent/forged tokens; expiry is not yet covered.

Create request:
```json
{"room":"room-example","start":"2030-05-10","end":"2030-05-12"}
```

Response (id is generated):
```json
{"id":"e928bcf5-f5a5-425b-a8e9-269290f80cf3","owner":"alice","room":"room-example","start":"2030-05-10","end":"2030-05-12","status":"confirmed"}
```

Errors have exactly one field: `{"error":"machine_readable_code"}`.
See [booking schema](../src/test/resources/schemas/booking.json) and [error schema](../src/test/resources/schemas/error.json).

Idempotency-Key contains 1–100 ASCII letters, digits or hyphens and is scoped per user. Reusing a key with a different payload yields `idempotency_conflict`. Overlap yields `room_unavailable`. Missing/invalid fields yield `invalid_booking`.

The response schema tests cover booking and error objects. This project does not claim full OpenAPI conformance validation. No pagination, timezone/timestamp support, inventory capacity, refunds or pricing API is included.
