# Review walkthrough

1. Run the suite from a fresh checkout.
2. Read [ConcurrencyTest](../src/test/java/qa/booking/tests/ConcurrencyTest.java): a barrier starts eight independent HTTP requests; the oracle checks both responses and stored state.
3. Compare [IdempotencyTest](../src/test/java/qa/booking/tests/IdempotencyTest.java) with the written [contract](api-contract.md).
4. Follow ApiExtension to LocalApiServer: startup deadline, ephemeral port, diagnostic output, AutoCloseable cleanup.
5. Inspect the transaction in the fixture and discuss the limitations of a synchronous single-process implementation.

## Interview questions

- Why is HTTP 201 count alone insufficient to prove no duplicate resources exist?
- Why can adjoining intervals coexist?
- Why does a request's JSON key order not affect idempotency?
- Why return 404 for another user's resource?
- What changes with PostgreSQL and multiple service instances?
- Which test gaps would you prioritize next?

## Honest resume wording

“Built an educational Java/REST Assured API suite with JSON Schema validation, isolated request builders, ownership and idempotency tests, concurrent booking checks, and automatic fixture lifecycle.”

Use actual run counts, keep educational context explicit, and do not claim production throughput or commercial impact.
