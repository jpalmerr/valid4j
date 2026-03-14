# Batch validation with sequence

## The problem

`combine` works when you know the number of validators at compile time:

```java
ValidatedNel.combine(validateName(name), validateEmail(email), validateAge(age), User::new);
```

But sometimes the number of items is dynamic - every row in a CSV, every ID in a request payload, every rule in a configurable pipeline.
You can't write a `combine` call when you don't know how many inputs there are.

## sequence

`sequence` flips the structure:

```
List<Validated<E, A>>  →  Validated<E, List<A>>
```

A list of individual results becomes a single result containing all values or all errors.

```java
List<Validated<NonEmptyList<String>, Integer>> results = ages.stream()
    .map(age -> validateAge(age))
    .toList();

Validated<NonEmptyList<String>, List<Integer>> combined = ValidatedNel.sequence(results);
```

If every element is `Valid`, you get `Valid(List<Integer>)` with all values in order. If any are `Invalid`, you get a single `Invalid` with every error accumulated - same as `combine`, just for a dynamic number of inputs.

## Example: validating a batch of ages

```java
static Validated<NonEmptyList<String>, Integer> validateAge(int age) {
    if (age < 18) return ValidatedNel.invalidNel("Age " + age + ": must be at least 18");
    if (age > 150) return ValidatedNel.invalidNel("Age " + age + ": seems unrealistic");
    return ValidatedNel.validNel(age);
}

List<Integer> input = List.of(25, 30, -1, 200);

List<Validated<NonEmptyList<String>, Integer>> validated = input.stream()
    .map(age -> validateAge(age))
    .toList();

Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(validated);
// Invalid with: ["Age -1: must be at least 18", "Age 200: seems unrealistic"]
```

All four ages are validated. The two failures are collected into a single `NonEmptyList`. The two successes are discarded - if any input fails, the whole batch fails.

## Example: validating CSV rows

```java
record Row(String name, String email, int age) {}

static Validated<NonEmptyList<String>, User> validateRow(Row row) {
    return ValidatedNel.combine(
        validateName(row.name()),
        validateEmail(row.email()),
        validateAge(row.age()),
        User::new
    );
}

List<Row> csvRows = parseCsv(file);

List<Validated<NonEmptyList<String>, User>> validated = csvRows.stream()
    .map(row -> validateRow(row))
    .toList();

Validated<NonEmptyList<String>, List<User>> result = ValidatedNel.sequence(validated);
```

Each row is validated independently with `combine` (accumulating errors within the row). Then `sequence` accumulates errors across all rows. If row 3 has 2 errors and row 7 has 1 error, the result holds all 3 errors.

## When to use sequence vs combine

- **`combine`** — fixed number of independent validators known at compile time. "Validate these 3 fields."
- **`sequence`** — dynamic list of validators determined at runtime. "Validate every item in this collection."
