package dev.depitropov.commons.errors.msg;

import static dev.depitropov.commons.errors.ErrorUtils.getNotEmptyMessageOrExceptionClassSimpleName;
import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import dev.depitropov.commons.errors.ErrorCode;
import dev.depitropov.commons.errors.ExceptionMessageExtractor;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

public class ConstraintViolationExceptionMessageExtractor
    implements ExceptionMessageExtractor<ConstraintViolationException> {

  public static final String DELIMITER = ", ";

  @Override
  public Class<ConstraintViolationException> getExceptionClass() {
    return ConstraintViolationException.class;
  }

  @Override
  public ErrorCode getErrorCode(final Throwable throwable) {
    return BAD_REQUEST_GENERAL;
  }

  @Override
  public String getMessage(final Throwable throwable) {
    final ConstraintViolationException ex = (ConstraintViolationException) throwable;

    final String message;

    if (isNotEmpty(ex.getConstraintViolations())) {

      message =
          ex.getConstraintViolations().stream()
              .map(
                  iter ->
                      iter.getPropertyPath() != null
                          ? iter.getPropertyPath().toString() + ":" + iter.getMessage()
                          : iter.getMessage())
              .sorted()
              .collect(Collectors.joining(DELIMITER));

    } else {

      message = getNotEmptyMessageOrExceptionClassSimpleName(ex);
    }

    return message;
  }
}
