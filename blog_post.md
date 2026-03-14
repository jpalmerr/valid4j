# Valid4j: Applicative validation for Java 21+

## Why I built Valid4J

As a Scala dev, now working with Java in production, I'm eagerly watching functional programming support arrive in Java 21.
Java 21 still does not have higher-kinded types, implicits, or the kind of typeclass based ergonomics that Cats relies on. But it does now have records, sealed interfaces, and pattern matching over `switch`.
That is enough to make some functional patterns feel native rather than bolted on.

There are definitely patterns and capabilities I miss from Scala. One such pattern is error accumulation. This is particularly useful
for systems that are checking for correctness. A basic example may be a sign-in form with multiple fields that need validating. 
In this case, you want a user to know all their entry errors in one response, not have to fix one by one. 
A production example from a previous job was a service that checked the availability of certain television content. It would collect 
metadata from multiple microservices, meaning there could be a number of reasons a show was unavailable. 

This is not an unsolved problem. Typelevel's [Validated](https://typelevel.org/cats/datatypes/validated.html) provides an excellent 
FP pattern to handle this. 

This can be handled in Java too. You can accumulate errors into a `List<String>`, however you lose type safety. Libraries like Vavr offer a Validation
type, but they predate sealed interfaces (the errors aren't exhaustively matchable) and bring a large transitive dependency. Bean Validation handles the HTTP layer,
but it's annotation driven, and coupled to a framework. 

I wanted something narrower: a pure FP approach to error accumulation, using modern Java 21 features, with zero
dependencies. A `Validated<E, A>` type that collects all errors in one pass, lets you pattern match on the result,
and stays out of your dependency tree. Inspired by Typelevel, [Valid4J](https://github.com/jpalmerr/valid4j) attempts to plug that gap.

## Why Java 21

None of this works because Java suddenly gained typeclasses. It works because Java's data modelling got better:

- `record` makes small immutable data carriers cheap
- `sealed interface` lets you model closed algebraic data types directly
- pattern matching on `switch` makes those types ergonomic to consume

## What the type gives you

I want the return type itself to tell me whether I succeeded or failed.

`Validated<E, A>` is a sealed interface with exactly two cases:

- `Valid`, holding a value of type `A`
- `Invalid`, holding an error of type `E`

A validation function doesn't throw. It returns a result that encodes success or failure in the type system.

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

Each function is standalone. Just a method that takes input and returns a typed result.

So how to handle a user field input of their name, email and age? We can collect the validators with a `.combine` call.

```java
record User(String name, String email, int age) {}

Validated<NonEmptyList<String>, User> result = ValidatedNel.combine(
    validateName(""),           // Invalid: "Name is required"
    validateEmail("not-valid"), // Invalid: "Email must contain @"
    validateAge(15),            // Invalid: "Must be at least 18 years old"
    User::new
);
```

Our result? An `Invalid` holding all three errors. 

If everything passes, the mapper constructs the domain object: `Valid(User("Alice", "alice@example.com", 30))`.

Since `Validated` is a sealed interface, Java 21 gives us exhaustive pattern matching capability on `switch`. 
The compiler will force you to handle both cases.

Errors don't have to be strings. Model your errors as a sealed hierarchy and get exhaustive pattern matching on the error side too.
That means your API layer can choose how to render messages, map error codes, or build response payloads without collapsing 
your type system before the boundary of your code.

```java
String describeError(RegistrationError error) {
    return switch (error) {
        case RegistrationError.NameTooShort(var min) ->
            "Name must be at least " + min + " characters";
        case RegistrationError.EmailInvalid(var reason) ->
            "Invalid email: " + reason;
        case RegistrationError.AgeTooYoung(var age, var min) ->
            "Age " + age + " is below minimum " + min;
    };
}
```

We've not had to parse strings. It's exhaustive, typed.

## How we achieve it

### `Validated` is applicative

valid4j ships a sealed interface: `Validated<E, A>`.

`Validated` is applicative rather than monadic. It provides `combine`, not `flatMap`, because our underlying thesis here is **independent** validation with **error accumulation**.

```java
Validated<NonEmptyList<String>, User> result = ValidatedNel.combine(
        validateName(name),
        validateEmail(email),
        validateAge(age),
        User::new
);
```

## Semigroup

When two `Invalid` values are combined, the library needs to know *how* to merge their errors. This is the job of a semigroup.

```java
@FunctionalInterface
public interface Semigroup<A> {
    A combine(A x, A y);
}
```

The only contract: associativity — `combine(x, combine(y, z))` equals `combine(combine(x, y), z)`. Order of grouping doesn't matter.

Here's the arity-2 implementation that acts as the foundation for the rest:

```java
static <E, A, B, R> Validated<E, R> combine(
        Validated<E, A> v1, Validated<E, B> v2,
        Semigroup<E> semigroup,
        BiFunction<A, B, R> mapper) {
    return switch (v1) {
        case Valid(var a) -> switch (v2) {
            case Valid(var b)    -> new Valid<>(mapper.apply(a, b));
            case Invalid(var e) -> new Invalid<>(e);
        };
        case Invalid(var e1) -> switch (v2) {
            case Valid(var _)    -> new Invalid<>(e1);
            case Invalid(var e2) -> new Invalid<>(semigroup.combine(e1, e2));
        };
    };
}
```

The semigroup is only invoked when both inputs are invalid — the one case where errors need to merge.

For the common case, `NonEmptyList` is the error type. `ValidatedNel.combine` bakes this in — you never pass the semigroup:

```java
ValidatedNel.combine(v1, v2, v3, User::new);
```

The general `Validated.combine` is there when you need a custom error strategy:

```java
Semigroup<String> join = (a, b) -> a + "; " + b;
Validated.combine(v1, v2, join, User::new);
```

## The boundary

Sometimes you need sequential logic *within* a validation pipeline. `andThen` handles this directly. For example:

```java
Validated<NonEmptyList<String>, String> result = validateEmail(rawEmail)
    .andThen(email ->
        email.endsWith("@blocked.com")
            ? ValidatedNel.invalidNel("Domain is blocked")
            : ValidatedNel.validNel(email));
```

The implementation is one line:

```java
default <B> Validated<E, B> andThen(Function<A, Validated<E, B>> f) {
    return fold(e -> invalid(e), f);
}
```

If valid, apply the function. If invalid, propagate.

## Limitations

###  Implicits 

In Cats, the semigroup is resolved implicitly by the compiler. You declare it once, and it's threaded through every call site automatically. The `Semigroup` typeclass participates in a larger Cats algebra - `Monoid`, `Functor`, `Applicative` - with instances provided out of the box and composed via implicit derivation.
Java doesn't have implicits. valid4j ships exactly two semigroup constructors: `Semigroup.nonEmptyList()` for the common case and `Semigroup.of(combiner)` as an escape hatch. For anything else, a lambda does the job.

I'm confident in the algebra, but in Java I can't make it invisible. The trade-off therefore is rather than shipping dozens of instances nobody would discover, we keep `Semigroup` minimal and let `ValidatedNel` handle the common case.

### Arity and the limits of Java generics

`Validated.combine` is overloaded from arity 2 through 8 — that's 7 explicit methods, each with a different function type (`BiFunction`, `Function3`, ... `Function8`). This is a direct consequence of Java lacking higher-kinded types.

Cats has the same problem. It generates `map2` through `map22` — 21 overloads. But Scala's `mapN` syntax, backed by `Semigroupal` and tuple derivation, hides the arity from the call site entirely. The user writes `(v1, v2, v3).mapN(User.apply)` and the compiler resolves the rest.

Java can't do that. There's no way to abstract over "a function of N arguments" without explicit overloads per arity, it's the cost of applicative composition in a language without higher-kinded types.

### Pattern matching ergonomics

The code samples in this post simplify the pattern matching syntax. In Scala you write `case Valid(user) =>` and the compiler infers the types. Java's pattern matching on generic records requires type witnesses:

```java
case Validated.Valid<NonEmptyList<RegistrationError>, User>(var user) ->
    "Registered: " + user.name();
case Validated.Invalid<NonEmptyList<RegistrationError>, User>(var errors) ->
    "Failed: " + errors.toList();
```

The sealed interface is guaranteeing exhaustiveness, which is the real win. But I do miss the elegance of Scala. 

### The associativity honour system

This is my favourite finding, exactly the kind of learning I set out to find when I began this project.

`Semigroup` has one contract: associativity. `combine(x, combine(y, z))` must equal `combine(combine(x, y), z)`. 
If the semigroup is associative, the result is the same regardless of how the library batches the fold. If it isn't associative, the accumulated errors silently depend on an implementation detail.

For our `ValidatedNel` case, the library owns the semigroup. We implement it, prove it with a property test, and hide it from the users call site.
The risk lives in the escape hatch: `Semigroup.of(combiner)`. A user could pass a combiner that breaks this contract. The library would produce subtly different results depending on internal accumulation order.

But when looking to Cats for inspiration on how to enforce this, I realised **Cats can't enforce this either**. Scala's type system cannot prove that a function is associative. 
The difference is the ecosystem, they ship lawful instances for common types so users rarely have to write their own semigroups and Cats provides [laws](https://typelevel.org/cats/typeclasses/lawtesting.html) for users to property test against.

As mentioned above regarding implicits, there's no way to say "here's how String combines" and have the compiler find it at call sites.
Which left me choosing a narrower approach. valid4J ships one verified semigroup, one wrapper class, and relies on documentation to inform a user customizing with `Semigroup.of`. 

# Summary

The theory behind valid4j reduces to three concepts:

1. **Applicative** (independence) — `combine` runs all validations, then inspects results.

2. **Semigroup** (merging) — when two `Invalid` values meet, the semigroup decides how their errors become one error. `ValidatedNel` handles this automatically for the common `NonEmptyList` case; `Validated.combine` exposes the full algebra for custom error types.

3. **Sequencing** — `andThen` for dependent steps. `Validated` deliberately has no `flatMap`, but `andThen` provides sequencing when you need it.