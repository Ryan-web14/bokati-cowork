package com.sni.bokaticowork.core.retry.service.implementation;

import com.sni.bokaticowork.core.retry.classifier.RetryExceptionClassifier;
import com.sni.bokaticowork.core.retry.exception.RetryExhaustedException;
import com.sni.bokaticowork.core.retry.policy.RetryPolicy;
import com.sni.bokaticowork.core.retry.service.interfaces.RetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRetryExecutor implements RetryExecutor {

    private final RetryExceptionClassifier retryExceptionClassifier;

    @Override
    public <T> T execute(String operationName, RetryPolicy retryPolicy, Supplier<T> action) {
        Throwable lastError = null;

        for (int attempt = 1; attempt <= retryPolicy.getMaxAttempts(); attempt++) {
            try {
                return action.get();
            } catch (Throwable ex) {
                lastError = ex;

                if (!retryExceptionClassifier.isRetryable(ex) || attempt >= retryPolicy.getMaxAttempts()) {
                    break;
                }

                long delay = retryPolicy.getBackoffStrategy().nextDelayMillis(attempt);
                log.warn("Retrying operation {} after attempt {}/{} due to {}",
                        operationName, attempt, retryPolicy.getMaxAttempts(), ex.getClass().getSimpleName(), ex);
                sleep(delay);
            }
        }

        if (retryPolicy.isThrowOnExhausted()) {
            throw new RetryExhaustedException("Retry exhausted for operation: " + operationName, lastError);
        }

        if (lastError instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        throw new RuntimeException("Retry exhausted for operation: " + operationName, lastError);
    }

    @Override
    public void run(String operationName, RetryPolicy retryPolicy, Runnable action) {
        execute(operationName, retryPolicy, () -> {
            action.run();
            return null;
        });
    }

    private void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", ex);
        }
    }
}
