# Booking API QA

An executable Java API-testing portfolio focused on **data integrity**: contracts, ownership, idempotency and competing requests. A local Node/SQLite booking service is included and started automatically by JUnit.

[Русская версия](README.ru.md) · [Architecture](docs/architecture.md) · [API contract](docs/api-contract.md) · [Test matrix](docs/test-matrix.md)

**Stack:** Java 21+ · JUnit 6 · REST Assured 6 · Maven Wrapper · JSON Schema · Node.js 24 / SQLite · GitHub Actions

## Quick start

Install **JDK 21 or later** and **Node.js 24**. Ensure `JAVA_HOME` points to the JDK and `node` is on PATH. Maven is downloaded by the wrapper on its first run.

Windows PowerShell:
```powershell
.\mvnw.cmd -B -ntp test
```

Linux / macOS:
```sh
./mvnw -B -ntp test
```

No Docker, database server, API key or remote demo account is needed. JUnit starts a local process on a free ephemeral port, creates an in-memory SQLite database, and closes the process after the test plan.

```powershell
.\mvnw.cmd -B -ntp "-Dtest=ConcurrencyTest" test
.\mvnw.cmd -B -ntp "-Dtest=IdempotencyTest" test
```

Reports: `target/surefire-reports/` (text + JUnit XML). Fixture diagnostics: `target/fixture-stderr.log`. To use a non-default Node executable: `-Dnode.executable=/absolute/path/to/node`.

## What this demonstrates

- **36 test invocations**, including parameterized date/field/room boundaries.
- Typed immutable request data and a builder producing isolated room identifiers.
- An API adapter with per-request REST Assured configuration, safe for concurrent tests.
- JSON Schema validation alongside status, header and business assertions.
- Owner-only access and list filtering; no leaked existence of another user's booking.
- Idempotency replay, payload mismatch, per-user keys and JSON-key-order independence.
- Two eight-request concurrency scenarios: one booking winner, and exactly one resource for repeated keys.
- Half-open date ranges, cancellation and availability reuse.
- JUnit resource ownership and automatic server cleanup.

## Structure

```text
src/test/java/qa/booking/tests/    risks and assertions
src/test/java/qa/booking/client/   HTTP adapter and immutable request builder
src/test/java/qa/booking/support/ JUnit extension and process lifecycle
src/test/resources/schemas/       independently specified response contracts
fixture/server.mjs               HTTP application under test + SQLite
docs/                            API rules, decisions, matrix and review guide
```

The [CI workflow](.github/workflows/tests.yml) runs on JDK 21 and 25, with Node 24, and uploads Surefire reports. A workflow file is configuration; a successful hosted CI run must be verified separately.

## Limits

The service is an educational localhost fixture: two synthetic users, short-lived in-memory tokens, in-memory SQLite, no external identity provider, pricing or booking capacity beyond one room slot. Data disappears on shutdown.

The SQLite operation is synchronous and transaction-protected in one Node process. Concurrent HTTP tests verify observable invariants in this setup; they are **not** a distributed-system proof or a load benchmark. Next steps include a real PostgreSQL service, multiple application instances, token-expiration tests and measured load tests.

[MIT license](LICENSE).
