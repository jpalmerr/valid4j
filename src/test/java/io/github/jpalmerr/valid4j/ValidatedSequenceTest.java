package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidatedSequenceTest {

  // -------------------------------------------------------------------------
  // ValidatedNel.sequence — primary API (no semigroup needed)
  // -------------------------------------------------------------------------

  @Test
  void sequence_emptyList_returnsValidEmptyList() {
    List<Validated<NonEmptyList<String>, Integer>> inputs = List.of();

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, List<Integer>>) result).value()).isEmpty();
  }

  @Test
  void sequence_allValid_returnsValidListOfValues() {
    List<Validated<NonEmptyList<String>, Integer>> inputs =
        List.of(ValidatedNel.validNel(1), ValidatedNel.validNel(2), ValidatedNel.validNel(3));

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, List<Integer>>) result).value())
        .containsExactly(1, 2, 3);
  }

  @Test
  void sequence_allInvalid_accumulatesAllErrors() {
    List<Validated<NonEmptyList<String>, Integer>> inputs =
        List.of(
            ValidatedNel.invalidNel("error1"),
            ValidatedNel.invalidNel("error2"),
            ValidatedNel.invalidNel("error3"));

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, List<Integer>>) result).error();
    assertThat(error.toList()).containsExactly("error1", "error2", "error3");
  }

  @Test
  void sequence_mixed_accumulatesErrorsFromInvalidOnly() {
    List<Validated<NonEmptyList<String>, Integer>> inputs =
        List.of(
            ValidatedNel.validNel(1),
            ValidatedNel.invalidNel("error1"),
            ValidatedNel.validNel(3),
            ValidatedNel.invalidNel("error2"));

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, List<Integer>>) result).error();
    assertThat(error.toList()).containsExactly("error1", "error2");
  }

  @Test
  void sequence_singleValid_returnsValidSingletonList() {
    List<Validated<NonEmptyList<String>, Integer>> inputs = List.of(ValidatedNel.validNel(42));

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, List<Integer>>) result).value())
        .containsExactly(42);
  }

  @Test
  void sequence_singleInvalid_returnsInvalidWithErrors() {
    List<Validated<NonEmptyList<String>, Integer>> inputs =
        List.of(ValidatedNel.invalidNel("only error"));

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, List<Integer>>) result).error();
    assertThat(error.toList()).containsExactly("only error");
  }

  @Test
  void sequence_preservesErrorOrder_leftToRight() {
    List<Validated<NonEmptyList<String>, Integer>> inputs =
        List.of(ValidatedNel.invalidNel("first", "second"), ValidatedNel.invalidNel("third"));

    Validated<NonEmptyList<String>, List<Integer>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, List<Integer>>) result).error();
    assertThat(error.toList()).containsExactly("first", "second", "third");
  }

  @Test
  void sequence_preservesValueOrder_leftToRight() {
    List<Validated<NonEmptyList<String>, String>> inputs =
        List.of(ValidatedNel.validNel("a"), ValidatedNel.validNel("b"), ValidatedNel.validNel("c"));

    Validated<NonEmptyList<String>, List<String>> result = ValidatedNel.sequence(inputs);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, List<String>>) result).value())
        .containsExactly("a", "b", "c");
  }

  @Test
  void sequence_nullList_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> ValidatedNel.sequence(null))
        .withMessage("values must not be null");
  }

  // -------------------------------------------------------------------------
  // Validated.sequence with explicit Semigroup — proves the base API works
  // -------------------------------------------------------------------------

  @Test
  void validatedSequence_withExplicitSemigroup_concatenatesErrors() {
    Semigroup<String> joinSemigroup = (a, b) -> a + ", " + b;

    List<Validated<String, Integer>> inputs =
        List.of(
            Validated.valid(1),
            Validated.invalid("bad value"),
            Validated.valid(3),
            Validated.invalid("out of range"));

    Validated<String, List<Integer>> result = Validated.sequence(inputs, joinSemigroup);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, List<Integer>>) result).error())
        .isEqualTo("bad value, out of range");
  }
}
