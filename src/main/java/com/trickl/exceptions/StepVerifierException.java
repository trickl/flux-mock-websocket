package com.trickl.exceptions;

public class StepVerifierException extends RuntimeException {

  private static final long serialVersionUID = -1761231643713163261L;

  public StepVerifierException(String message) {
    super(message);
  }

  public StepVerifierException(String message, Throwable cause) {
    super(message, cause);
  }

  public StepVerifierException(Throwable cause) {
    super(cause);
  }
}
