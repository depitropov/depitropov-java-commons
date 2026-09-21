package dev.depitropov.commons.errors.msg;

import dev.depitropov.commons.errors.BusinessRuleException;
import dev.depitropov.commons.errors.ErrorCode;
import dev.depitropov.commons.errors.ExceptionMessageExtractor;

public class BusinessRuleExceptionMessageExtractor
    implements ExceptionMessageExtractor<BusinessRuleException> {

  private final boolean useOriginalMessage;

  public BusinessRuleExceptionMessageExtractor(final boolean useOriginalMessage) {
    this.useOriginalMessage = useOriginalMessage;
  }

  @Override
  public Class<BusinessRuleException> getExceptionClass() {
    return BusinessRuleException.class;
  }

  @Override
  public ErrorCode getErrorCode(final Throwable throwable) {
    final BusinessRuleException ex = (BusinessRuleException) throwable;

    return ex.getErrorCode();
  }

  @Override
  public String getMessage(final Throwable throwable) {
    final BusinessRuleException ex = (BusinessRuleException) throwable;

    if (useOriginalMessage) {

      return ex.getOriginalMessage();

    } else {

      return ex.getMessage();
    }
  }
}
