package com.sni.bokaticowork.core.exception;

import com.sni.bokaticowork.core.exception.customs.*;
import com.sni.bokaticowork.core.utils.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    private final ErrorResponse errorResponse;
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError){
        return new ResponseEntity<>(apiError, HttpStatus.valueOf(apiError.getStatus()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleRessourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                "The requested ressouce was not found",
                request,
                ex,
                null,
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.BAD_REQUEST,
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                "Bad request error",
                request,
                ex,
                null,
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictException(
            ConflictException ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.CONFLICT,
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Resource conflict error",
                request,
                ex,
                null,
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                "Authorization required",
                request,
                ex,
                null,
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidenException(
            ForbiddenException ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.FORBIDDEN,
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                "Access to the resource is forbidden",
                request,
                ex,
                null,
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(
            ValidationException ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                "Validation error",
                request,
                ex,
                ex.getErrors(),
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        ApiError error = errorResponse.buildErrorResponse(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Invalid request parameters",
                "Validation error",
                request,
                ex,
                errors,
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(
            BindException ex, HttpServletRequest request
    ) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        ApiError error = errorResponse.buildErrorResponse(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Invalid request parameters",
                "Validation error",
                request,
                ex,
                errors,
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex, HttpServletRequest request
    ) {
        ApiError error = errorResponse.buildErrorResponse(
                ErrorCode.BAD_REQUEST,
                HttpStatus.BAD_REQUEST,
                "Missing multipart request part: " + ex.getRequestPartName(),
                "Bad request error",
                request,
                ex,
                List.of("Missing multipart request part: " + ex.getRequestPartName()),
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request
    ) {
        String message = "Missing request parameter: " + ex.getParameterName();
        ApiError error = errorResponse.buildErrorResponse(
                ErrorCode.BAD_REQUEST,
                HttpStatus.BAD_REQUEST,
                message,
                "Bad request error",
                request,
                ex,
                List.of(message),
                ex);
        return buildResponseEntity(error);
    }

    @ExceptionHandler(BadCredentialException.class)
    public ResponseEntity<Object> handleBadCredentialException(
            BadCredentialException ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                "Invalid credentials provided",
                request,
                ex,
                null,
                ex);
        return buildResponseEntity(error);
    }

    private String formatFieldError(FieldError error) {
        Object rejectedValue = error.getRejectedValue();
        String value = rejectedValue == null ? "null" : rejectedValue.toString();
        return error.getField() + ": " + error.getDefaultMessage() + " (rejected value: " + value + ")";
    }

    @ExceptionHandler(AccesDeniedException.class)
    public ResponseEntity<Object> handleAccesDeniedException(
            AccesDeniedException ex, HttpServletRequest request
    ){
        ApiError error =errorResponse.buildErrorResponse(
                ErrorCode.ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                "Access denied",
                request,
                ex,
                null,
                ex);

        return buildResponseEntity(error);
    }



    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(
            Exception ex, HttpServletRequest request
    ) {
        ApiError error =errorResponse
                .buildErrorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                "An unexpected error occurred",
                request,
                ex,
                null,
                ex);
        LOGGER.error("Unexpected error occurred", ex);
        return buildResponseEntity(error);
    }
}
