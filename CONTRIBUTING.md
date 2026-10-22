# Contributing

Start with the documented business rules and test matrix. A new scenario should explain the risk it detects, create its own data, and pass independently as well as in the parallel suite.

Keep assertions in test scenarios, infrastructure in adapters/fixtures, and demo behavior in the application. Do not add fixed sleeps, shared mutable test accounts/state, or retries to hide a failure.

Before opening a pull request, run the checks in README and describe the behavior changed and how you verified it. Include failure evidence for a regression fix when available. Use synthetic data only.
