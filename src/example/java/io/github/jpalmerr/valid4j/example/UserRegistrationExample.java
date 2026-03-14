package io.github.jpalmerr.valid4j.example;

import io.github.jpalmerr.valid4j.NonEmptyList;
import io.github.jpalmerr.valid4j.Semigroup;
import io.github.jpalmerr.valid4j.Validated;
import io.github.jpalmerr.valid4j.ValidatedNel;
import java.util.List;

/**
 * End-to-end example: user registration with typed domain errors.
 *
 * <p>Demonstrates:
 *
 * <ul>
 *   <li>Defining validation functions that return {@code Validated}
 *   <li>Combining independent validations (error accumulation)
 *   <li>Typed domain errors via sealed interfaces
 *   <li>Pattern matching on results
 *   <li>Sequential validation via {@code andThen}
 *   <li>Batch validation via {@code sequence}
 *   <li>Custom semigroup for advanced error types
 * </ul>
 */
public class UserRegistrationExample {

  // -------------------------------------------------------------------------
  // Domain types
  // -------------------------------------------------------------------------

  record User(String name, String email, int age) {}

  sealed interface RegistrationError {
    record NameRequired() implements RegistrationError {}

    record NameTooShort(int minLength) implements RegistrationError {}

    record EmailRequired() implements RegistrationError {}

    record EmailInvalid(String reason) implements RegistrationError {}

    record EmailBlocked(String domain) implements RegistrationError {}

    record AgeTooYoung(int age, int minimum) implements RegistrationError {}

    record AgeTooOld(int age, int maximum) implements RegistrationError {}
  }

  // -------------------------------------------------------------------------
  // 1. Basic: individual field validation
  // -------------------------------------------------------------------------

  static Validated<NonEmptyList<RegistrationError>, String> validateName(String name) {
    if (name == null || name.isBlank()) {
      return ValidatedNel.invalidNel(new RegistrationError.NameRequired());
    }
    if (name.trim().length() < 2) {
      return ValidatedNel.invalidNel(new RegistrationError.NameTooShort(2));
    }
    return ValidatedNel.validNel(name.trim());
  }

  static Validated<NonEmptyList<RegistrationError>, String> validateEmail(String email) {
    if (email == null || email.isBlank()) {
      return ValidatedNel.invalidNel(new RegistrationError.EmailRequired());
    }
    if (!email.contains("@")) {
      return ValidatedNel.invalidNel(new RegistrationError.EmailInvalid("must contain @"));
    }
    return ValidatedNel.validNel(email.trim().toLowerCase());
  }

  static Validated<NonEmptyList<RegistrationError>, Integer> validateAge(int age) {
    if (age < 18) {
      return ValidatedNel.invalidNel(new RegistrationError.AgeTooYoung(age, 18));
    }
    if (age > 150) {
      return ValidatedNel.invalidNel(new RegistrationError.AgeTooOld(age, 150));
    }
    return ValidatedNel.validNel(age);
  }

  // -------------------------------------------------------------------------
  // 2. Combining validations — all errors collected in one pass
  // -------------------------------------------------------------------------

  static Validated<NonEmptyList<RegistrationError>, User> registerUser(
      String name, String email, int age) {
    return ValidatedNel.combine(
        validateName(name), validateEmail(email), validateAge(age), User::new);
  }

  // -------------------------------------------------------------------------
  // 3. Sequential validation — when step 2 depends on step 1
  // -------------------------------------------------------------------------

  static Validated<NonEmptyList<RegistrationError>, String> validateEmailWithBlocklist(
      String rawEmail) {
    // First: validate the format (accumulating).
    // Then: check business rule (sequential — only runs if format is valid).
    return validateEmail(rawEmail)
        .andThen(
            email -> {
              String domain = email.substring(email.indexOf("@") + 1);
              if (domain.equals("blocked.com")) {
                return ValidatedNel.invalidNel(new RegistrationError.EmailBlocked(domain));
              }
              return ValidatedNel.validNel(email);
            });
  }

  // -------------------------------------------------------------------------
  // 4. Batch validation — validate a list of items
  // -------------------------------------------------------------------------

  static Validated<NonEmptyList<RegistrationError>, List<Integer>> validateAllAges(
      List<Integer> ages) {
    List<Validated<NonEmptyList<RegistrationError>, Integer>> validated =
        ages.stream().map(UserRegistrationExample::validateAge).toList();
    return ValidatedNel.sequence(validated);
  }

