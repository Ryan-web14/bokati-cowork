package com.sni.bokaticowork.core.exception;

import com.sni.bokaticowork.core.exception.customs.*;
import com.sni.bokaticowork.core.utils.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorResponse errorResponse;

    // ── Business / domain exceptions ─────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(ResourceAlreadyExistException.class)
    public ResponseEntity<Object> handleResourceAlreadyExists(ResourceAlreadyExistException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.RESOURCE_ALREADY_EXISTS, HttpStatus.CONFLICT,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidation(ValidationException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                ex.getMessage(), request, ex, ex.getErrors()));
    }

    // ── Auth / access exceptions ──────────────────────────────────────────────

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                "Access to this resource is forbidden.", request, ex, null));
    }

    @ExceptionHandler(AccesDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccesDeniedException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
                "Access denied.", request, ex, null));
    }

    @ExceptionHandler(BadCredentialException.class)
    public ResponseEntity<Object> handleBadCredential(BadCredentialException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                "Invalid credentials provided.", request, ex, null));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Object> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.ACCOUNT_LOCKED, HttpStatus.UNAUTHORIZED,
                ex.getMessage(), request, ex, null));
    }

    // ── Spring MVC / binding exceptions ──────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                "Invalid request parameters.", request, ex, errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBind(BindException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                "Invalid request parameters.", request, ex, errors));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingRequestPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                "Missing required request part: " + ex.getRequestPartName() + ".", request, ex, null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingRequestParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName() + ".", request, ex, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed.", request, ex, null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String supported = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("unknown");
        return respond(errorResponse.build(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type '" + ex.getContentType() + "' is not supported. Supported: " + supported + ".",
                request, ex, null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.METHOD_NOT_SUPPORTED, HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint.", request, ex, null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.ENDPOINT_NOT_FOUND, HttpStatus.NOT_FOUND,
                "The requested endpoint does not exist.", request, ex, null));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again or contact support.", request, ex, null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<Object> respond(ApiError apiError) {
        return new ResponseEntity<>(apiError, HttpStatus.valueOf(apiError.getStatus()));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
