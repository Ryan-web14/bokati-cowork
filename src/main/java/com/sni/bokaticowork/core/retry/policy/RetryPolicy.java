package com.sni.bokaticowork.core.retry.policy;

import com.sni.bokaticowork.core.retry.backoff.BackoffStrategy;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetryPolicy {

    private final int maxAttempts;
    private final BackoffStrategy backoffStrategy;
    private final boolean throwOnExhausted;
}
