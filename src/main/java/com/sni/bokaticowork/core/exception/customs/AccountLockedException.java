package com.sni.bokaticowork.core.exception.customs;

import com.sni.bokaticowork.core.utils.error.ErrorCode;

public class AccountLockedException extends BaseException {

    private static final String ERROR_CODE = ErrorCode.ACCOUNT_LOCKED;

    public AccountLockedException(String message) {
        super(message, ERROR_CODE);
    }
}
