package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Real-world examples demonstrating typical valid4j usage patterns with {@link ValidatedNel}.
 *
 * <p>These tests serve as both correctness checks and documentation. The key pattern: combine
 * independent field validations applicatively — all errors collected, none lost.
 */
class RealWorldExampleTest {

  // --- Domain types ---
  record User(String name, String email, int age) {}

  // --- Validation functions ---
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
    if (age < 0) return ValidatedNel.invalidNel("Age cannot be negative");
    if (age < 18) return ValidatedNel.invalidNel("Must be at least 18 years old");
    if (age > 150) return ValidatedNel.invalidNel("Age seems unrealistic");
    return ValidatedNel.validNel(age);
  }

  @Test
  void userRegistration_allFieldsValid_returnsUser() {
    Validated<NonEmptyList<String>, User> result =
        Validated.combine(
            validateName("Alice"),
            validateEmail("alice@example.com"),
            validateAge(30),
            Semigroup.nonEmptyList(),
            User::new);

    assertThat(result.isValid()).isTrue();
    User user = ((Validated.Valid<NonEmptyList<String>, User>) result).value();
    assertThat(user.name()).isEqualTo("Alice");
    assertThat(user.email()).isEqualTo("alice@example.com");
    assertThat(user.age()).isEqualTo(30);
  }

  @Test
  void userRegistration_allFieldsInvalid_collectsAllErrors() {
    Validated<NonEmptyList<String>, User> result =
        Validated.combine(
            validateName(""),
            validateEmail("not-an-email"),
            validateAge(-5),
            Semigroup.nonEmptyList(),
            User::new);

    assertThat(result.isInvalid()).isTrue();
    NonEmptyList<String> error = ((Validated.Invalid<NonEmptyList<String>, User>) result).error();
    assertThat(error.toList())
        .containsExactly("Name is required", "Email must contain @", "Age cannot be negative");
  }

  @Test
  void userRegistration_someFieldsInvalid_collectsOnlyErrors() {
    Validated<NonEmptyList<String>, User> result =
        Validated.combine(
            validateName("Bob"),
            validateEmail(""),
            validateAge(15),
            Semigroup.nonEmptyList(),
            User::new);

    assertThat(result.isInvalid()).isTrue();
    NonEmptyList<String> error = ((Validated.Invalid<NonEmptyList<String>, User>) result).error();
    assertThat(error.toList())
        .containsExactly("Email is required", "Must be at least 18 years old");
  }

  @Test
  void userRegistration_withPatternMatching() {
    var result =
        Validated.combine(
            validateName("Charlie"),
            validateEmail("charlie@test.com"),
            validateAge(25),
            Semigroup.nonEmptyList(),
            User::new);

    String message =
        switch (result) {
          case Validated.Valid<NonEmptyList<String>, User>(var user) ->
              "Welcome, " + user.name() + "!";
          case Validated.Invalid<NonEmptyList<String>, User>(var error) ->
              "Fix these: " + String.join(", ", error.toList());
        };

    assertThat(message).isEqualTo("Welcome, Charlie!");
  }

  @Test
  void userRegistration_errorsExtractor_returnsNelOnInvalid() {
    var result =
        Validated.combine(
            validateName(""),
            validateEmail("bad"),
            validateAge(10),
            Semigroup.nonEmptyList(),
            User::new);

    var errors = ValidatedNel.errors(result);

    assertThat(errors).isPresent();
    assertThat(errors.get().toList())
        .containsExactly(
            "Name is required", "Email must contain @", "Must be at least 18 years old");
  }

  @Test
  void sequence_batchValidation() {
    List<Validated<NonEmptyList<String>, Integer>> validations =
        List.of(validateAge(25), validateAge(30), validateAge(-1), validateAge(200));

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(validations);

    assertThat(result.isInvalid()).isTrue();
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, List<Integer>>) result).error();
    assertThat(error.toList()).containsExactly("Age cannot be negative", "Age seems unrealistic");
  }

  @Test
  void validatedNel_guaranteesNonEmptyErrors() {
    var result =
        Validated.combine(
            ValidatedNel.<String, String>invalidNel("Name required"),
            ValidatedNel.<String, String>invalidNel("Email required"),
            Semigroup.nonEmptyList(),
            (name, email) -> name + " <" + email + ">");

    assertThat(result.isInvalid()).isTrue();
    NonEmptyList<String> error = ((Validated.Invalid<NonEmptyList<String>, String>) result).error();
    // The merged NEL contains all errors from both invalids
    assertThat(error.toList()).containsExactly("Name required", "Email required");
  }
}
