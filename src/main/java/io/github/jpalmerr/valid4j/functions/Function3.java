package io.github.jpalmerr.valid4j.functions;

/**
 * A function that accepts three arguments and produces a result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <R> the return type
 */
@FunctionalInterface
public interface Function3<A, B, C, R> {
  R apply(A a, B b, C c);
}
