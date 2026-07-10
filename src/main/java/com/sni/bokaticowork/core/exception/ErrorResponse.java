package com.sni.bokaticowork.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class ErrorResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorResponse.class);

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    public ApiError build(String errorCode, HttpStatus status, String message,
                          HttpServletRequest request, Throwable ex, List<String> errors) {

        String traceId = UUID.randomUUID().toString();
        String path = request != null ? request.getRequestURI() : null;

        if (status.is5xxServerError()) {
            LOGGER.error("[traceId={}] {} {} · {}", traceId, errorCode, path, message, ex);
        } else {
            LOGGER.warn("[traceId={}] {} {} · {}", traceId, errorCode, path,
                    ex != null ? ex.getMessage() : message);
        }

        ApiError.ApiErrorBuilder builder = ApiError.builder()
                .errorCode(errorCode)
                .status(status.value())
                .message(message)
                .traceId(traceId)
                .errors(errors);

        if (devMode) {
            builder.path(path)
                    .debugMessage(ex != null ? buildDebugMessage(ex) : null)
                    .exceptionName(ex != null ? ex.getClass().getName() : null);
        }

        return builder.build();
    }

    private String buildDebugMessage(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getSimpleName()).append(": ").append(ex.getMessage());
        Throwable cause = ex.getCause();
        while (cause != null) {
            sb.append(" → ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }
        return sb.toString();
    }
}
