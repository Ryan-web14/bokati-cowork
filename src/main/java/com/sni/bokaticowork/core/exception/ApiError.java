package com.sni.bokaticowork.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    // Always present
    private String errorCode;
    private int status;
    private String message;
    private String traceId;

    // Validation field errors — included when non-empty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> errors;

    // Dev-only debug fields — null in production, excluded by NON_NULL
    private String path;
    private String debugMessage;
    private String exceptionName;
}
