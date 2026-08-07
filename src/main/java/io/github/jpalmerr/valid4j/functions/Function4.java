package io.github.jpalmerr.valid4j.functions;

/**
 * A function that accepts four arguments and produces a result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <D> the type of the fourth argument
 * @param <R> the return type
 */
@FunctionalInterface
public interface Function4<A, B, C, D, R> {
  /**
   * Applies this function to the given arguments.
   *
   * @param a the first argument
   * @param b the second argument
   * @param c the third argument
   * @param d the fourth argument
   * @return the function result
   */
  R apply(A a, B b, C c, D d);
}
