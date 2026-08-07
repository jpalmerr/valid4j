/**
 * Typed error accumulation via applicative validation, with zero runtime dependencies.
 *
 * <p>{@link io.github.jpalmerr.valid4j.Validated} is the core type; {@link
 * io.github.jpalmerr.valid4j.ValidatedNel} is the convenience layer over {@link
 * io.github.jpalmerr.valid4j.NonEmptyList} error accumulation.
 */
module io.github.jpalmerr.valid4j {
  exports io.github.jpalmerr.valid4j;
  exports io.github.jpalmerr.valid4j.functions;
}
