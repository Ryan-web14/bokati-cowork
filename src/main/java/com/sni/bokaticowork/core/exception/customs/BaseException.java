package com.sni.bokaticowork.core.exception.customs;

public abstract class BaseException extends RuntimeException{

    private final String errorCode;

    /** Code stable et precis de la regle en cause (lot B10), que le frontend peut tester ; vide sinon. */
    private String reason;

    public BaseException(String message, String errorCode ) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }



    public String getReason() {
        return reason;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseException> T withReason(String reason) {
        this.reason = reason;
        return (T) this;
    }
}
