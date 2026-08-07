package io.github.jpalmerr.valid4j.functions;

/**
 * A function that accepts six arguments and produces a result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <D> the type of the fourth argument
 * @param <F> the type of the fifth argument
 * @param <G> the type of the sixth argument
 * @param <R> the return type
 */
@FunctionalInterface
public interface Function6<A, B, C, D, F, G, R> {
  /**
   * Applies this function to the given arguments.
   *
   * @param a the first argument
   * @param b the second argument
   * @param c the third argument
   * @param d the fourth argument
   * @param f the fifth argument
   * @param g the sixth argument
   * @return the function result
   */
  R apply(A a, B b, C c, D d, F f, G g);
}
