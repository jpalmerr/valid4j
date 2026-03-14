package io.github.jpalmerr.valid4j;

import io.github.jpalmerr.valid4j.functions.Function3;
import io.github.jpalmerr.valid4j.functions.Function4;
import io.github.jpalmerr.valid4j.functions.Function5;
import io.github.jpalmerr.valid4j.functions.Function6;
import io.github.jpalmerr.valid4j.functions.Function7;
import io.github.jpalmerr.valid4j.functions.Function8;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Convenience factories, combiners, and extractors for {@code Validated<NonEmptyList<E>, A>} — a
 * Validated where the error is a {@link NonEmptyList} guaranteed to be non-empty. Equivalent to
 * Cats' type alias {@code ValidatedNel[E, A] = Validated[NonEmptyList[E], A]}.
 *
 * <p>For combining multiple validated values with {@link NonEmptyList} error accumulation:
 *
 * <pre>{@code
 * ValidatedNel.combine(v1, v2, v3, User::new);
 * }</pre>
 *
 * <p>For custom error types, use {@link Validated#combine} with an explicit {@link Semigroup}.
 */
public final class ValidatedNel {

  private ValidatedNel() {}

  // -------------------------------------------------------------------------
  // Factories
  // -------------------------------------------------------------------------

  /**
   * Creates a {@link Validated.Valid} with a {@link NonEmptyList} error channel.
   *
   * @param value the valid value (must not be null)
   * @param <E> error element type
   * @param <A> value type
   * @return a {@code Valid<NonEmptyList<E>, A>}
   */
  public static <E, A> Validated<NonEmptyList<E>, A> validNel(A value) {
    return Validated.valid(value);
  }

  /**
   * Creates an {@link Validated.Invalid} containing a single error wrapped in a {@link
   * NonEmptyList}.
   *
   * @param error the error (must not be null)
   * @param <E> error element type
   * @param <A> value type
   * @return an {@code Invalid<NonEmptyList<E>, A>}
   */
  public static <E, A> Validated<NonEmptyList<E>, A> invalidNel(E error) {
    Objects.requireNonNull(error, "error must not be null");
    return Validated.invalid(NonEmptyList.of(error));
  }

  /**
   * Creates an {@link Validated.Invalid} containing multiple errors wrapped in a {@link
   * NonEmptyList}.
   *
   * @param first the first error (must not be null)
   * @param rest additional errors (must not be null)
   * @param <E> error element type
   * @param <A> value type
   * @return an {@code Invalid<NonEmptyList<E>, A>}
   */
  @SafeVarargs
  public static <E, A> Validated<NonEmptyList<E>, A> invalidNel(E first, E... rest) {
    List<E> tail = new ArrayList<>(rest.length);
    for (E element : rest) {
      tail.add(element);
    }
    return Validated.invalid(new NonEmptyList<>(first, tail));
  }

  // -------------------------------------------------------------------------
  // Conversion from Validated
  // -------------------------------------------------------------------------

  /**
   * Converts a {@code Validated<E, A>} to a {@code Validated<NonEmptyList<E>, A>} by wrapping a
   * single error in a {@link NonEmptyList}. Valid values are passed through unchanged.
   *
   * <p>Equivalent to Cats' {@code toValidatedNel}. Placed here to avoid coupling the core {@link
   * Validated} type to {@link NonEmptyList}.
   *
   * <pre>{@code
   * Validated<NonEmptyList<String>, User> nel = ValidatedNel.fromValidated(validateUser(form));
   * }</pre>
   *
   * @param validated the source {@code Validated} (must not be null)
   * @param <E> error element type
   * @param <A> value type
   * @return a {@code Validated<NonEmptyList<E>, A>}
   */
  public static <E, A> Validated<NonEmptyList<E>, A> fromValidated(Validated<E, A> validated) {
    Objects.requireNonNull(validated, "validated must not be null");
    return switch (validated) {
      case Validated.Valid<E, A>(var value) -> Validated.valid(value);
      case Validated.Invalid<E, A>(var error) -> Validated.invalid(NonEmptyList.of(error));
    };
  }

  // -------------------------------------------------------------------------
  // Convenience extractors
  // -------------------------------------------------------------------------

  /**
   * Extracts the {@link NonEmptyList} of errors from a ValidatedNel result. Returns {@link
   * Optional#empty()} if the result is valid.
   *
   * @param validated the validated instance to inspect (must not be null)
   * @param <E> error element type
   * @param <A> value type
   * @return an {@code Optional<NonEmptyList<E>>} containing the errors, or empty if valid
   */
  public static <E, A> Optional<NonEmptyList<E>> errors(Validated<NonEmptyList<E>, A> validated) {
    Objects.requireNonNull(validated, "validated must not be null");
    return switch (validated) {
      case Validated.Valid<NonEmptyList<E>, A> ignored -> Optional.empty();
      case Validated.Invalid<NonEmptyList<E>, A>(var error) -> Optional.of(error);
    };
  }

  // -------------------------------------------------------------------------
  // Combine — applicative error accumulation with NonEmptyList
  // -------------------------------------------------------------------------

  /**
   * Combines two validated values with {@link NonEmptyList} error accumulation. Equivalent to
   * {@code Validated.combine(v1, v2, Semigroup.nonEmptyList(), mapper)}.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error element type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <R> the result type
   * @return a combined {@code Validated<NonEmptyList<E>, R>}
   */
  public static <E, A, B, R> Validated<NonEmptyList<E>, R> combine(
      Validated<NonEmptyList<E>, A> v1,
      Validated<NonEmptyList<E>, B> v2,
      BiFunction<? super A, ? super B, ? extends R> mapper) {
    return Validated.combine(v1, v2, Semigroup.nonEmptyList(), mapper);
  }

  /** Three-input variant of {@link #combine}. */
  public static <E, A, B, C, R> Validated<NonEmptyList<E>, R> combine(
      Validated<NonEmptyList<E>, A> v1,
      Validated<NonEmptyList<E>, B> v2,
      Validated<NonEmptyList<E>, C> v3,
      Function3<? super A, ? super B, ? super C, ? extends R> mapper) {
    return Validated.combine(v1, v2, v3, Semigroup.nonEmptyList(), mapper);
  }

  /** Four-input variant of {@link #combine}. */
  public static <E, A, B, C, D, R> Validated<NonEmptyList<E>, R> combine(
      Validated<NonEmptyList<E>, A> v1,
      Validated<NonEmptyList<E>, B> v2,
      Validated<NonEmptyList<E>, C> v3,
      Validated<NonEmptyList<E>, D> v4,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends R> mapper) {
    return Validated.combine(v1, v2, v3, v4, Semigroup.nonEmptyList(), mapper);
  }

  /** Five-input variant of {@link #combine}. */
  public static <E, A, B, C, D, F, R> Validated<NonEmptyList<E>, R> combine(
      Validated<NonEmptyList<E>, A> v1,
      Validated<NonEmptyList<E>, B> v2,
      Validated<NonEmptyList<E>, C> v3,
      Validated<NonEmptyList<E>, D> v4,
      Validated<NonEmptyList<E>, F> v5,
      Function5<? super A, ? super B, ? super C, ? super D, ? super F, ? extends R> mapper) {
    return Validated.combine(v1, v2, v3, v4, v5, Semigroup.nonEmptyList(), mapper);
  }

  /** Six-input variant of {@link #combine}. */
  public static <E, A, B, C, D, F, G, R> Validated<NonEmptyList<E>, R> combine(
      Validated<NonEmptyList<E>, A> v1,
      Validated<NonEmptyList<E>, B> v2,
      Validated<NonEmptyList<E>, C> v3,
      Validated<NonEmptyList<E>, D> v4,
      Validated<NonEmptyList<E>, F> v5,
      Validated<NonEmptyList<E>, G> v6,
      Function6<? super A, ? super B, ? super C, ? super D, ? super F, ? super G, ? extends R>
          mapper) {
    return Validated.combine(v1, v2, v3, v4, v5, v6, Semigroup.nonEmptyList(), mapper);
  }

  /** Seven-input variant of {@link #combine}. */
  public static <E, A, B, C, D, F, G, H, R> Validated<NonEmptyList<E>, R> combine(
      Validated<NonEmptyList<E>, A> v1,
      Validated<NonEmptyList<E>, B> v2,
      Validated<NonEmptyList<E>, C> v3,
      Validated<NonEmptyList<E>, D> v4,
      Validated<NonEmptyList<E>, F> v5,
      Validated<NonEmptyList<E>, G> v6,
      Validated<NonEmptyList<E>, H> v7,
      Function7<
              ? super A,
              ? super B,
              ? super C,
              ? super D,
              ? super F,
              ? super G,
              ? super H,
              ? extends R>
          mapper) {
    return Validated.combine(v1, v2, v3, v4, v5, v6, v7, Semigroup.nonEmptyList(), mapper);
  }

  /** Eight-input variant of {@link #combine}. */
  public static <E, A, B, C, D, F, G, H, I, R> Validated<NonEmptyList<E>, R> combine(
      Validated<NonEmptyList<E>, A> v1,
      Validated<NonEmptyList<E>, B> v2,
      Validated<NonEmptyList<E>, C> v3,
      Validated<NonEmptyList<E>, D> v4,
      Validated<NonEmptyList<E>, F> v5,
      Validated<NonEmptyList<E>, G> v6,
      Validated<NonEmptyList<E>, H> v7,
      Validated<NonEmptyList<E>, I> v8,
      Function8<
              ? super A,
              ? super B,
              ? super C,
              ? super D,
              ? super F,
              ? super G,
              ? super H,
              ? super I,
              ? extends R>
          mapper) {
    return Validated.combine(v1, v2, v3, v4, v5, v6, v7, v8, Semigroup.nonEmptyList(), mapper);
  }

  // -------------------------------------------------------------------------
  // Sequence
  // -------------------------------------------------------------------------

  /**
   * Converts a list of {@code Validated<NonEmptyList<E>, A>} values into a {@code
   * Validated<NonEmptyList<E>, List<A>>}.
   *
   * <p>Delegates to {@link Validated#sequence} with {@link Semigroup#nonEmptyList()}.
   *
   * @param values the list of validated values to sequence (must not be null)
   * @param <E> the error element type
   * @param <A> the value type
   * @return {@link Validated.Valid} with all values if all are valid, or {@link Validated.Invalid}
   *     with all errors appended into one {@link NonEmptyList}
   */
  public static <E, A> Validated<NonEmptyList<E>, List<A>> sequence(
      List<Validated<NonEmptyList<E>, A>> values) {
    return Validated.sequence(values, Semigroup.nonEmptyList());
  }
}
