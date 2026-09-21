package dev.depitropov.commons;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DEObjectUtilsTest {

  @Test
  void testSafeGet() {
    final String testStr = "test";

    assertThat(DEObjectUtils.safeGet(null, String::length)).isNull();

    final Object objA = new Object();
    final Object objDefault = new Object();

    Object res = DEObjectUtils.safeGet(testStr, str -> objA);
    assertThat(res).isEqualTo(objA);

    res = DEObjectUtils.safeGet(null, str -> objA);
    assertThat(res).isNull();

    res = DEObjectUtils.safeGet(null, str -> objA, objDefault);
    assertThat(res).isEqualTo(objDefault);
  }

  @Test
  void testSafeGetBoolean() {

    boolean res = DEObjectUtils.safeGet(null, (obj) -> true, false);
    assertThat(res).isFalse();

    res = DEObjectUtils.safeGet(null, (obj) -> false, true);
    assertThat(res).isTrue();

    res = DEObjectUtils.safeGet("", String::isEmpty, false);
    assertThat(res).isTrue();

    res = DEObjectUtils.safeGet("dd", String::isEmpty, false);
    assertThat(res).isFalse();
  }

  @Test
  void testSafeGetInt() {

    int res = DEObjectUtils.safeGet(null, (obj) -> 4, 0);
    assertThat(res).isZero();

    res = DEObjectUtils.safeGet(null, (obj) -> 4, 1);
    assertThat(res).isEqualTo(1);

    res = DEObjectUtils.safeGet("", String::length, 10);
    assertThat(res).isZero();

    res = DEObjectUtils.safeGet("dd", String::length, 0);
    assertThat(res).isEqualTo(2);
  }

  @Test
  void testSafeGetLong() {

    long res = DEObjectUtils.safeGet(null, (obj) -> 4L, 0L);
    assertThat(res).isZero();

    res = DEObjectUtils.safeGet(null, (obj) -> 4L, 1L);
    assertThat(res).isEqualTo(1L);

    res = DEObjectUtils.safeGet("", (str) -> (long) str.length(), 10L);
    assertThat(res).isZero();

    res = DEObjectUtils.safeGet("dd", (str) -> (long) str.length(), 0L);
    assertThat(res).isEqualTo(2L);
  }
}
