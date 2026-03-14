package io.github.jpalmerr.valid4j.functions;

/**
 * A function that accepts five arguments and produces a result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <D> the type of the fourth argument
 * @param <E> the type of the fifth argument
 * @param <R> the return type
 */
@FunctionalInterface
public interface Function5<A, B, C, D, E, R> {
  R apply(A a, B b, C c, D d, E e);
}
