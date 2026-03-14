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
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A value that is either valid ({@link Valid}) or invalid ({@link Invalid}).
 *
 * <p>Unlike fail-fast approaches, {@code Validated} is designed for error accumulation: multiple
 * validations can be combined and all errors collected, not just the first. Error accumulation is
 * driven by a {@link Semigroup} that determines how two errors are merged into one.
 *
 * <p>Use {@link #valid(Object)} and {@link #invalid(Object)} to construct instances. Use {@link
 * #map(Function)}, {@link #mapError(Function)}, and {@link #fold(Function, Function)} to transform
 * and extract values.
 *
 * <p>Applicative combination: {@link #combine} and {@link #sequence} run independent validations
 * and accumulate all errors via a semigroup — equivalent to Cats' {@code mapN} / {@code
 * Applicative} style. When all inputs are valid the mapper is applied; when any are invalid, all
 * errors are merged.
 *
 * <p>{@code Validated} deliberately does not provide {@code flatMap}. Because {@code flatMap} is
 * sequential (each step depends on the previous), it cannot accumulate errors from independent
 * branches. If you need sequential, dependent computation use {@link #andThen} instead.
 *
 * <p>Pattern matching is fully supported via Java 21 sealed interfaces:
 *
 * <pre>{@code
 * switch (result) {
 *     case Validated.Valid<?, String>(var value)   -> System.out.println("Got: " + value);
 *     case Validated.Invalid<String, ?>(var error) -> System.out.println("Error: " + error);
 * }
 * }</pre>
 *
 * <p><strong>Type parameter convention:</strong> {@code E} is always the error type. Value type
 * parameters use {@code A, B, C, D, F, G, H, I} — the letter {@code E} is skipped in the value
 * sequence to avoid collision with the error type.
 *
 * @param <E> the error type
 * @param <A> the value type
 */
public sealed interface Validated<E, A> permits Validated.Valid, Validated.Invalid {

  /**
   * The successful case: holds a valid value.
   *
   * <p>Equality is based on the wrapped value only. The phantom error type parameter {@code E} does
   * not participate in {@code equals}/{@code hashCode} due to type erasure. Two {@code Valid}
   * instances with different error types but the same value are considered equal.
   *
   * @param <E> the error type (unused in this case, but required for type alignment)
   * @param <A> the value type
   */
  record Valid<E, A>(A value) implements Validated<E, A> {

    /** Compact constructor: rejects null values. */
    public Valid {
      Objects.requireNonNull(value, "value must not be null");
    }
  }

  /**
   * The failure case: holds a single error value.
   *
   * <p>To accumulate multiple errors, use an error type that itself can hold many values (e.g.
   * {@link NonEmptyList}) and combine via a {@link Semigroup}.
   *
   * <p>Equality is based on the wrapped error only. The phantom value type parameter {@code A} does
   * not participate in {@code equals}/{@code hashCode} due to type erasure. Two {@code Invalid}
   * instances with different value types but the same error are considered equal.
   *
   * @param <E> the error type
   * @param <A> the value type (unused in this case, but required for type alignment)
   */
  record Invalid<E, A>(E error) implements Validated<E, A> {

    /** Compact constructor: rejects null errors. */
    public Invalid {
      Objects.requireNonNull(error, "error must not be null");
    }
  }

  // -------------------------------------------------------------------------
  // Static factories
  // -------------------------------------------------------------------------

  /**
   * Creates a {@link Valid} instance wrapping the given value.
   *
   * @param value the valid value (must not be null)
   * @param <E> error type
   * @param <A> value type
   * @return a new {@code Valid<E, A>}
   */
  static <E, A> Validated<E, A> valid(A value) {
    return new Valid<>(value);
  }

  /**
   * Creates an {@link Invalid} instance holding a single error.
   *
   * <pre>{@code
   * Validated.invalid("required field missing")
   * }</pre>
   *
   * @param error the error (must not be null)
   * @param <E> error type
   * @param <A> value type
   * @return a new {@code Invalid<E, A>}
   */
  static <E, A> Validated<E, A> invalid(E error) {
    return new Invalid<>(error);
  }

  // -------------------------------------------------------------------------
  // Instance methods
  // -------------------------------------------------------------------------

  /**
   * Transforms the value if this is {@link Valid}; passes through unchanged if {@link Invalid}.
   * Functor {@code map} over the success side.
   *
   * @param f the mapping function (must not be null)
   * @param <B> the result value type
   * @return a new {@code Valid<E, B>} if valid, or the same {@code Invalid<E, B>} if invalid
   */
  default <B> Validated<E, B> map(Function<? super A, ? extends B> f) {
    Objects.requireNonNull(f, "f must not be null");
    return switch (this) {
      case Valid<E, A>(var value) -> new Valid<>(f.apply(value));
      case Invalid<E, A>(var error) -> new Invalid<>(error);
    };
  }

  /**
   * Transforms the error if this is {@link Invalid}; passes through unchanged if {@link Valid}.
   * leftMap / Bifunctor map over the error side.
   *
   * @param f the error mapping function (must not be null)
   * @param <F> the result error type
   * @return a new {@code Invalid<F, A>} if invalid, or the same {@code Valid<F, A>} if valid
   */
  default <F> Validated<F, A> mapError(Function<? super E, ? extends F> f) {
    Objects.requireNonNull(f, "f must not be null");
    return switch (this) {
      case Valid<E, A>(var value) -> new Valid<>(value);
      case Invalid<E, A>(var error) -> new Invalid<>(f.apply(error));
    };
  }

  /**
   * Exhaustively extracts a value from either case (catamorphism).
   *
   * <pre>{@code
   * String message = result.fold(
   *     error -> "Error: " + error,
   *     value -> "Value: " + value
   * );
   * }</pre>
   *
   * @param onInvalid function applied to the error if invalid (must not be null)
   * @param onValid function applied to the value if valid (must not be null)
   * @param <B> the result type
   * @return the result of whichever function was applied
   */
  default <B> B fold(
      Function<? super E, ? extends B> onInvalid, Function<? super A, ? extends B> onValid) {
    Objects.requireNonNull(onInvalid, "onInvalid must not be null");
    Objects.requireNonNull(onValid, "onValid must not be null");
    return switch (this) {
      case Valid<E, A>(var value) -> onValid.apply(value);
      case Invalid<E, A>(var error) -> onInvalid.apply(error);
    };
  }

  /**
   * Sequences this {@code Validated} with a function that returns a {@code Validated}. If this is
   * {@link Valid}, applies {@code f} to the value and returns the result. If this is {@link
   * Invalid}, propagates the error without calling {@code f}.
   *
   * <p>Use {@code andThen} when a second validation depends on the result of the first:
   *
   * <pre>{@code
   * Validated<String, String> result =
   *     validateEmail(rawEmail).andThen(email -> checkEmailNotBlocked(email));
   * }</pre>
   *
   * <p>Note: unlike {@link #combine}, {@code andThen} does not accumulate errors from both steps —
   * if this is {@link Invalid}, the function is never called. Use {@link #combine} for independent
   * validations.
   *
   * @param f the function to apply to the value if valid (must not be null)
   * @param <B> the result value type
   * @return the result of {@code f} if valid, or this {@link Invalid} propagated
   */
  default <B> Validated<E, B> andThen(Function<? super A, ? extends Validated<E, B>> f) {
    Objects.requireNonNull(f, "f must not be null");
    return fold(e -> invalid(e), f);
  }

  /**
   * Returns {@code true} if this is a {@link Valid}.
   *
   * @return {@code true} for {@link Valid}, {@code false} for {@link Invalid}
   */
  default boolean isValid() {
    return switch (this) {
      case Valid<E, A> ignored -> true;
      case Invalid<E, A> ignored -> false;
    };
  }

  /**
   * Returns {@code true} if this is an {@link Invalid}.
   *
   * @return {@code true} for {@link Invalid}, {@code false} for {@link Valid}
   */
  default boolean isInvalid() {
    return !isValid();
  }

  // -------------------------------------------------------------------------
  // Value extraction
  // -------------------------------------------------------------------------

  /**
   * Returns the value if {@link Valid}. If {@link Invalid}, maps the error to an exception via
   * {@code exceptionMapper} and throws it.
   *
   * <pre>{@code
   * User user = validateUser(form)
   *     .getOrElseThrow(error -> new ValidationException("Invalid user: " + error));
   * }</pre>
   *
   * @param exceptionMapper maps the error to a throwable (must not be null)
   * @param <X> the exception type
   * @return the valid value
   * @throws X if this is {@link Invalid}
   */
  default <X extends Throwable> A getOrElseThrow(Function<? super E, ? extends X> exceptionMapper)
      throws X {
    Objects.requireNonNull(exceptionMapper, "exceptionMapper must not be null");
    return switch (this) {
      case Valid<E, A>(var value) -> value;
      case Invalid<E, A>(var error) -> throw exceptionMapper.apply(error);
    };
  }

  // -------------------------------------------------------------------------
  // Sequence — convert a list of Validated into a Validated list
  // -------------------------------------------------------------------------

  /**
   * Converts a list of {@code Validated} values into a {@code Validated} list. Traverse / {@code
   * sequence} from Cats: collapses {@code List<Validated<E, A>>} into {@code Validated<E, List<A>>}
   * with error accumulation via the semigroup.
   *
   * <p>If all values are {@link Valid}, returns a {@link Valid} containing all values in the same
   * order. If any are {@link Invalid}, accumulates their errors left-to-right using the given
   * {@link Semigroup} and returns a single {@link Invalid}.
   *
   * <pre>{@code
   * List<Validated<String, Integer>> inputs = List.of(
   *     Validated.valid(1),
   *     Validated.valid(2),
   *     Validated.valid(3)
   * );
   * Validated<String, List<Integer>> result = Validated.sequence(inputs, (a, b) -> a + ", " + b);
   * // → Valid([1, 2, 3])
   * }</pre>
   *
   * @param values the list of {@code Validated} values to sequence (must not be null)
   * @param semigroup the semigroup used to combine errors from multiple invalid inputs (must not be
   *     null)
   * @param <E> the error type
   * @param <A> the value type
   * @return {@link Valid} with all values if all are valid, or {@link Invalid} with accumulated
   *     errors
   */
  static <E, A> Validated<E, List<A>> sequence(
      List<Validated<E, A>> values, Semigroup<E> semigroup) {
    Objects.requireNonNull(values, "values must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");

    E accumulatedError = null;
    List<A> successes = new ArrayList<>();

    for (Validated<E, A> v : values) {
      switch (v) {
        case Valid<E, A>(var value) -> successes.add(value);
        case Invalid<E, A>(var error) ->
            accumulatedError =
                (accumulatedError == null) ? error : semigroup.combine(accumulatedError, error);
      }
    }

    if (accumulatedError != null) {
      return new Invalid<>(accumulatedError);
    }
    return new Valid<>(List.copyOf(successes));
  }

  // -------------------------------------------------------------------------
  // Combine — applicative error accumulation
  // -------------------------------------------------------------------------

  /**
   * Combines two validated values, accumulating errors via the given {@link Semigroup} if any
   * inputs are invalid. Applicative combination — equivalent to Cats' {@code mapN}.
   *
   * <p>If both are valid, applies the mapper function and returns a {@link Valid}. If any are
   * invalid, their errors are combined left-to-right using the semigroup and returned as a single
   * {@link Invalid} — the mapper is never called.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param semigroup the semigroup used to combine errors (must not be null)
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <R> the result type
   * @return a combined {@code Validated<E, R>}
   */
  static <E, A, B, R> Validated<E, R> combine(
      Validated<E, A> v1,
      Validated<E, B> v2,
      Semigroup<E> semigroup,
      BiFunction<? super A, ? super B, ? extends R> mapper) {
    Objects.requireNonNull(v1, "v1 must not be null");
    Objects.requireNonNull(v2, "v2 must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    return switch (v1) {
      case Valid<E, A>(var a) ->
          switch (v2) {
            case Valid<E, B>(var b) -> new Valid<>(mapper.apply(a, b));
            case Invalid<E, B>(var e2) -> new Invalid<>(e2);
          };
      case Invalid<E, A>(var e1) ->
          switch (v2) {
            case Valid<E, B> ignored -> new Invalid<>(e1);
            case Invalid<E, B>(var e2) -> new Invalid<>(semigroup.combine(e1, e2));
          };
    };
  }

  /**
   * Combines three validated values, accumulating errors via the given {@link Semigroup} if any
   * inputs are invalid.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param v3 the third validated value
   * @param semigroup the semigroup used to combine errors (must not be null)
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <C> the type of the third valid value
   * @param <R> the result type
   * @return a combined {@code Validated<E, R>}
   */
  static <E, A, B, C, R> Validated<E, R> combine(
      Validated<E, A> v1,
      Validated<E, B> v2,
      Validated<E, C> v3,
      Semigroup<E> semigroup,
      Function3<? super A, ? super B, ? super C, ? extends R> mapper) {
    Objects.requireNonNull(v1, "v1 must not be null");
    Objects.requireNonNull(v2, "v2 must not be null");
    Objects.requireNonNull(v3, "v3 must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    E err = null;
    A a = null;
    B b = null;
    C c = null;
    switch (v1) {
      case Valid<E, A>(var val) -> a = val;
      case Invalid<E, A>(var e) -> err = e;
    }
    switch (v2) {
      case Valid<E, B>(var val) -> b = val;
      case Invalid<E, B>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v3) {
      case Valid<E, C>(var val) -> c = val;
      case Invalid<E, C>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    if (err != null) return new Invalid<>(err);
    return new Valid<>(mapper.apply(a, b, c));
  }

  /**
   * Combines four validated values, accumulating errors via the given {@link Semigroup} if any
   * inputs are invalid.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param v3 the third validated value
   * @param v4 the fourth validated value
   * @param semigroup the semigroup used to combine errors (must not be null)
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <C> the type of the third valid value
   * @param <D> the type of the fourth valid value
   * @param <R> the result type
   * @return a combined {@code Validated<E, R>}
   */
  static <E, A, B, C, D, R> Validated<E, R> combine(
      Validated<E, A> v1,
      Validated<E, B> v2,
      Validated<E, C> v3,
      Validated<E, D> v4,
      Semigroup<E> semigroup,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends R> mapper) {
    Objects.requireNonNull(v1, "v1 must not be null");
    Objects.requireNonNull(v2, "v2 must not be null");
    Objects.requireNonNull(v3, "v3 must not be null");
    Objects.requireNonNull(v4, "v4 must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    E err = null;
    A a = null;
    B b = null;
    C c = null;
    D d = null;
    switch (v1) {
      case Valid<E, A>(var val) -> a = val;
      case Invalid<E, A>(var e) -> err = e;
    }
    switch (v2) {
      case Valid<E, B>(var val) -> b = val;
      case Invalid<E, B>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v3) {
      case Valid<E, C>(var val) -> c = val;
      case Invalid<E, C>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v4) {
      case Valid<E, D>(var val) -> d = val;
      case Invalid<E, D>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    if (err != null) return new Invalid<>(err);
    return new Valid<>(mapper.apply(a, b, c, d));
  }

  /**
   * Combines five validated values, accumulating errors via the given {@link Semigroup} if any
   * inputs are invalid.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param v3 the third validated value
   * @param v4 the fourth validated value
   * @param v5 the fifth validated value
   * @param semigroup the semigroup used to combine errors (must not be null)
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <C> the type of the third valid value
   * @param <D> the type of the fourth valid value
   * @param <F> the type of the fifth valid value
   * @param <R> the result type
   * @return a combined {@code Validated<E, R>}
   */
  static <E, A, B, C, D, F, R> Validated<E, R> combine(
      Validated<E, A> v1,
      Validated<E, B> v2,
      Validated<E, C> v3,
      Validated<E, D> v4,
      Validated<E, F> v5,
      Semigroup<E> semigroup,
      Function5<? super A, ? super B, ? super C, ? super D, ? super F, ? extends R> mapper) {
    Objects.requireNonNull(v1, "v1 must not be null");
    Objects.requireNonNull(v2, "v2 must not be null");
    Objects.requireNonNull(v3, "v3 must not be null");
    Objects.requireNonNull(v4, "v4 must not be null");
    Objects.requireNonNull(v5, "v5 must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    E err = null;
    A a = null;
    B b = null;
    C c = null;
    D d = null;
    F f = null;
    switch (v1) {
      case Valid<E, A>(var val) -> a = val;
      case Invalid<E, A>(var e) -> err = e;
    }
    switch (v2) {
      case Valid<E, B>(var val) -> b = val;
      case Invalid<E, B>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v3) {
      case Valid<E, C>(var val) -> c = val;
      case Invalid<E, C>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v4) {
      case Valid<E, D>(var val) -> d = val;
      case Invalid<E, D>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v5) {
      case Valid<E, F>(var val) -> f = val;
      case Invalid<E, F>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    if (err != null) return new Invalid<>(err);
    return new Valid<>(mapper.apply(a, b, c, d, f));
  }

  /**
   * Combines six validated values, accumulating errors via the given {@link Semigroup} if any
   * inputs are invalid.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param v3 the third validated value
   * @param v4 the fourth validated value
   * @param v5 the fifth validated value
   * @param v6 the sixth validated value
   * @param semigroup the semigroup used to combine errors (must not be null)
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <C> the type of the third valid value
   * @param <D> the type of the fourth valid value
   * @param <F> the type of the fifth valid value
   * @param <G> the type of the sixth valid value
   * @param <R> the result type
   * @return a combined {@code Validated<E, R>}
   */
  static <E, A, B, C, D, F, G, R> Validated<E, R> combine(
      Validated<E, A> v1,
      Validated<E, B> v2,
      Validated<E, C> v3,
      Validated<E, D> v4,
      Validated<E, F> v5,
      Validated<E, G> v6,
      Semigroup<E> semigroup,
      Function6<? super A, ? super B, ? super C, ? super D, ? super F, ? super G, ? extends R>
          mapper) {
    Objects.requireNonNull(v1, "v1 must not be null");
    Objects.requireNonNull(v2, "v2 must not be null");
    Objects.requireNonNull(v3, "v3 must not be null");
    Objects.requireNonNull(v4, "v4 must not be null");
    Objects.requireNonNull(v5, "v5 must not be null");
    Objects.requireNonNull(v6, "v6 must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    E err = null;
    A a = null;
    B b = null;
    C c = null;
    D d = null;
    F f = null;
    G g = null;
    switch (v1) {
      case Valid<E, A>(var val) -> a = val;
      case Invalid<E, A>(var e) -> err = e;
    }
    switch (v2) {
      case Valid<E, B>(var val) -> b = val;
      case Invalid<E, B>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v3) {
      case Valid<E, C>(var val) -> c = val;
      case Invalid<E, C>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v4) {
      case Valid<E, D>(var val) -> d = val;
      case Invalid<E, D>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v5) {
      case Valid<E, F>(var val) -> f = val;
      case Invalid<E, F>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v6) {
      case Valid<E, G>(var val) -> g = val;
      case Invalid<E, G>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    if (err != null) return new Invalid<>(err);
    return new Valid<>(mapper.apply(a, b, c, d, f, g));
  }

  /**
   * Combines seven validated values, accumulating errors via the given {@link Semigroup} if any
   * inputs are invalid.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param v3 the third validated value
   * @param v4 the fourth validated value
   * @param v5 the fifth validated value
   * @param v6 the sixth validated value
   * @param v7 the seventh validated value
   * @param semigroup the semigroup used to combine errors (must not be null)
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <C> the type of the third valid value
   * @param <D> the type of the fourth valid value
   * @param <F> the type of the fifth valid value
   * @param <G> the type of the sixth valid value
   * @param <H> the type of the seventh valid value
   * @param <R> the result type
   * @return a combined {@code Validated<E, R>}
   */
  static <E, A, B, C, D, F, G, H, R> Validated<E, R> combine(
      Validated<E, A> v1,
      Validated<E, B> v2,
      Validated<E, C> v3,
      Validated<E, D> v4,
      Validated<E, F> v5,
      Validated<E, G> v6,
      Validated<E, H> v7,
      Semigroup<E> semigroup,
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
    Objects.requireNonNull(v1, "v1 must not be null");
    Objects.requireNonNull(v2, "v2 must not be null");
    Objects.requireNonNull(v3, "v3 must not be null");
    Objects.requireNonNull(v4, "v4 must not be null");
    Objects.requireNonNull(v5, "v5 must not be null");
    Objects.requireNonNull(v6, "v6 must not be null");
    Objects.requireNonNull(v7, "v7 must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    E err = null;
    A a = null;
    B b = null;
    C c = null;
    D d = null;
    F f = null;
    G g = null;
    H h = null;
    switch (v1) {
      case Valid<E, A>(var val) -> a = val;
      case Invalid<E, A>(var e) -> err = e;
    }
    switch (v2) {
      case Valid<E, B>(var val) -> b = val;
      case Invalid<E, B>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v3) {
      case Valid<E, C>(var val) -> c = val;
      case Invalid<E, C>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v4) {
      case Valid<E, D>(var val) -> d = val;
      case Invalid<E, D>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v5) {
      case Valid<E, F>(var val) -> f = val;
      case Invalid<E, F>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v6) {
      case Valid<E, G>(var val) -> g = val;
      case Invalid<E, G>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v7) {
      case Valid<E, H>(var val) -> h = val;
      case Invalid<E, H>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    if (err != null) return new Invalid<>(err);
    return new Valid<>(mapper.apply(a, b, c, d, f, g, h));
  }

  /**
   * Combines eight validated values, accumulating errors via the given {@link Semigroup} if any
   * inputs are invalid.
   *
   * @param v1 the first validated value
   * @param v2 the second validated value
   * @param v3 the third validated value
   * @param v4 the fourth validated value
   * @param v5 the fifth validated value
   * @param v6 the sixth validated value
   * @param v7 the seventh validated value
   * @param v8 the eighth validated value
   * @param semigroup the semigroup used to combine errors (must not be null)
   * @param mapper the function to apply when all inputs are valid (must not be null)
   * @param <E> the error type
   * @param <A> the type of the first valid value
   * @param <B> the type of the second valid value
   * @param <C> the type of the third valid value
   * @param <D> the type of the fourth valid value
   * @param <F> the type of the fifth valid value
   * @param <G> the type of the sixth valid value
   * @param <H> the type of the seventh valid value
   * @param <I> the type of the eighth valid value
   * @param <R> the result type
   * @return a combined {@code Validated<E, R>}
   */
  static <E, A, B, C, D, F, G, H, I, R> Validated<E, R> combine(
      Validated<E, A> v1,
      Validated<E, B> v2,
      Validated<E, C> v3,
      Validated<E, D> v4,
      Validated<E, F> v5,
      Validated<E, G> v6,
      Validated<E, H> v7,
      Validated<E, I> v8,
      Semigroup<E> semigroup,
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
    Objects.requireNonNull(v1, "v1 must not be null");
    Objects.requireNonNull(v2, "v2 must not be null");
    Objects.requireNonNull(v3, "v3 must not be null");
    Objects.requireNonNull(v4, "v4 must not be null");
    Objects.requireNonNull(v5, "v5 must not be null");
    Objects.requireNonNull(v6, "v6 must not be null");
    Objects.requireNonNull(v7, "v7 must not be null");
    Objects.requireNonNull(v8, "v8 must not be null");
    Objects.requireNonNull(semigroup, "semigroup must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    E err = null;
    A a = null;
    B b = null;
    C c = null;
    D d = null;
    F f = null;
    G g = null;
    H h = null;
    I i = null;
    switch (v1) {
      case Valid<E, A>(var val) -> a = val;
      case Invalid<E, A>(var e) -> err = e;
    }
    switch (v2) {
      case Valid<E, B>(var val) -> b = val;
      case Invalid<E, B>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v3) {
      case Valid<E, C>(var val) -> c = val;
      case Invalid<E, C>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v4) {
      case Valid<E, D>(var val) -> d = val;
      case Invalid<E, D>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v5) {
      case Valid<E, F>(var val) -> f = val;
      case Invalid<E, F>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v6) {
      case Valid<E, G>(var val) -> g = val;
      case Invalid<E, G>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v7) {
      case Valid<E, H>(var val) -> h = val;
      case Invalid<E, H>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    switch (v8) {
      case Valid<E, I>(var val) -> i = val;
      case Invalid<E, I>(var e) -> err = (err == null) ? e : semigroup.combine(err, e);
    }
    if (err != null) return new Invalid<>(err);
    return new Valid<>(mapper.apply(a, b, c, d, f, g, h, i));
  }
}
