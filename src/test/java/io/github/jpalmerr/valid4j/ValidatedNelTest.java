package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ValidatedNelTest {

  // -------------------------------------------------------------------------
  // validNel
  // -------------------------------------------------------------------------

  @Test
  void validNel_createsValidInstance() {
    Validated<NonEmptyList<String>, Integer> result = ValidatedNel.validNel(42);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(42);
  }

  // -------------------------------------------------------------------------
  // invalidNel — single error
  // -------------------------------------------------------------------------

  @Test
  void invalidNel_singleError_createsInvalidWithNel() {
    Validated<NonEmptyList<String>, Integer> result = ValidatedNel.invalidNel("field required");

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("field required");
  }

  @Test
  void invalidNel_singleError_nelHasCorrectStructure() {
    Validated<NonEmptyList<String>, Integer> result = ValidatedNel.invalidNel("bad input");

    NonEmptyList<String> nel = ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(nel.size()).isEqualTo(1);
    assertThat(nel.head()).isEqualTo("bad input");
    assertThat(nel.tail()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // invalidNel — multiple errors (varargs)
  // -------------------------------------------------------------------------

  @Test
  void invalidNel_multipleErrors_createsInvalidWithAllErrorsInNel() {
    Validated<NonEmptyList<String>, Integer> result =
        ValidatedNel.invalidNel("too short", "invalid chars");

    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("too short", "invalid chars");
  }

  // -------------------------------------------------------------------------
  // errors() extractor
  // -------------------------------------------------------------------------

  @Test
  void errors_onValid_returnsEmpty() {
    Validated<NonEmptyList<String>, Integer> valid = ValidatedNel.validNel(99);

    Optional<NonEmptyList<String>> result = ValidatedNel.errors(valid);

    assertThat(result).isEmpty();
  }

  @Test
  void errors_onInvalid_returnsNel() {
    Validated<NonEmptyList<String>, Integer> invalid = ValidatedNel.invalidNel("field required");

    Optional<NonEmptyList<String>> result = ValidatedNel.errors(invalid);

    assertThat(result).isPresent();
    assertThat(result.get().toList()).containsExactly("field required");
  }

  @Test
  void errors_onInvalidWithMultipleErrors_returnsFullNel() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("e1");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("e2", "e3");
    Validated<NonEmptyList<String>, Integer> combined =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    Optional<NonEmptyList<String>> result = ValidatedNel.errors(combined);

    assertThat(result).isPresent();
    assertThat(result.get().toList()).containsExactly("e1", "e2", "e3");
  }

  // -------------------------------------------------------------------------
  // combine2
  // -------------------------------------------------------------------------

  @Test
  void combine2_allValid_appliesMapper() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(10);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(20);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(30);
  }

  @Test
  void combine2_allInvalid_mergesNelErrors() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("error1");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("error2");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("error1", "error2");
  }

  @Test
  void combine2_mixed_collectsOnlyInvalidErrors() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(10);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("v2 error");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("v2 error");
  }

  // -------------------------------------------------------------------------
  // combine3
  // -------------------------------------------------------------------------

  @Test
  void combine3_allValid_appliesMapper() {
    Validated<NonEmptyList<String>, String> v1 = ValidatedNel.validNel("John");
    Validated<NonEmptyList<String>, String> v2 = ValidatedNel.validNel("john@example.com");
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.validNel(30);

    Validated<NonEmptyList<String>, String> result =
        Validated.combine(
            v1,
            v2,
            v3,
            Semigroup.nonEmptyList(),
            (name, email, age) -> name + "/" + email + "/" + age);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, String>) result).value())
        .isEqualTo("John/john@example.com/30");
  }

  @Test
  void combine3_allInvalid_mergesAllNelErrors() {
    Validated<NonEmptyList<String>, String> v1 = ValidatedNel.invalidNel("name required");
    Validated<NonEmptyList<String>, String> v2 = ValidatedNel.invalidNel("email invalid");
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.invalidNel("age out of range");

    Validated<NonEmptyList<String>, String> result =
        Validated.combine(
            v1,
            v2,
            v3,
            Semigroup.nonEmptyList(),
            (name, email, age) -> name + "/" + email + "/" + age);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error = ((Validated.Invalid<NonEmptyList<String>, String>) result).error();
    assertThat(error.toList())
        .containsExactly("name required", "email invalid", "age out of range");
  }

  // -------------------------------------------------------------------------
  // combine4 — confirm pattern works at higher arity
  // -------------------------------------------------------------------------

  @Test
  void combine4_allValid_appliesMapper() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(1);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(2);
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.validNel(3);
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.validNel(4);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, v3, v4, Semigroup.nonEmptyList(), (a, b, c, d) -> a + b + c + d);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(10);
  }

  @Test
  void combine4_allInvalid_mergesAllNelErrors() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("e1");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("e2");
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.invalidNel("e3");
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.invalidNel("e4");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, v3, v4, Semigroup.nonEmptyList(), (a, b, c, d) -> a + b + c + d);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("e1", "e2", "e3", "e4");
  }

  // -------------------------------------------------------------------------
  // Multi-error NEL — errors from invalidNel(first, ...rest) are all merged
  // -------------------------------------------------------------------------

  @Test
  void combine_multipleErrorsPerNel_allMerged() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("e1", "e2");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("e3", "e4", "e5");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("e1", "e2", "e3", "e4", "e5");
  }

  // -------------------------------------------------------------------------
  // ValidatedNel.combine — convenience (no semigroup parameter)
  // -------------------------------------------------------------------------

  @Test
  void nelCombine2_allValid_appliesMapper() {
    var v1 = ValidatedNel.<String, Integer>validNel(10);
    var v2 = ValidatedNel.<String, Integer>validNel(20);

    var result = ValidatedNel.combine(v1, v2, Integer::sum);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(30);
  }

  @Test
  void nelCombine2_allInvalid_accumulatesErrors() {
    var v1 = ValidatedNel.<String, Integer>invalidNel("e1");
    var v2 = ValidatedNel.<String, Integer>invalidNel("e2");

    var result = ValidatedNel.combine(v1, v2, Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<NonEmptyList<String>, Integer>) result).error().toList())
        .containsExactly("e1", "e2");
  }

  @Test
  void nelCombine3_allValid_appliesMapper() {
    var v1 = ValidatedNel.<String, String>validNel("Alice");
    var v2 = ValidatedNel.<String, String>validNel("alice@example.com");
    var v3 = ValidatedNel.<String, Integer>validNel(30);

    var result = ValidatedNel.combine(v1, v2, v3, (name, email, age) -> name + "/" + age);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, String>) result).value())
        .isEqualTo("Alice/30");
  }

  @Test
  void nelCombine3_allInvalid_accumulatesErrors() {
    var v1 = ValidatedNel.<String, String>invalidNel("e1");
    var v2 = ValidatedNel.<String, String>invalidNel("e2");
    var v3 = ValidatedNel.<String, Integer>invalidNel("e3");

    var result = ValidatedNel.combine(v1, v2, v3, (a, b, c) -> "");

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<NonEmptyList<String>, String>) result).error().toList())
        .containsExactly("e1", "e2", "e3");
  }

  // -------------------------------------------------------------------------
  // fromValidated
  // -------------------------------------------------------------------------

  @Test
  void fromValidated_valid_returnsValidNel() {
    Validated<String, Integer> valid = Validated.valid(42);

    Validated<NonEmptyList<String>, Integer> result = ValidatedNel.fromValidated(valid);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(42);
  }

  @Test
  void fromValidated_invalid_wrapsErrorInNel() {
    Validated<String, Integer> invalid = Validated.invalid("field required");

    Validated<NonEmptyList<String>, Integer> result = ValidatedNel.fromValidated(invalid);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("field required");
  }

  @Test
  void fromValidated_null_throwsNPE() {
    assertThatNullPointerException()
        .isThrownBy(() -> ValidatedNel.fromValidated(null))
        .withMessage("validated must not be null");
  }
}
