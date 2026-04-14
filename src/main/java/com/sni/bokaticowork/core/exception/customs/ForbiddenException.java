package com.sni.bokaticowork.core.exception.customs;


import com.sni.bokaticowork.core.utils.error.ErrorCode;

public class ForbiddenException extends BaseException {

  private static final String ERROR_CODE = ErrorCode.FORBIDDEN;

  public ForbiddenException(String message) {
    super(message, ERROR_CODE);
  }

  public ForbiddenException(String message, Throwable cause) {
    super(message, ERROR_CODE, cause);
  }

}
