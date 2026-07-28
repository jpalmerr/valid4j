# Changelog

## Unreleased

Not yet published. `io.github.jpalmerr:valid4j` is not available on Maven Central; build from
source or install to your local Maven repository.

### Features

- `Validated<E, A>` sealed interface with `Valid` and `Invalid` cases
- `ValidatedNel` convenience layer for `NonEmptyList` error accumulation
- `combine` for applicative composition (arities 2–8)
- `andThen` for sequential dependent validation
- `sequence` for batch validation of lists
- `map`, `mapError`, `fold`, `getOrElseThrow` operations
- `NonEmptyList<A>` immutable collection with type-level non-emptiness guarantee
- `Semigroup<A>` interface for custom error combination strategies
- Java 21 pattern matching support via sealed types and records
- Zero runtime dependencies
