package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ValidatedTest {

  // -------------------------------------------------------------------------
  // Construction
  // -------------------------------------------------------------------------

  @Test
  void valid_createsValidInstance() {
    Validated<String, Integer> result = Validated.valid(42);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<String, Integer>) result).value()).isEqualTo(42);
  }

  @Test
  void valid_nullValue_throwsNullPointerException() {
    assertThatNullPointerException().isThrownBy(() -> Validated.valid(null));
  }

  @Test
  void invalid_createsInvalidWithSingleError() {
    Validated<String, Integer> result = Validated.invalid("required");

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, Integer>) result).error()).isEqualTo("required");
  }

  @Test
  void invalid_nullError_throwsNullPointerException() {
    assertThatNullPointerException().isThrownBy(() -> Validated.invalid(null));
  }

  // -------------------------------------------------------------------------
  // map
  // -------------------------------------------------------------------------

  @Test
  void map_onValid_transformsValue() {
    Validated<String, Integer> result = Validated.<String, Integer>valid(5).map(n -> n * 2);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<String, Integer>) result).value()).isEqualTo(10);
  }

  @Test
  void map_onInvalid_returnsInvalidUnchanged() {
    Validated<String, Integer> invalid = Validated.invalid("error");

    Validated<String, String> result = invalid.map(Object::toString);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, String>) result).error()).isEqualTo("error");
  }

  // -------------------------------------------------------------------------
  // mapError
  // -------------------------------------------------------------------------

  @Test
  void mapError_onValid_returnsValidUnchanged() {
    Validated<String, Integer> valid = Validated.valid(99);

    Validated<Integer, Integer> result = valid.mapError(String::length);

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<Integer, Integer>) result).value()).isEqualTo(99);
  }

  @Test
  void mapError_onInvalid_transformsError() {
    Validated<String, Integer> invalid = Validated.invalid("err");

    Validated<Integer, Integer> result = invalid.mapError(String::length);

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<Integer, Integer>) result).error()).isEqualTo(3);
  }

  // -------------------------------------------------------------------------
  // fold
  // -------------------------------------------------------------------------

  @Test
  void fold_onValid_appliesValidFunction() {
    Validated<String, Integer> valid = Validated.valid(7);

    String result = valid.fold(error -> "invalid: " + error, value -> "valid: " + value);

    assertThat(result).isEqualTo("valid: 7");
  }

  @Test
  void fold_onInvalid_appliesInvalidFunction() {
    Validated<String, Integer> invalid = Validated.invalid("missing");

    String result = invalid.fold(error -> "error: " + error, value -> "valid: " + value);

    assertThat(result).isEqualTo("error: missing");
  }

  // -------------------------------------------------------------------------
  // isValid / isInvalid
  // -------------------------------------------------------------------------

  @Test
  void isValid_onValid_returnsTrue() {
    assertThat(Validated.valid("ok").isValid()).isTrue();
  }

  @Test
  void isValid_onInvalid_returnsFalse() {
    assertThat(Validated.invalid("bad").isValid()).isFalse();
  }

  @Test
  void isInvalid_onValid_returnsFalse() {
    assertThat(Validated.valid("ok").isInvalid()).isFalse();
  }

  @Test
  void isInvalid_onInvalid_returnsTrue() {
    assertThat(Validated.invalid("bad").isInvalid()).isTrue();
  }

  // -------------------------------------------------------------------------
  // Null parameter validation
  // -------------------------------------------------------------------------

  @Test
  void map_nullFunction_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> Validated.valid(1).map(null))
        .withMessage("f must not be null");
  }

  @Test
  void mapError_nullFunction_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> Validated.<String, Integer>invalid("err").mapError(null))
        .withMessage("f must not be null");
  }

  @Test
  void fold_nullOnInvalid_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> Validated.valid(1).fold(null, v -> v))
        .withMessage("onInvalid must not be null");
  }

  @Test
  void fold_nullOnValid_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> Validated.valid(1).fold(e -> e, null))
        .withMessage("onValid must not be null");
  }

  // -------------------------------------------------------------------------
  // andThen
  // -------------------------------------------------------------------------

  @Test
  void andThen_onValid_appliesFunction() {
    Validated<String, Integer> valid = Validated.valid(10);

    Validated<String, Integer> result = valid.andThen(n -> Validated.valid(n * 2));

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<String, Integer>) result).value()).isEqualTo(20);
  }

  @Test
  void andThen_onInvalid_propagatesError() {
    Validated<String, Integer> invalid = Validated.invalid("err");

    Validated<String, Integer> result = invalid.andThen(n -> Validated.valid(n * 2));

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, Integer>) result).error()).isEqualTo("err");
  }

  @Test
  void andThen_functionReturnsInvalid_propagatesNewError() {
    Validated<String, Integer> valid = Validated.valid(10);

    Validated<String, Integer> result = valid.andThen(n -> Validated.invalid("downstream error"));

    assertThat(result).isInstanceOf(Validated.Invalid.class);
    assertThat(((Validated.Invalid<String, Integer>) result).error()).isEqualTo("downstream error");
  }

  @Test
  void andThen_typeChange_works() {
    Validated<String, Integer> valid = Validated.valid(42);

    Validated<String, String> result = valid.andThen(n -> Validated.valid("user=" + n));

    assertThat(result).isInstanceOf(Validated.Valid.class);
    assertThat(((Validated.Valid<String, String>) result).value()).isEqualTo("user=42");
  }

  @Test
  void andThen_nullFunction_throwsNPE() {
    assertThatNullPointerException()
        .isThrownBy(() -> Validated.valid(1).andThen(null))
        .withMessage("f must not be null");
  }

  // -------------------------------------------------------------------------
  // getOrElseThrow
  // -------------------------------------------------------------------------

  @Test
  void getOrElseThrow_onValid_returnsValue() throws Exception {
    Validated<String, Integer> valid = Validated.valid(42);

    int result = valid.getOrElseThrow(RuntimeException::new);

    assertThat(result).isEqualTo(42);
  }

  @Test
  void getOrElseThrow_onInvalid_throwsMappedException() {
    Validated<String, Integer> invalid = Validated.invalid("bad input");

    assertThatThrownBy(() -> invalid.getOrElseThrow(msg -> new IllegalArgumentException(msg)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bad input");
  }

  @Test
  void getOrElseThrow_nullExceptionMapper_throwsNPE() {
    assertThatNullPointerException()
        .isThrownBy(() -> Validated.valid(1).getOrElseThrow(null))
        .withMessage("exceptionMapper must not be null");
  }
}
