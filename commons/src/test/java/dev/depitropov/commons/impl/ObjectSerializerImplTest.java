package dev.depitropov.commons.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.type.SimpleType;
import dev.depitropov.commons.ClassTypeProvider;
import dev.depitropov.commons.ObjectSerializer;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("rawtypes")
class ObjectSerializerImplTest {

  private ObjectSerializer objUnderTest;

  @BeforeEach
  void setup() {
    objUnderTest = new ObjectSerializerImpl(new ObjectMapper());
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Data
  static class TestData {
    private long id;
    private String data;
  }

  @NoArgsConstructor
  static class TestDataInvalid {
    @SuppressWarnings("unused")
    public int getId() throws JacksonException {
      throw InvalidNullException.from(null, SimpleType.constructUnsafe(String.class), "Test error");
    }
  }

  @Test
  void toStringTestStrings() {
    assertThat(objUnderTest.toString(null)).isEmpty();
    assertThat(objUnderTest.toString("")).isEmpty();
    assertThat(objUnderTest.toString("string")).isEqualTo("string");
  }

  @Test
  void toStringThrowTestStrings() {
    assertThat(objUnderTest.toStringThrow(null)).isEmpty();
    assertThat(objUnderTest.toStringThrow("")).isEmpty();
    assertThat(objUnderTest.toStringThrow("string")).isEqualTo("string");
  }

  @Test
  void toStringTest() {

    final String dataJson = "{\"id\":0,\"data\":null}";

    assertThat(objUnderTest.toString(new TestData())).isEqualTo(dataJson);
  }

  @Test
  void toStringTestInvalid() {

    assertThat(objUnderTest.toString(new TestDataInvalid())).isNull();
  }

  @Test
  void toStringThrowTest() {

    final String dataJson = "{\"id\":0,\"data\":null}";

    assertThat(objUnderTest.toStringThrow(new TestData())).isEqualTo(dataJson);
  }

  @Test
  void toStringThrowTestInvalid() {

    final TestDataInvalid data = new TestDataInvalid();

    assertThat(
            assertThrows(RuntimeException.class, () -> objUnderTest.toStringThrow(data)).getCause())
        .isInstanceOf(JacksonException.class);
  }

  @Test
  void toObjectTestWithClass() {

    final String dataJson = "{\"id\":1,\"data\":\"test data\"}";

    final TestData serializedObject =
        objUnderTest.toObject(dataJson, ClassTypeProvider.of(TestData.class));

    assertThat(serializedObject.getId()).isEqualTo(1);
  }

  @Test
  void toObjectTestWithTypeReference() {

    final String dataJson =
        "[{\"id\":1,\"data\":\"test data\"}, {\"id\":2,\"data\":\"test data 2\"}]";

    final List<TestData> serializedObject =
        objUnderTest.toObject(dataJson, ClassTypeProvider.of(new TypeReference<>() {}));

    assertThat(serializedObject).hasSize(2);
    assertThat(serializedObject.get(0).getId()).isEqualTo(1);
    assertThat(serializedObject.get(1).getId()).isEqualTo(2);
  }

  @Test
  void toObjectTestWithTypeJavaType() {

    final String dataJson =
        "{\"id\":1,\"data\":\"test data\"}, {\"id\":2,\"data\":\"test data 2\"}";

    final TestData serializedObject =
        objUnderTest.toObject(
            dataJson, ClassTypeProvider.of(SimpleType.constructUnsafe(TestData.class)));

    assertThat(serializedObject.getId()).isEqualTo(1);
  }

  @Test
  void toObjectTestInvalid() {

    final String dataJson = "invalid";

    final List<TestData> serializedObject =
        objUnderTest.toObject(
            dataJson, ClassTypeProvider.of(SimpleType.constructUnsafe(TestData.class)));

    assertThat(serializedObject).isNull();
  }

  @Test
  void toObjectThrow() {
    final ClassTypeProvider<Map> classMap = ClassTypeProvider.of(Map.class);

    assertThat(
            assertThrows(
                    RuntimeException.class, () -> objUnderTest.toObjectThrow("invalid", classMap))
                .getCause())
        .isInstanceOf(JsonProcessingException.class);
  }

  @Test
  void toObjectThrowReturnString() {
    String res = objUnderTest.toObjectThrow("string", ClassTypeProvider.of(String.class));
    assertThat(res).isEqualTo("string");

    res = objUnderTest.toObjectThrow("string", ClassTypeProvider.of(new TypeReference<>() {}));
    assertThat(res).isEqualTo("string");

    res =
        objUnderTest.toObjectThrow(
            "string", ClassTypeProvider.of(SimpleType.constructUnsafe(String.class)));
    assertThat(res).isEqualTo("string");
  }

  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  static class TestT<T> {
    T value;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  abstract static class TBase {
    String valueBase;
  }

  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @EqualsAndHashCode(callSuper = true)
  static class TChild extends TBase {
    String valueChild;

    public TChild(final String valueBase, final String valueChild) {
      super(valueBase);
      this.valueChild = valueChild;
    }
  }

  private <T> TestT<T> testRead(final String json, final Class<T> tClass) {

    final JavaType type =
        new ObjectMapper().getTypeFactory().constructParametricType(TestT.class, tClass);

    return objUnderTest.toObject(json, ClassTypeProvider.of(type));
  }

  @Test
  void toObject_ShouldReturnOk_WithJavaType() {

    final String dataJson = "{\"value\":{\"valueBase\":\"base\",\"valueChild\":\"child\"}}";

    final TestT<TChild> data = new TestT<>(new TChild("base", "child"));
    final String dJson = objUnderTest.toString(data);

    assertThat(dJson).isEqualTo(dataJson);

    final TestT<TChild> serializedObject = testRead(dJson, TChild.class);

    assertThat(serializedObject).isEqualTo(data);
  }
}
