package com.sni.bokaticowork.core.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    // Basic error information (always included)
    private String errorCode;
    private int status;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // Detailed information (conditionally included)
    private String path;
    private String stackTrace;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    private String debugMessage;
    private String exceptionName;
    private String suggestedAction;
    private String traceId; // Useful for log correlation
}