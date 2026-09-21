package dev.depitropov.commons.test;

import static dev.depitropov.commons.test.DETestUtils.assertJson;
import static dev.depitropov.commons.test.DETestUtils.assertThrowableCauseContains;
import static dev.depitropov.commons.test.DETestUtils.assertThrowsMessage;
import static dev.depitropov.commons.test.DETestUtils.getNowAfter1ms;
import static dev.depitropov.commons.test.DETestUtils.toFormattedJson;
import static dev.depitropov.commons.test.DETestUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class DETestUtilsTest {

  static final Instant TEST_INSTANT_20220905_081555_789 = Instant.ofEpochMilli(1652084155789L);

  static class TestJsonObj {
    public String key1 = "value";

    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd'T'HH:mm:ss",
        timezone = "UTC")
    public Instant key2 = TEST_INSTANT_20220905_081555_789;
  }

  @Test
  void toJsonTest() {
    final LinkedHashMap<String, Object> testObj = new LinkedHashMap<>();
    testObj.put("key1", "value");
    testObj.put("key2", TEST_INSTANT_20220905_081555_789);

    assertEquals("{\"key1\":\"value\",\"key2\":1652084155.789000000}", toJson(testObj));

    assertEquals(
        "{\"key1\":\"value\",\"key2\":\"2022-05-09T08:15:55\"}", toJson(new TestJsonObj()));
  }

  @Test
  void assertJsonTest() {
    final Map<String, Object> jsonMap = new LinkedHashMap<>();
    jsonMap.put("key1", 19);
    jsonMap.put("key2", "value");

    assertJson("{\n" + "  \"key1\" : 19,\n" + "  \"key2\" : \"value\"\n" + "}", jsonMap);

    assertJson(
        "{\n" + "  \"key1\" : 19,\n" + "  \"key2\" : \"value\"\n" + "}",
        "{\"key1\": 19,\"key2\":\"value\"}");
  }

  @Test
  void toFormattedJsonTest() {
    assertThat("{\n" + "  \"key1\" : 19,\n" + "  \"key2\" : \"value\"\n" + "}")
        .isEqualTo(toFormattedJson("{\"key1\": 19,\"key2\":\"value\"}"));
  }

  @Test
  void getNowAfter1msTest() {
    final Instant now = Instant.now();

    final Instant nowAfter1ms = getNowAfter1ms();

    assertTrue(nowAfter1ms.isAfter(now));
  }

  @Test
  void assertThrowsMessageTest() {
    assertThrowsMessage(
        IllegalStateException.class,
        () -> {
          throw new IllegalStateException("test 1");
        },
        "test 1");

    assertThrowsMessage(
        IllegalStateException.class,
        () -> {
          throw new IllegalStateException("test 1");
        },
        "test %d",
        1);

    AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () ->
                assertThrowsMessage(
                    IllegalArgumentException.class,
                    () -> {
                      throw new IllegalStateException("test 1");
                    },
                    "test 1"));
    assertEquals(
        "Unexpected exception type thrown, expected: "
            + "<java.lang.IllegalArgumentException> but was: <java.lang.IllegalStateException>",
        error.getMessage());

    error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertThrowsMessage(IllegalArgumentException.class, () -> {}, "test 1"));
    assertEquals(
        "Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.",
        error.getMessage());
  }

  @SuppressWarnings("ThrowableNotThrown")
  @Test
  void assertThrowableCauseContainsTest() {
    final IllegalStateException causeLevel1 = new IllegalStateException("test");
    final Throwable tLevel1 = new RuntimeException(causeLevel1);

    Throwable res = assertThrowableCauseContains(tLevel1, IllegalStateException.class);
    assertEquals(causeLevel1, res);

    final IllegalArgumentException causeLevel2 = new IllegalArgumentException(causeLevel1);
    final Throwable tLevel2 = new RuntimeException(causeLevel2);

    res = assertThrowableCauseContains(tLevel2, IllegalStateException.class);
    assertEquals(causeLevel1, res);

    final AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertThrowableCauseContains(tLevel2, NullPointerException.class));
    assertEquals(
        "Unexpected cause: class java.lang.NullPointerException "
            + "in java.lang.RuntimeException: java.lang.IllegalArgumentException: "
            + "java.lang.IllegalStateException: test",
        error.getMessage());
  }
}
