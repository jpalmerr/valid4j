# valid4j

**A small, zero-dependency Java 21 library for typed error accumulation.**

`valid4j` is a research-driven implementation of applicative validation in modern Java, inspired by Cats' `Validated`.
It explores how far Java 21 features such as sealed interfaces, records, and pattern matching can support a validation API that collects all errors rather than failing fast.

Its scope is deliberately narrow, but the implementation is intended for real use: a lightweight, dependency-free option
for teams that want typed, composable validation without adopting a larger framework or FP runtime.
The library is backed by a comprehensive test suite, runnable examples, and CI, with an accompanying blog post explains the design tradeoffs behind the API.

See blog post [here](https://gist.github.com/jpalmerr/6cc94fa4185a5fadbe2025088b4eec7d).

![CI](https://github.com/jpalmerr/valid4j/actions/workflows/ci.yml/badge.svg)

---

## Why valid4j?

**Validate all fields, collect all errors. Zero boilerplate.**

You can accumulate errors into a `List<String>` and check it at the end. But that gives you:

- **No compiler safety** — nothing forces you to check the list before using the result. Forget `errors.isEmpty()` and you silently proceed with bad data.
- **Coupled logic** — every call site mixes validation with error collection. The accumulation pattern is re-implemented everywhere.
- **Stringly-typed errors** — your errors are strings. Your API layer parses strings. Your tests assert on strings. Rename a message and everything breaks silently.

valid4j gives you a `Validated<E, A>` type where:

- The **compiler forces** you to handle both success and failure (sealed interface, exhaustive `switch`)
- Validation functions are **standalone pure methods** — composition is handled by `combine`, not by the call site
- Errors are **typed** — use strings, enums, sealed hierarchies, whatever fits your domain

---

## Quick Start

```kotlin
// build.gradle.kts
implementation("io.github.jpalmerr:valid4j:0.1.0")
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>io.github.jpalmerr</groupId>
  <artifactId>valid4j</artifactId>
  <version>0.1.0</version>
</dependency>
```

Requires Java 21+. Zero runtime dependencies.

---

## The Problem

Standard validation throws on the first error. Your users see one problem at a time:

```java
// Old way: throws on the first bad field
void register(String name, String email, int age) {
    if (name.isBlank()) throw new IllegalArgumentException("Name is required");
    if (!email.contains("@")) throw new IllegalArgumentException("Email must contain @");
    if (age < 18) throw new IllegalArgumentException("Must be at least 18");
    // ...
}
```

User submits a form with three problems. They fix the name error. Submit again. Now they see the email error. Fix that. Submit again. Now they see the age error. Three round trips for three fixable issues.

**valid4j collects all errors in one pass:**

```java
Validated<NonEmptyList<String>, User> result = ValidatedNel.combine(
    validateName(name),
    validateEmail(email),
    validateAge(age),
    User::new
);

// If all three fail, result holds all three errors — not just the first
```

---

## Usage

### Validating a single field

Write validation functions that return `Validated<E, A>` — either a valid value or an error:

```java
static Validated<NonEmptyList<String>, String> validateName(String name) {
    if (name == null || name.isBlank()) return ValidatedNel.invalidNel("Name is required");
    if (name.length() < 2) return ValidatedNel.invalidNel("Name must be at least 2 characters");
    return ValidatedNel.validNel(name.trim());
}

static Validated<NonEmptyList<String>, String> validateEmail(String email) {
    if (email == null || email.isBlank()) return ValidatedNel.invalidNel("Email is required");
    if (!email.contains("@")) return ValidatedNel.invalidNel("Email must contain @");
    return ValidatedNel.validNel(email.trim().toLowerCase());
}

static Validated<NonEmptyList<String>, Integer> validateAge(int age) {
    if (age < 18) return ValidatedNel.invalidNel("Must be at least 18 years old");
    if (age > 150) return ValidatedNel.invalidNel("Age seems unrealistic");
    return ValidatedNel.validNel(age);
}
```

### Combining validations (error accumulation)

`ValidatedNel.combine` runs all validations independently and collects every error:

```java
record User(String name, String email, int age) {}

Validated<NonEmptyList<String>, User> result = ValidatedNel.combine(
    validateName(""),           // fails: "Name is required"
    validateEmail("not-valid"), // fails: "Email must contain @"
    validateAge(15),            // fails: "Must be at least 18 years old"
    User::new
);

// result is Invalid with ALL THREE errors in a NonEmptyList
```

When everything passes, the mapper is called with the valid values:

```java
Validated<NonEmptyList<String>, User> result = ValidatedNel.combine(
    validateName("Alice"),
    validateEmail("alice@example.com"),
    validateAge(30),
    User::new
);

// result is Valid(User("Alice", "alice@example.com", 30))
```

`ValidatedNel.combine` is overloaded for arities 2 through 8.

### Pattern matching

`Validated` is a sealed interface with two cases. Java 21 switch expressions are exhaustive over it:

```java
String message = switch (result) {
    case Validated.Valid<NonEmptyList<String>, User>(var user) ->
        "Welcome, " + user.name() + "!";
    case Validated.Invalid<NonEmptyList<String>, User>(var errors) ->
        "Please fix: " + String.join(", ", errors.toList());
};
```

Or use `fold` for a functional alternative:

```java
String message = result.fold(
    errors -> "Please fix: " + String.join(", ", errors.toList()),
    user   -> "Welcome, " + user.name() + "!"
);
```

### Sequential validation (andThen)

Sometimes a second validation only makes sense if the first passed. `Validated` is designed for *independent* error accumulation, so it has no `flatMap`. Use `andThen` when step 2 depends on step 1's result:

```java
// Step 1: validate format. Step 2: check business rule. Step 2 only runs if step 1 passes.
Validated<String, String> result = validateEmail(rawEmail)
    .andThen(email ->
        email.endsWith("@blocked.com")
            ? Validated.invalid("This email domain is not allowed")
            : Validated.valid(email));
```

If `validateEmail` fails, `andThen` propagates the error without calling the function. If it succeeds, the function runs and its result (valid or invalid) is returned.

**Why no `flatMap` on `Validated`?** Error accumulation requires that all validations run independently. With `flatMap`, step 2 depends on step 1's result — you cannot run both and collect errors from both. valid4j makes the same choice as Cats: use `combine` for independent fields, `andThen` for dependent sequential steps.

### Transforming results

`map` transforms the value inside a `Valid`, passing through `Invalid` unchanged:

```java
Validated<String, String> email = validateEmail("ALICE@EXAMPLE.COM");
Validated<String, String> lower = email.map(String::toLowerCase);
```

`mapError` transforms error values:

```java
Validated<String, User> result = validateUser(input);
Validated<ApiError, User> apiResult = result.mapError(msg -> new ApiError(400, msg));
```

### Bridging to exceptions (getOrElseThrow)

At the boundary of your validation logic, you often need to throw for frameworks that expect exceptions:

```java
User user = validateUser(form)
    .getOrElseThrow(errors -> new ValidationException(errors.toList()));
```

Returns the value if `Valid`. If `Invalid`, maps the error to an exception and throws it.

### Batch validation (sequence)

Validate a list of items and collect all errors:

```java
List<Validated<NonEmptyList<String>, Integer>> ages = List.of(
    validateAge(25),
    validateAge(30),
    validateAge(-1),   // fails
    validateAge(200)   // fails
);

Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(ages);

// result is Invalid with all errors collected in a NonEmptyList
```

If every element is valid, `sequence` returns a `Valid` containing all values in order.

### NonEmptyList errors (ValidatedNel)

`Validated<String, A>` stores a single error. If you want to accumulate multiple errors with a type-level guarantee that at least one is always present, use `ValidatedNel` — which wraps `Validated<NonEmptyList<E>, A>`:

```java
// ValidatedNel works with Validated<NonEmptyList<E>, A>
Validated<NonEmptyList<String>, String> v1 = ValidatedNel.invalidNel("Name required");
Validated<NonEmptyList<String>, String> v2 = ValidatedNel.invalidNel("Email required");

Validated<NonEmptyList<String>, String> result = ValidatedNel.combine(v1, v2,
    (name, email) -> name + " <" + email + ">"
);

// Access the merged NonEmptyList:
NonEmptyList<String> errors = ((Validated.Invalid<NonEmptyList<String>, String>) result)
    .error();

errors.toList(); // ["Name required", "Email required"]
errors.size();   // 2 — guaranteed >= 1 by the type
```

All errors from every invalid input are merged into one `NonEmptyList`, preserving left-to-right order.

### Custom error types

`ValidatedNel` works with any error type — strings, enums, sealed interfaces, records. Use a sealed interface for typed domain errors:

```java
sealed interface FormError {
    record NameTooShort(int minLength) implements FormError {}
    record InvalidEmail(String reason) implements FormError {}
    record Underage(int age, int minimum) implements FormError {}
}

Validated<NonEmptyList<FormError>, String> validateName(String name) {
    return name.length() < 2
        ? ValidatedNel.invalidNel(new FormError.NameTooShort(2))
        : ValidatedNel.validNel(name);
}

// Combine works out of the box — errors are collected into a NonEmptyList<FormError>
Validated<NonEmptyList<FormError>, User> result = ValidatedNel.combine(
    validateName(name), validateEmail(email), validateAge(age), User::new
);
```

For advanced use cases (custom error combination strategies), see the [`Semigroup`](src/main/java/io/github/jpalmerr/valid4j/Semigroup.java) interface and the [example file](src/example/java/io/github/jpalmerr/valid4j/example/UserRegistrationExample.java).

---

## Validation with I/O

valid4j is an algebra for combining results — it doesn't manage I/O execution. If your validations involve database lookups or HTTP calls, you control the concurrency and pass the results to `combine`.

Java 21 virtual threads make this straightforward:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var emailCheck   = executor.submit(() -> validateEmailUnique(email));    // DB lookup
    var addressCheck = executor.submit(() -> validateAddressExists(addr));   // HTTP call
    var ageCheck     = executor.submit(() -> validateAge(age));              // instant

    return ValidatedNel.combine(emailCheck.get(), addressCheck.get(), ageCheck.get(), User::new);
}
```

All three validations run concurrently on virtual threads. `combine` runs after all three complete. The library doesn't change — the platform handles the concurrency.

This is the same model as Cats' `Validated` in Scala, which is also synchronous. Cats uses `IO.parMapN` for concurrent I/O; Java 21 uses virtual threads.

---

## When to use valid4j

**Use it** when you're validating multiple independent fields and want every error back in one pass:

- **API request validation** — a `POST /users` body has 5 fields, 3 are wrong. Return all 3 errors in one 400 response, not three sequential round trips.
- **Configuration parsing** — load a config file, validate every field, report every problem so the operator fixes them in one go.
- **Domain object construction** — raw input comes in as strings; validate and convert to a typed domain record, or explain exactly what's wrong.
- **Data import / ETL** — validate each row of a CSV. Collect all invalid rows with reasons, process the valid ones.

**Don't use it** for:

- Simple null/format checks already handled by Bean Validation (`@NotNull`, `@Email`)
- Single-field validation where exceptions are fine

### Where it sits in the stack

```
HTTP layer (Bean Validation / @Valid)  →  DTO
    ↓
Domain validation (valid4j)            →  Domain object
    ↓
Business logic
```

Bean Validation catches "is this field present and the right shape." valid4j catches "do these fields *together* form a valid domain object" — with typed errors you control.

---

## Why not Bean Validation?

Bean Validation (`@NotNull`, `@Email`, `@Min`) solves a different problem. It is:

- **Annotation-based** — validation logic lives on the class, not at the call site
- **Tied to Jakarta EE / Spring** — requires a `Validator` infrastructure
- **Untyped errors** — `ConstraintViolation<T>` is generic infrastructure, not your domain type
- **Static** — hard to compose or add runtime context

valid4j is:

- **Code-based** — validation functions are plain Java methods, fully testable without a framework
- **Zero dependencies** — works anywhere Java 21 runs
- **Typed errors** — your error type is whatever you choose (`String`, an enum, a sealed interface)
- **Composable** — combine, andThen, map, sequence — build complex validation from small pieces

The two approaches are not in competition. Bean Validation is good for HTTP layer constraints on Spring DTOs. valid4j is good for domain-level validation where you control the error type and want composability.

---

## Why not Vavr?

Vavr's `Validation<E, A>` is the closest existing type, but:

- **No sealed interfaces or pattern matching** — Vavr predates Java 17. Exhaustive `switch` on `Valid` / `Invalid` is not possible.
- **Large transitive dependency** — Vavr brings a full FP runtime. valid4j is zero dependencies.

Both libraries cap `combine` at arity 8. If you already use Vavr across your codebase, its `Validation` type is reasonable. If you want modern Java 21 idioms and zero dependencies, valid4j fits better.

---

## Inspiration

valid4j is inspired by Cats' `Validated` type from Scala. The core idea - applicative combination with error accumulation — and the deliberate absence of monadic operations on `Validated` mirror Cats' design. You don't need to know Cats to use valid4j; the library is idiomatic Java 21 from start to finish.

---

## Examples

The repo ships runnable examples in `src/example/java/`. Clone the repo and run them with Gradle:

| Example | Command | What it shows |
|---------|---------|---------------|
| [User registration](src/example/java/io/github/jpalmerr/valid4j/example/UserRegistrationExample.java) | `./gradlew runExample` | Combine, andThen, sequence, pattern matching, typed domain errors, custom semigroup |
| [Async validation](src/example/java/io/github/jpalmerr/valid4j/example/AsyncValidationExample.java) | `./gradlew runAsyncExample` | Virtual thread concurrency with I/O validators, sequential vs parallel timing comparison |

See also [examples/async-validation.md](examples/async-validation.md) for an explanation of the async pattern.

---

## For the curious

**How does collecting all errors actually work?**

Normally when you combine two results, you short-circuit: if the first fails, you never check the second.

`combine` works differently. Before checking whether any input failed, it runs all the validations. Then it inspects every result:

- All valid? Call the mapper with all the values.
- Any invalid? Collect every error from every invalid input using the `Semigroup`. Ignore the valid values. Never call the mapper.

This is why `combine` can report three errors at once when three fields fail. Each validation runs independently and the errors are gathered at the end.

The key rule: `combine` is for independent validations that do not depend on each other's results. `andThen` is for dependent steps where step 2 uses the output of step 1.

---

## License

Apache 2.0. See [LICENSE](LICENSE).
