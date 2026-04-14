package com.sni.bokaticowork.core.exception.customs;


import com.sni.bokaticowork.core.utils.error.ErrorCode;

public class UnauthorizedException extends BaseException {

  private static final String ERROR_CODE = ErrorCode.UNAUTHORIZED;

  public UnauthorizedException(String message){
    super(message, ERROR_CODE);
  }

  public UnauthorizedException(String message, Throwable cause){
    super(message, ERROR_CODE, cause);
  }
}
