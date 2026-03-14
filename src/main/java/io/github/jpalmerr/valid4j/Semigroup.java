package io.github.jpalmerr.valid4j;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A binary operation that combines two values of the same type, satisfying the associativity law:
 * {@code combine(x, combine(y, z)) == combine(combine(x, y), z)}.
 *
 * <p>Inspired by the Cats {@code Semigroup} typeclass in Scala. A {@code Semigroup<A>} allows two
 * {@code A} values to be combined into one, which is the key mechanism enabling applicative error
 * accumulation in {@link Validated}.
 *
 * <p>When used with {@link Validated#combine}, the semigroup is invoked only when two or more
 * inputs are {@link Validated.Invalid} — it determines how their errors are merged into a single
 * error value.
 *
 * <p>For the common case of {@link NonEmptyList} error accumulation, use {@link
 * ValidatedNel#combine} which hardcodes {@link #nonEmptyList()} — no semigroup parameter needed.
 *
 * @param <A> the type of values this semigroup combines
 */
@FunctionalInterface
public interface Semigroup<A> {

  /**
   * Combines two values into one.
   *
   * <p>Implementations must satisfy associativity: {@code combine(x, combine(y, z))} is equivalent
   * to {@code combine(combine(x, y), z)}.
   *
   * @param x the first value (must not be null)
   * @param y the second value (must not be null)
   * @return the combined value
   */
  A combine(A x, A y);

  /**
   * Wraps a {@link BiFunction} as a {@code Semigroup}.
   *
   * <p><strong>Caller's responsibility:</strong> the combiner must satisfy associativity — {@code
   * combine(x, combine(y, z)) == combine(combine(x, y), z)}. {@link Validated#combine} folds errors
   * left-to-right, so a non-associative combiner will produce results that depend on internal
   * accumulation order. Verify your instance with property-based tests.
   *
   * @param combiner the combining function (must not be null)
   * @param <A> the value type
   * @return a semigroup backed by the given function
   */
  static <A> Semigroup<A> of(BiFunction<A, A, A> combiner) {
    Objects.requireNonNull(combiner, "combiner must not be null");
    return combiner::apply;
  }

  /**
   * {@link NonEmptyList} concatenation — the primary use case for {@link Validated} error
   * accumulation.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Validated.combine(v1, v2, Semigroup.nonEmptyList(), mapper);
   * }</pre>
   *
   * @param <A> the list element type
   * @return a semigroup that appends all elements of the second list to the first
   */
  static <A> Semigroup<NonEmptyList<A>> nonEmptyList() {
    return NonEmptyList::appendAll;
  }
}
