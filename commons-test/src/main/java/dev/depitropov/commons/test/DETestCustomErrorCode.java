package dev.depitropov.commons.test;

import dev.depitropov.commons.errors.ErrorCode;

public class DETestCustomErrorCode implements ErrorCode {

  private final int errorCode;

  private final String errorMessage;

  public DETestCustomErrorCode(final int errorCode, final String errorMessage) {
    this.errorCode = errorCode;

    this.errorMessage = errorMessage;
  }

  @Override
  public int errorCode() {
    return errorCode;
  }

  @Override
  public String errorMessage() {
    return errorMessage;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof final ErrorCode other)) {
      return false;
    }

    return errorCode() == other.errorCode() && errorMessage().equals(other.errorMessage());
  }

  @Override
  public int hashCode() {
    int result = Integer.hashCode(errorCode());

    result = 31 * result + errorMessage().hashCode();

    return result;
  }

  @Override
  public String toString() {
    return errorMessage();
  }
}