  // -------------------------------------------------------------------------
  // 5. Pattern matching — handling results
  // -------------------------------------------------------------------------

  static String formatResult(Validated<NonEmptyList<RegistrationError>, User> result) {
    return switch (result) {
      case Validated.Valid<NonEmptyList<RegistrationError>, User>(var user) ->
          "Registered: " + user.name() + " (" + user.email() + "), age " + user.age();

      case Validated.Invalid<NonEmptyList<RegistrationError>, User>(var errors) -> {
        StringBuilder sb = new StringBuilder("Registration failed:\n");
        for (RegistrationError error : errors) {
          sb.append("  - ").append(describeError(error)).append("\n");
        }
        yield sb.toString().stripTrailing();
      }
    };
  }

  static String describeError(RegistrationError error) {
    return switch (error) {
      case RegistrationError.NameRequired() -> "Name is required";
      case RegistrationError.NameTooShort(var min) ->
          "Name must be at least " + min + " characters";
      case RegistrationError.EmailRequired() -> "Email is required";
      case RegistrationError.EmailInvalid(var reason) -> "Invalid email: " + reason;
      case RegistrationError.EmailBlocked(var domain) -> "Email domain blocked: " + domain;
      case RegistrationError.AgeTooYoung(var age, var min) ->
          "Age " + age + " is below minimum " + min;
      case RegistrationError.AgeTooOld(var age, var max) ->
          "Age " + age + " exceeds maximum " + max;
    };
  }

  // -------------------------------------------------------------------------
  // 6. Advanced: custom Semigroup for a different error type
  // -------------------------------------------------------------------------

  // Sometimes you want errors as plain strings with a custom combination strategy.
  // Define a Semigroup to control how two errors merge:

  static final Semigroup<String> JOIN_ERRORS = (a, b) -> a + "; " + b;

  static Validated<String, String> validateNameSimple(String name) {
    if (name == null || name.isBlank()) return Validated.invalid("Name is required");
    if (name.trim().length() < 2) return Validated.invalid("Name too short");
    return Validated.valid(name.trim());
  }

  static Validated<String, String> validateEmailSimple(String email) {
    if (email == null || email.isBlank()) return Validated.invalid("Email is required");
    if (!email.contains("@")) return Validated.invalid("Email must contain @");
    return Validated.valid(email.trim().toLowerCase());
  }

  static Validated<String, User> registerUserSimple(String name, String email, int age) {
    // With raw Validated.combine, you pass the semigroup explicitly:
    return Validated.combine(
        validateNameSimple(name),
        validateEmailSimple(email),
        JOIN_ERRORS,
        (n, e) -> new User(n, e, age));
  }

  // -------------------------------------------------------------------------
  // Main — run all examples
  // -------------------------------------------------------------------------

  public static void main(String[] args) {
    System.out.println("=== 1. All fields valid ===");
    System.out.println(formatResult(registerUser("Alice", "alice@example.com", 30)));

    System.out.println();
    System.out.println("=== 2. All fields invalid (errors accumulated) ===");
    System.out.println(formatResult(registerUser("", "not-an-email", 12)));

    System.out.println();
    System.out.println("=== 3. Sequential validation (blocked email) ===");
    var emailResult = validateEmailWithBlocklist("user@blocked.com");
    String emailMessage =
        emailResult.fold(
            errors -> "Blocked: " + describeError(errors.head()), email -> "Accepted: " + email);
    System.out.println(emailMessage);

    System.out.println();
    System.out.println("=== 4. Batch validation ===");
    var agesResult = validateAllAges(List.of(25, 30, 12, 200));
    String agesMessage =
        agesResult.fold(
            errors ->
                "Invalid ages: "
                    + errors.toList().stream().map(UserRegistrationExample::describeError).toList(),
            ages -> "All valid: " + ages);
    System.out.println(agesMessage);

    System.out.println();
    System.out.println("=== 5. Transforming results ===");
    var mapped =
        registerUser("Bob", "bob@example.com", 25)
            .map(user -> user.name().toUpperCase())
            .mapError(errors -> errors.map(UserRegistrationExample::describeError));
    System.out.println(mapped);

    System.out.println();
    System.out.println("=== 6. Custom semigroup (string errors) ===");
    var simple = registerUserSimple("", "bad", 25);
    String simpleMessage = simple.fold(errors -> "Errors: " + errors, user -> "OK: " + user);
    System.out.println(simpleMessage);
  }
}
