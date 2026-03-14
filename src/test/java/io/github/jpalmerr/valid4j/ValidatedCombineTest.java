package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ValidatedCombineTest {

  // -------------------------------------------------------------------------
  // Validated.combine2 — primary API for error accumulation
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
  void combine2_allInvalid_accumulatesAllErrors() {
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
  void combine2_firstValidSecondInvalid_returnsSecondError() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(10);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("v2 error");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("v2 error");
  }

  @Test
  void combine2_firstInvalidSecondValid_returnsFirstError() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("v1 error");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(20);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("v1 error");
  }

  // -------------------------------------------------------------------------
  // Validated.combine3
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
  void combine3_allInvalid_accumulatesAllErrors() {
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

  @Test
  void combine3_mixed_accumulatesErrorsFromInvalidOnly() {
    Validated<NonEmptyList<String>, String> v1 = ValidatedNel.validNel("John");
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
    assertThat(error.toList()).containsExactly("email invalid", "age out of range");
  }

  // -------------------------------------------------------------------------
  // Validated.combine4 through combine8 — one happy path each
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
  void combine5_allValid_appliesMapper() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(1);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(2);
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.validNel(3);
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.validNel(4);
    Validated<NonEmptyList<String>, Integer> v5 = ValidatedNel.validNel(5);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            v1, v2, v3, v4, v5, Semigroup.nonEmptyList(), (a, b, c, d, e) -> a + b + c + d + e);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(15);
  }

  @Test
  void combine6_allValid_appliesMapper() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(1);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(2);
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.validNel(3);
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.validNel(4);
    Validated<NonEmptyList<String>, Integer> v5 = ValidatedNel.validNel(5);
    Validated<NonEmptyList<String>, Integer> v6 = ValidatedNel.validNel(6);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            Semigroup.nonEmptyList(),
            (a, b, c, d, e, f) -> a + b + c + d + e + f);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(21);
  }

  @Test
  void combine7_allValid_appliesMapper() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(1);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(2);
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.validNel(3);
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.validNel(4);
    Validated<NonEmptyList<String>, Integer> v5 = ValidatedNel.validNel(5);
    Validated<NonEmptyList<String>, Integer> v6 = ValidatedNel.validNel(6);
    Validated<NonEmptyList<String>, Integer> v7 = ValidatedNel.validNel(7);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            v7,
            Semigroup.nonEmptyList(),
            (a, b, c, d, e, f, g) -> a + b + c + d + e + f + g);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(28);
  }

  @Test
  void combine8_allValid_appliesMapper() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.validNel(1);
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(2);
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.validNel(3);
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.validNel(4);
    Validated<NonEmptyList<String>, Integer> v5 = ValidatedNel.validNel(5);
    Validated<NonEmptyList<String>, Integer> v6 = ValidatedNel.validNel(6);
    Validated<NonEmptyList<String>, Integer> v7 = ValidatedNel.validNel(7);
    Validated<NonEmptyList<String>, Integer> v8 = ValidatedNel.validNel(8);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            v7,
            v8,
            Semigroup.nonEmptyList(),
            (a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<NonEmptyList<String>, Integer>) result).value()).isEqualTo(36);
  }

  // -------------------------------------------------------------------------
  // Higher arity error accumulation spot-check
  // -------------------------------------------------------------------------

  @Test
  void combine6_allInvalid_accumulatesAllErrors() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("e1");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("e2");
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.invalidNel("e3");
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.invalidNel("e4");
    Validated<NonEmptyList<String>, Integer> v5 = ValidatedNel.invalidNel("e5");
    Validated<NonEmptyList<String>, Integer> v6 = ValidatedNel.invalidNel("e6");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            Semigroup.nonEmptyList(),
            (a, b, c, d, e, f) -> a + b + c + d + e + f);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("e1", "e2", "e3", "e4", "e5", "e6");
  }

  @Test
  void combine4_allInvalid_accumulatesAllErrors() {
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

  @Test
  void combine8_allInvalid_accumulatesAllErrors() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("e1");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("e2");
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.invalidNel("e3");
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.invalidNel("e4");
    Validated<NonEmptyList<String>, Integer> v5 = ValidatedNel.invalidNel("e5");
    Validated<NonEmptyList<String>, Integer> v6 = ValidatedNel.invalidNel("e6");
    Validated<NonEmptyList<String>, Integer> v7 = ValidatedNel.invalidNel("e7");
    Validated<NonEmptyList<String>, Integer> v8 = ValidatedNel.invalidNel("e8");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            v7,
            v8,
            Semigroup.nonEmptyList(),
            (a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8");
  }

  @Test
  void combine8_mixed_accumulatesOnlyInvalidErrors() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("e1");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(2);
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.invalidNel("e3");
    Validated<NonEmptyList<String>, Integer> v4 = ValidatedNel.validNel(4);
    Validated<NonEmptyList<String>, Integer> v5 = ValidatedNel.invalidNel("e5");
    Validated<NonEmptyList<String>, Integer> v6 = ValidatedNel.validNel(6);
    Validated<NonEmptyList<String>, Integer> v7 = ValidatedNel.invalidNel("e7");
    Validated<NonEmptyList<String>, Integer> v8 = ValidatedNel.validNel(8);

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            v7,
            v8,
            Semigroup.nonEmptyList(),
            (a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("e1", "e3", "e5", "e7");
  }

  // -------------------------------------------------------------------------
  // Critical: error order, no short-circuit
  // -------------------------------------------------------------------------

  @Test
  void combine_errorOrderIsPreserved_leftToRight() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("first");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.validNel(2);
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.invalidNel("third");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, v3, Semigroup.nonEmptyList(), (a, b, c) -> a + b + c);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    assertThat(error.toList()).containsExactly("first", "third");
  }

  @Test
  void combine_doesNotShortCircuit_checksAllInputs() {
    Validated<NonEmptyList<String>, Integer> v1 = ValidatedNel.invalidNel("from v1");
    Validated<NonEmptyList<String>, Integer> v2 = ValidatedNel.invalidNel("from v2");
    Validated<NonEmptyList<String>, Integer> v3 = ValidatedNel.invalidNel("from v3");

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, v3, Semigroup.nonEmptyList(), (a, b, c) -> a + b + c);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    NonEmptyList<String> error =
        ((Validated.Invalid<NonEmptyList<String>, Integer>) result).error();
    // All three errors must be present — not just the first
    assertThat(error.toList()).containsExactly("from v1", "from v2", "from v3");
    assertThat(error.toList()).hasSize(3);
  }

  // -------------------------------------------------------------------------
  // Validated.combine with explicit Semigroup — proves the base API works
  // -------------------------------------------------------------------------

  @Test
  void validateCombine_withExplicitSemigroup_concatenatesStrings() {
    // A string-concatenation semigroup joins the errors with " | "
    Semigroup<String> joinSemigroup = (a, b) -> a + " | " + b;

    Validated<String, Integer> v1 = Validated.invalid("error1");
    Validated<String, Integer> v2 = Validated.invalid("error2");

    Validated<String, Integer> result = Validated.combine(v1, v2, joinSemigroup, Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, Integer>) result).error()).isEqualTo("error1 | error2");
  }

  @Test
  void validatedCombine_withExplicitSemigroup_allValid_appliesMapper() {
    Semigroup<String> joinSemigroup = (a, b) -> a + " | " + b;

    Validated<String, Integer> v1 = Validated.valid(5);
    Validated<String, Integer> v2 = Validated.valid(10);

    Validated<String, Integer> result = Validated.combine(v1, v2, joinSemigroup, Integer::sum);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<String, Integer>) result).value()).isEqualTo(15);
  }

  @Test
  void validatedCombine_withExplicitSemigroup_firstInvalidOnly_returnsFirstError() {
    Semigroup<String> joinSemigroup = (a, b) -> a + " | " + b;

    Validated<String, Integer> v1 = Validated.invalid("only error");
    Validated<String, Integer> v2 = Validated.valid(10);

    Validated<String, Integer> result = Validated.combine(v1, v2, joinSemigroup, Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, Integer>) result).error()).isEqualTo("only error");
  }

  @Test
  void validatedCombine_withExplicitSemigroup_secondInvalidOnly_returnsSecondError() {
    Semigroup<String> joinSemigroup = (a, b) -> a + " | " + b;

    Validated<String, Integer> v1 = Validated.valid(5);
    Validated<String, Integer> v2 = Validated.invalid("only error");

    Validated<String, Integer> result = Validated.combine(v1, v2, joinSemigroup, Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, Integer>) result).error()).isEqualTo("only error");
  }

  @Test
  void validatedCombine_withExplicitSemigroup_multiError_concatenatesAll() {
    Semigroup<List<String>> listSemigroup =
        (a, b) -> {
          java.util.List<String> combined = new java.util.ArrayList<>(a);
          combined.addAll(b);
          return java.util.Collections.unmodifiableList(combined);
        };

    Validated<List<String>, Integer> v1 = Validated.invalid(List.of("e1", "e2"));
    Validated<List<String>, Integer> v2 = Validated.invalid(List.of("e3"));

    Validated<List<String>, Integer> result =
        Validated.combine(v1, v2, listSemigroup, Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<List<String>, Integer>) result).error())
        .containsExactly("e1", "e2", "e3");
  }

  // -------------------------------------------------------------------------
  // Mapper-not-called property
  // -------------------------------------------------------------------------

  @Test
  void combine2_allInvalid_mapperIsNeverCalled() {
    AtomicBoolean mapperCalled = new AtomicBoolean(false);

    Validated.combine(
        ValidatedNel.<String, Integer>invalidNel("e1"),
        ValidatedNel.<String, Integer>invalidNel("e2"),
        Semigroup.nonEmptyList(),
        (a, b) -> {
          mapperCalled.set(true);
          return a + b;
        });

    assertThat(mapperCalled).isFalse();
  }

  @Test
  void combine3_allInvalid_mapperIsNeverCalled() {
    AtomicBoolean mapperCalled = new AtomicBoolean(false);

    Validated.combine(
        ValidatedNel.<String, Integer>invalidNel("e1"),
        ValidatedNel.<String, Integer>invalidNel("e2"),
        ValidatedNel.<String, Integer>invalidNel("e3"),
        Semigroup.nonEmptyList(),
        (a, b, c) -> {
          mapperCalled.set(true);
          return a + b + c;
        });

    assertThat(mapperCalled).isFalse();
  }

  // -------------------------------------------------------------------------
  // Null parameter validation on combine
  // -------------------------------------------------------------------------

  @Test
  void combine_nullV1_throwsNullPointerException() {
    Semigroup<String> semigroup = (a, b) -> a + b;
    Validated<String, Integer> v2 = Validated.valid(1);

    assertThatNullPointerException()
        .isThrownBy(() -> Validated.combine(null, v2, semigroup, Integer::sum))
        .withMessage("v1 must not be null");
  }

  @Test
  void combine_nullSemigroup_throwsNullPointerException() {
    Validated<String, Integer> v1 = Validated.valid(1);
    Validated<String, Integer> v2 = Validated.valid(2);

    assertThatNullPointerException()
        .isThrownBy(() -> Validated.combine(v1, v2, null, Integer::sum))
        .withMessage("semigroup must not be null");
  }

  @Test
  void combine_nullMapper_throwsNullPointerException() {
    Semigroup<String> semigroup = (a, b) -> a + b;
    Validated<String, Integer> v1 = Validated.valid(1);
    Validated<String, Integer> v2 = Validated.valid(2);

    assertThatNullPointerException()
        .isThrownBy(() -> Validated.combine(v1, v2, semigroup, null))
        .withMessage("mapper must not be null");
  }
}
