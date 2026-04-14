package com.sni.bokaticowork.core.retry.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetryContext {

    private final String operationName;
    private final int attempt;
    private final int maxAttempts;
    private final Throwable lastError;
}
