package com.sni.bokaticowork.core.utils.error;


public final class ErrorCode {

    //Request errors
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";

    // Resource errors
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String RESOURCE_ALREADY_EXISTS = "RESOURCE_ALREADY_EXISTS";

    // Validation errors
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String MISSING_PARAMETER = "MISSING_PARAMETER";

    // Authentication errors
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";

    // Server errors
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";

    // File errors
    public static final String FILE_STORAGE_ERROR = "FILE_STORAGE_ERROR";
    public static final String FILE_SIZE_EXCEEDED = "FILE_SIZE_EXCEEDED";
    public static final String INVALID_FILE_FORMAT = "INVALID_FILE_FORMAT";

    // HTTP errors
    public static final String METHOD_NOT_SUPPORTED = "METHOD_NOT_SUPPORTED";
    public static final String MEDIA_TYPE_NOT_SUPPORTED = "MEDIA_TYPE_NOT_SUPPORTED";
    public static final String ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";

    // Business logic errors
    public static final String CONFLICT = "CONFLICT";
    public static final String OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED";
    public static final String INSUFFICIENT_PRIVILEGES = "INSUFFICIENT_PRIVILEGES";

    private ErrorCode() {
        throw new IllegalStateException("Utility class");
    }
}