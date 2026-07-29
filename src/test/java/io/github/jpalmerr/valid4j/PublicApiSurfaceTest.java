package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PublicApiSurfaceTest {

  // -------------------------------------------------------------------------
  // Validated
  // -------------------------------------------------------------------------

  @Test
  void validated_exposesExactlyTheDocumentedSurface() {
    assertThat(publicMethodNamesAndArities(Validated.class))
        .containsExactlyInAnyOrder(
            "andThen(1)",
            "combine(4)",
            "combine(5)",
            "combine(6)",
            "combine(7)",
            "combine(8)",
            "combine(9)",
            "combine(10)",
            "fold(2)",
            "getOrElseThrow(1)",
            "invalid(1)",
            "isInvalid(0)",
            "isValid(0)",
            "map(1)",
            "mapError(1)",
            "sequence(2)",
            "valid(1)");
  }

  @Test
  void validated_isSealedWithExactlyValidAndInvalid() {
    assertThat(Validated.class.isSealed()).isTrue();
    assertThat(Stream.of(Validated.class.getPermittedSubclasses()).map(Class::getSimpleName))
        .containsExactlyInAnyOrder("Valid", "Invalid");
  }

  // Strictly subsumed by the surface assertion above, and kept anyway: the absence of flatMap is a
  // deliberate design decision, not an omission. combine is the applicative operation for
  // independent validations and andThen is for sequential dependent ones, so a flatMap added in
  // good faith should fail against a test that names the reason.
  @Test
  void validated_hasNoFlatMap() {
    assertThat(publicMethodNamesAndArities(Validated.class))
        .noneMatch(sig -> sig.startsWith("flatMap("));
  }

  // -------------------------------------------------------------------------
  // ValidatedNel
  // -------------------------------------------------------------------------

  @Test
  void validatedNel_exposesExactlyTheDocumentedSurface() {
    assertThat(publicMethodNamesAndArities(ValidatedNel.class))
        .containsExactlyInAnyOrder(
            "combine(3)",
            "combine(4)",
            "combine(5)",
            "combine(6)",
            "combine(7)",
            "combine(8)",
            "combine(9)",
            "errors(1)",
            "fromValidated(1)",
            "invalidNel(1)",
            "invalidNel(2)",
            "sequence(1)",
            "validNel(1)");
  }

  // -------------------------------------------------------------------------
  // NonEmptyList
  // -------------------------------------------------------------------------

  @Test
  void nonEmptyList_exposesExactlyTheDocumentedSurface() {
    assertThat(publicMethodNamesAndArities(NonEmptyList.class))
        .containsExactlyInAnyOrder(
            "append(1)",
            "appendAll(1)",
            "equals(1)",
            "fromList(1)",
            "hashCode(0)",
            "head(0)",
            "iterator(0)",
            "map(1)",
            "of(2)",
            "size(0)",
            "tail(0)",
            "toList(0)",
            "toString(0)");
  }

  // -------------------------------------------------------------------------
  // Semigroup
  // -------------------------------------------------------------------------

  @Test
  void semigroup_exposesExactlyTheDocumentedSurface() {
    assertThat(publicMethodNamesAndArities(Semigroup.class))
        .containsExactlyInAnyOrder("combine(2)", "nonEmptyList(0)", "of(1)");
  }

  // -------------------------------------------------------------------------
  // Function arities
  // -------------------------------------------------------------------------

  @Test
  void functionArities_3To8_areFunctionalInterfacesNamedApplyWithMatchingArity()
      throws ClassNotFoundException {
    for (int arity = 3; arity <= 8; arity++) {
      Class<?> fn = Class.forName("io.github.jpalmerr.valid4j.functions.Function" + arity);

      assertThat(fn.isInterface()).isTrue();
      assertThat(fn.isAnnotationPresent(FunctionalInterface.class)).isTrue();
      assertThat(publicMethodNamesAndArities(fn)).containsExactly("apply(" + arity + ")");
    }
  }

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  // Names and arities only -- not descriptors. Return types, parameter types, and static/default
  // are deliberately not pinned here; that is Revapi's job at release time. getDeclaredMethods
  // returns an unspecified order, so sorting keeps AssertJ's failure output stable.
  private static List<String> publicMethodNamesAndArities(Class<?> type) {
    return Arrays.stream(type.getDeclaredMethods())
        .filter(m -> Modifier.isPublic(m.getModifiers()))
        .filter(m -> !m.isSynthetic())
        .map(m -> m.getName() + "(" + m.getParameterCount() + ")")
        .sorted()
        .toList();
  }
}
