package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

class PropertyTest {

  // -------------------------------------------------------------------------
  // Semigroup laws
  // -------------------------------------------------------------------------

  @Property
  void stringSemigroup_associativity(
      @ForAll @StringLength(max = 10) String x,
      @ForAll @StringLength(max = 10) String y,
      @ForAll @StringLength(max = 10) String z) {
    Semigroup<String> semigroup = (a, b) -> a + b;

    String leftAssoc = semigroup.combine(semigroup.combine(x, y), z);
    String rightAssoc = semigroup.combine(x, semigroup.combine(y, z));

    assertThat(leftAssoc).isEqualTo(rightAssoc);
  }

  @Property
  void nonEmptyListAppendAll_associativity(
      @ForAll @StringLength(max = 5) String headA,
      @ForAll @StringLength(max = 5) String headB,
      @ForAll @StringLength(max = 5) String headC) {
    NonEmptyList<String> a = NonEmptyList.of(headA);
    NonEmptyList<String> b = NonEmptyList.of(headB);
    NonEmptyList<String> c = NonEmptyList.of(headC);

    NonEmptyList<String> leftAssoc = a.appendAll(b).appendAll(c);
    NonEmptyList<String> rightAssoc = a.appendAll(b.appendAll(c));

    assertThat(leftAssoc).isEqualTo(rightAssoc);
  }

  // -------------------------------------------------------------------------
  // Functor laws (Validated)
  // -------------------------------------------------------------------------

  @Property
  void validatedValid_functorIdentity(@ForAll @IntRange(min = -1000, max = 1000) int n) {
    Validated<String, Integer> v = Validated.valid(n);

    Validated<String, Integer> result = v.map(Function.identity());

    assertThat(result).isEqualTo(Validated.valid(n));
  }

  @Property
  void validatedValid_functorComposition(@ForAll @IntRange(min = -1000, max = 1000) int n) {
    Validated<String, Integer> v = Validated.valid(n);
    Function<Integer, Integer> f = x -> x + 1;
    Function<Integer, Integer> g = x -> x * 2;

    Validated<String, Integer> mapThenMap = v.map(f).map(g);
    Validated<String, Integer> compose = v.map(f.andThen(g));

    assertThat(mapThenMap).isEqualTo(compose);
  }

  @Property
  void validatedInvalid_functorIdentity(@ForAll @StringLength(max = 20) String e) {
    Validated<String, Integer> v = Validated.invalid(e);

    Validated<String, Integer> result = v.map(Function.identity());

    assertThat(result).isEqualTo(Validated.invalid(e));
  }

  // -------------------------------------------------------------------------
  // andThen laws (Validated)
  // -------------------------------------------------------------------------

  @Property
  void validated_andThen_leftIdentity(@ForAll @IntRange(min = -1000, max = 1000) int a) {
    Function<Integer, Validated<String, Integer>> f = n -> Validated.valid(n * 2);

    Validated<String, Integer> leftIdentity = Validated.<String, Integer>valid(a).andThen(f);
    Validated<String, Integer> direct = f.apply(a);

    assertThat(leftIdentity).isEqualTo(direct);
  }

  @Property
  void validated_andThen_rightIdentity(@ForAll @IntRange(min = -1000, max = 1000) int n) {
    Validated<String, Integer> m = Validated.valid(n);

    Validated<String, Integer> result = m.andThen(Validated::valid);

    assertThat(result).isEqualTo(m);
  }

  @Property
  void validated_andThen_associativity(@ForAll @IntRange(min = -1000, max = 1000) int n) {
    Validated<String, Integer> m = Validated.valid(n);
    Function<Integer, Validated<String, Integer>> f = x -> Validated.valid(x + 1);
    Function<Integer, Validated<String, Integer>> g = x -> Validated.valid(x * 2);

    Validated<String, Integer> leftAssoc = m.andThen(f).andThen(g);
    Validated<String, Integer> rightAssoc = m.andThen(a -> f.apply(a).andThen(g));

    assertThat(leftAssoc).isEqualTo(rightAssoc);
  }

  @Property
  void nonEmptyList_fromListRoundTrip(
      @ForAll @IntRange(min = -100, max = 100) int head,
      @ForAll @Size(max = 5) List<@IntRange(min = -100, max = 100) Integer> tailValues) {
    List<Integer> tail = new ArrayList<>(tailValues);
    List<Integer> list = new ArrayList<>();
    list.add(head);
    list.addAll(tail);

    List<Integer> roundTripped = NonEmptyList.fromList(list).get().toList();

    assertThat(roundTripped).isEqualTo(list);
  }

  // -------------------------------------------------------------------------
  // Applicative (Validated.combine) — error accumulation
  // -------------------------------------------------------------------------

  @Property
  void validatedCombine_bothInvalid_accumulatesBothErrors(
      @ForAll @StringLength(min = 1, max = 10) String error1,
      @ForAll @StringLength(min = 1, max = 10) String error2) {
    Validated<String, Integer> v1 = Validated.invalid(error1);
    Validated<String, Integer> v2 = Validated.invalid(error2);
    Semigroup<String> semigroup = (a, b) -> a + b;

    Validated<String, String> result = Validated.combine(v1, v2, semigroup, (a, b) -> a + "," + b);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, String>) result).error()).isEqualTo(error1 + error2);
  }
}
