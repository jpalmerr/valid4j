# Async validation with valid4j

## The setup

Your registration flow validates three fields. Two require I/O:

```java
// Pure — instant
static Validated<NonEmptyList<String>, String> validateName(String name) {
    if (name == null || name.isBlank()) return ValidatedNel.invalidNel("Name is required");
    return ValidatedNel.validNel(name.trim());
}

// Database lookup — ~400ms
static Validated<NonEmptyList<String>, String> validateEmailUnique(String email) {
    boolean taken = userRepository.existsByEmail(email);
    return taken
        ? ValidatedNel.invalidNel("Email already registered")
        : ValidatedNel.validNel(email);
}

// HTTP call to address verification service — ~300ms
static Validated<NonEmptyList<String>, String> validateAddress(String raw) {
    var result = addressService.verify(raw);
    return result.isValid()
        ? ValidatedNel.validNel(result.normalised())
        : ValidatedNel.invalidNel("Address verification failed: " + result.reason());
}
```

## The problem

If you pass these directly to `combine`, Java evaluates the arguments left-to-right before the method is called:

```java
// Sequential — total time is ~700ms (400 + 300)
ValidatedNel.combine(
    validateName(name),             // instant
    validateEmailUnique(email),     // blocks 400ms, then returns
    validateAddress(address),       // blocks 300ms (only starts after email finishes)
    Registration::new
);
```

The I/O calls run one after another. Not because `combine` is sequential - it treats all inputs independently - but because Java evaluates method arguments eagerly and in order.

## The solution: virtual threads

Submit each validation to a virtual thread executor. They run concurrently. Pass the results to `combine`:

```java
// Concurrent — total time is ~400ms (max of parallel calls)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var nameResult    = executor.submit(() -> validateName(name));
    var emailResult   = executor.submit(() -> validateEmailUnique(email));
    var addressResult = executor.submit(() -> validateAddress(address));

    return ValidatedNel.combine(
        nameResult.get(),
        emailResult.get(),
        addressResult.get(),
        Registration::new
    );
}
```

All three validations start immediately on virtual threads. The `.get()` calls block until each completes — but on Java 21, blocking a virtual thread is cheap (it yields its carrier thread). `combine` runs once all three results are available.

Error accumulation works identically. If the email check fails AND the address check fails, both errors appear in the result — same as the sequential version.

## Why the library doesn't need to change

`combine` takes already-computed `Validated` values. It doesn't care how they were produced - synchronously, on virtual threads, from a cache, from a message queue. The algebra (applicative combination, semigroup error merging) is separate from the execution model.
