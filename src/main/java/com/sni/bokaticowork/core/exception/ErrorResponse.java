package com.sni.bokaticowork.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ErrorResponse {

    //TODO: must define here because spring cannot autowire primitive type or their wrapper
   @Value("true")
    private boolean isDevEnvironment;

    public ErrorResponse(boolean isDevEnvironment) {
        this.isDevEnvironment = isDevEnvironment;
    }

    public ApiError buildErrorResponse(String errorCode, HttpStatus status,
                                       String devMessage, String prodMessage,
                                       HttpServletRequest path, Throwable ex, List<String> errors, Exception stackTrace) {

        String traceId = UUID.randomUUID().toString();
        String finalMessage = isDevEnvironment ? devMessage : prodMessage;

        ApiError.ApiErrorBuilder builder = ApiError.builder()
                .errorCode(errorCode)
                .status(status.value())
                .message(finalMessage)
                .timestamp(LocalDateTime.now())
                .traceId(traceId);

        if (isDevEnvironment) {
            builder.path(getRequestPath(path))
                    .errors(errors)
                    .debugMessage(ex != null ? ex.getMessage() : null)
                    .exceptionName(ex != null ? ex.getClass().getName() : null)
                    .stackTrace(getStackTrace(stackTrace));
        } else {
            builder.suggestedAction("Please contact support with the provided trace ID.");
        }

        return builder.build();
    }

    private String getRequestPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}