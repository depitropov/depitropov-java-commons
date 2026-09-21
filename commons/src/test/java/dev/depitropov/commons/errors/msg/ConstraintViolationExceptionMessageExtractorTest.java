package dev.depitropov.commons.errors.msg;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ConstraintViolationExceptionMessageExtractorTest {

  private final ConstraintViolationExceptionMessageExtractor instance =
      new ConstraintViolationExceptionMessageExtractor();

  @Test
  void getMessage() {
    assertThat(
            instance.getMessage(
                new ConstraintViolationException(
                    Set.of(
                        new ConstraintViolationTest("Msg 1"),
                        new ConstraintViolationTest("Msg2")))))
        .isEqualTo("Msg 1, Msg2");

    assertThat(
            instance.getMessage(
                new ConstraintViolationException(
                    Set.of(
                        new ConstraintViolationTest("Msg 1", new PathImpl("zipCode")),
                        new ConstraintViolationTest("Msg2", new PathImpl("name"))))))
        .isEqualTo("name:Msg2, zipCode:Msg 1");
  }

  private record PathImpl(String value) implements Path {

    @Override
    public String toString() {
      return value;
    }

    @Override
    public Iterator<Node> iterator() {
      return null;
    }

    @Override
    public void forEach(final Consumer<? super Node> action) {
      Path.super.forEach(action);
    }

    @Override
    public Spliterator<Node> spliterator() {
      return Path.super.spliterator();
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static class ConstraintViolationTest
      implements ConstraintViolation<ConstraintViolationTest> {

    private final String message;

    private final Path propertyPath;

    public ConstraintViolationTest(final String message) {
      this(message, null);
    }

    public ConstraintViolationTest(final String message, final Path propertyPath) {
      this.message = message;
      this.propertyPath = propertyPath;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public String getMessageTemplate() {
      return null;
    }

    @Override
    public ConstraintViolationTest getRootBean() {
      return null;
    }

    @Override
    public Class getRootBeanClass() {
      return null;
    }

    @Override
    public Object getLeafBean() {
      return null;
    }

    @Override
    public Object[] getExecutableParameters() {
      return new Object[0];
    }

    @Override
    public Object getExecutableReturnValue() {
      return null;
    }

    @Override
    public Path getPropertyPath() {
      return propertyPath;
    }

    @Override
    public Object getInvalidValue() {
      return null;
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
      return null;
    }

    @Override
    public Object unwrap(final Class aClass) {
      return null;
    }
  }
}
