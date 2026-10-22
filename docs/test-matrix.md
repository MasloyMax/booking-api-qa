# Risk-based coverage

| Test class | Invocations | Risk and oracle |
|---|---:|---|
| BookingLifecycleTest | 5 | Create/read/cancel, adjacent intervals, separate rooms, repeat cancellation, unknown ID |
| ValidationTest | 19 | Reversed/equal/impossible dates, leap year, room boundaries, missing fields, ownership injection, malformed JSON, missing key |
| AuthorizationTest | 5 | Missing/forged token, bad login, cross-user read/cancel/list, cross-user overlap |
| IdempotencyTest | 5 | Exact replay, payload conflict, per-user scope, key order, replay after cancellation |
| ConcurrencyTest | 2 | Eight competing creates; eight retries with one key |
| **Total** | **36** | Counts include parameterized invocations |

Highest-priority data-integrity oracles:
- Competing requests: exactly one 201, seven 409 responses, one stored booking.
- Same-key retries: one 201, seven 200 responses, one distinct resource ID.
- Ownership: Bob cannot read/cancel/list Alice's booking; Alice's state remains confirmed.
- Invalid dates: rejection plus an empty room-specific list.
- Cancellation: historical record remains, slot can be booked again.

Gaps: token expiry, process-restart persistence, malformed authorization variants, exhaustive overlap permutations, multiple server instances, load/performance, external identity providers, migration testing. These are visible future work, not hidden claims of coverage.
