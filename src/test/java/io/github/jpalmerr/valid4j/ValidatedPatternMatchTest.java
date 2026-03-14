package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Showcases Java 21 pattern matching with switch on {@link Validated}.
 *
 * <p>Because {@code Validated} is a sealed interface, the compiler enforces exhaustiveness: every
 * switch must handle both {@link Validated.Valid} and {@link Validated.Invalid}.
 */
class ValidatedPatternMatchTest {

  @Test
  void switch_onValid_matchesValidCase() {
    Validated<String, Integer> result = Validated.valid(100);

    String message =
        switch (result) {
          case Validated.Valid<String, Integer>(var value) -> "got value: " + value;
          case Validated.Invalid<String, Integer>(var error) -> "got error: " + error;
        };

    assertThat(message).isEqualTo("got value: 100");
  }

  @Test
  void switch_onInvalid_matchesInvalidCase() {
    Validated<String, Integer> result = Validated.invalid("field required");

    String message =
        switch (result) {
          case Validated.Valid<String, Integer>(var value) -> "got value: " + value;
          case Validated.Invalid<String, Integer>(var error) -> "got error: " + error;
        };

    assertThat(message).isEqualTo("got error: field required");
  }

  @Test
  void switch_exhaustive_coversAllCases() {
    // A switch expression on a sealed interface must cover all permits.
    // This test proves the compiler accepts it without a default branch.
    Validated<String, String> valid = Validated.valid("hello");
    Validated<String, String> invalid = Validated.invalid("oops");

    int validScore = scoreResult(valid);
    int invalidScore = scoreResult(invalid);

    assertThat(validScore).isEqualTo(1);
    assertThat(invalidScore).isEqualTo(0);
  }

  /** Helper that demonstrates a switch expression returning a value — no default needed. */
  private int scoreResult(Validated<String, String> result) {
    return switch (result) {
      case Validated.Valid<String, String>(var value) -> 1;
      case Validated.Invalid<String, String>(var error) -> 0;
    };
  }

  @Test
  void switch_withGuard_appliesAdditionalCondition() {
    // Java 21 when-guards can further refine matched cases.
    Validated<String, Integer> small = Validated.valid(3);
    Validated<String, Integer> large = Validated.valid(50);

    assertThat(describe(small)).isEqualTo("small: 3");
    assertThat(describe(large)).isEqualTo("large: 50");
  }

  private String describe(Validated<String, Integer> result) {
    return switch (result) {
      case Validated.Valid<String, Integer>(var value) when value < 10 -> "small: " + value;
      case Validated.Valid<String, Integer>(var value) -> "large: " + value;
      case Validated.Invalid<String, Integer>(var error) -> "invalid: " + error;
    };
  }

  @Test
  void switch_withNonEmptyListError_accessesNelDirectly() {
    // When using ValidatedNel, the error branch gives us NonEmptyList<E> directly —
    // no need to call .getFirst() on an outer list.
    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            ValidatedNel.invalidNel("name required"),
            ValidatedNel.invalidNel("email required"),
            Semigroup.nonEmptyList(),
            Integer::sum);

    String message =
        switch (result) {
          case Validated.Valid<NonEmptyList<String>, Integer>(var value) -> "valid: " + value;
          case Validated.Invalid<NonEmptyList<String>, Integer>(var error) ->
              "errors: " + String.join(", ", error.toList());
        };

    assertThat(message).isEqualTo("errors: name required, email required");
  }
}
