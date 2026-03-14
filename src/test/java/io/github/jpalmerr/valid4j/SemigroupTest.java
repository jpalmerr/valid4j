package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class SemigroupTest {

  // -------------------------------------------------------------------------
  // of
  // -------------------------------------------------------------------------

  @Test
  void of_wrapsFunction() {
    Semigroup<Integer> sum = Semigroup.of(Integer::sum);
    assertThat(sum.combine(3, 4)).isEqualTo(7);
  }

  @Test
  void of_null_throwsNPE() {
    assertThatNullPointerException()
        .isThrownBy(() -> Semigroup.of(null))
        .withMessage("combiner must not be null");
  }

  @Test
  void of_isAssociative() {
    Semigroup<Integer> sum = Semigroup.of(Integer::sum);
    assertThat(sum.combine(1, sum.combine(2, 3))).isEqualTo(sum.combine(sum.combine(1, 2), 3));
  }

  // -------------------------------------------------------------------------
  // nonEmptyList()
  // -------------------------------------------------------------------------

  @Test
  void nonEmptyList_appendsAllElements() {
    Semigroup<NonEmptyList<String>> s = Semigroup.nonEmptyList();
    NonEmptyList<String> left = NonEmptyList.of("a", "b");
    NonEmptyList<String> right = NonEmptyList.of("c", "d");
    assertThat(s.combine(left, right).toList()).containsExactly("a", "b", "c", "d");
  }

  @Test
  void nonEmptyList_isAssociative() {
    Semigroup<NonEmptyList<String>> s = Semigroup.nonEmptyList();
    NonEmptyList<String> a = NonEmptyList.of("a");
    NonEmptyList<String> b = NonEmptyList.of("b");
    NonEmptyList<String> c = NonEmptyList.of("c");
    assertThat(s.combine(a, s.combine(b, c)).toList())
        .isEqualTo(s.combine(s.combine(a, b), c).toList());
  }

  @Test
  void nonEmptyList_usedWithValidatedCombine_accumulatesErrors() {
    Validated<NonEmptyList<String>, Integer> v1 = Validated.invalid(NonEmptyList.of("e1"));
    Validated<NonEmptyList<String>, Integer> v2 = Validated.invalid(NonEmptyList.of("e2"));

    Validated<NonEmptyList<String>, Integer> result =
        Validated.combine(v1, v2, Semigroup.nonEmptyList(), Integer::sum);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<NonEmptyList<String>, Integer>) result).error().toList())
        .containsExactly("e1", "e2");
  }
}
