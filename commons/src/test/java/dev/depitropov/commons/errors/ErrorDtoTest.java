package dev.depitropov.commons.errors;

import static dev.depitropov.commons.DEResourceUtils.loadResource;
import static dev.depitropov.commons.DETestUtils.assertJson;
import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_GATEWAY_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.NOT_FOUND_GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class ErrorDtoTest {

  static final Instant TEST_INSTANT_20220905_081555_789 = Instant.ofEpochMilli(1652084155789L);

  static final List<String> ERROR_DETAILS = List.of("error detail1", "error detail 2");

  final ErrorDto testModel =
      new ErrorDto(
          TEST_INSTANT_20220905_081555_789,
          BAD_REQUEST_GENERAL,
          ERROR_DETAILS,
          new Exception("Test"));

  @SuppressWarnings("java:S2925")
  @SneakyThrows
  @Test
  void constructorTest() {
    Instant time = Instant.now();
    Thread.sleep(1);

    ErrorDto res = new ErrorDto();
    assertTrue(res.getTimestamp().isAfter(time));
    assertEquals(500, res.getHttpStatus());
    assertEquals(5009999, res.getErrorCode());
    assertEquals("INTERNAL_SERVER_ERROR_GENERAL", res.getErrorMessage());
    assertEquals(List.of(), res.getErrorDetails());

    time = Instant.now();
    Thread.sleep(1);

    res = new ErrorDto(NOT_FOUND_GENERAL);
    assertTrue(res.getTimestamp().isAfter(time));
    assertEquals(404, res.getHttpStatus());
    assertEquals(4049999, res.getErrorCode());
    assertEquals("NOT_FOUND_GENERAL", res.getErrorMessage());
    assertEquals(List.of(), res.getErrorDetails());

    time = Instant.now();
    Thread.sleep(1);

    res = new ErrorDto(BAD_GATEWAY_GENERAL, ERROR_DETAILS);
    assertTrue(res.getTimestamp().isAfter(time));
    assertEquals(502, res.getHttpStatus());
    assertEquals(5029999, res.getErrorCode());
    assertEquals("BAD_GATEWAY_GENERAL", res.getErrorMessage());
    assertEquals(ERROR_DETAILS, res.getErrorDetails());
  }

  @Test
  void getters() {
    assertEquals(TEST_INSTANT_20220905_081555_789, testModel.getTimestamp());
    assertEquals(400, testModel.getHttpStatus());
    assertEquals(4009999, testModel.getErrorCode());
    assertEquals("BAD_REQUEST_GENERAL", testModel.getErrorMessage());
    assertEquals(ERROR_DETAILS, testModel.getErrorDetails());
  }

  @SneakyThrows
  @Test
  void testSerialization() {
    final ObjectMapper mapper = new ObjectMapper();
    final JavaTimeModule javaTimeModule = new JavaTimeModule();
    mapper.registerModule(javaTimeModule);

    assertJson(loadResource("error-test1.json"), mapper.writeValueAsString(testModel));
  }

  @Test
  void toStringTest() {
    final String res = testModel.toString();
    assertTrue(
        res.endsWith(
            "["
                + "timestamp=2022-05-09T08:15:55.789Z,"
                + "timestamp.epoch=1652084155789,"
                + "httpStatus=400,"
                + "errorCode=4009999,"
                + "errorMessage=BAD_REQUEST_GENERAL,"
                + "errorDetails=[error detail1, error detail 2],"
                + "cause=java.lang.Exception: Test"
                + "]"),
        "Unexpected: " + res);

    assertTrue(res.startsWith("dev.depitropov.commons.errors.ErrorDto@"), "Unexpected: " + res);
  }
}
