package io.github.jpalmerr.valid4j.functions;

/**
 * A function that accepts eight arguments and produces a result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <D> the type of the fourth argument
 * @param <E> the type of the fifth argument
 * @param <F> the type of the sixth argument
 * @param <G> the type of the seventh argument
 * @param <H> the type of the eighth argument
 * @param <R> the return type
 */
@FunctionalInterface
public interface Function8<A, B, C, D, E, F, G, H, R> {
  R apply(A a, B b, C c, D d, E e, F f, G g, H h);
}
