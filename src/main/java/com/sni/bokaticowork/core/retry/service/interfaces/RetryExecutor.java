package com.sni.bokaticowork.core.retry.service.interfaces;

import com.sni.bokaticowork.core.retry.policy.RetryPolicy;

import java.util.function.Supplier;

public interface RetryExecutor {

    <T> T execute(String operationName, RetryPolicy retryPolicy, Supplier<T> action);

    void run(String operationName, RetryPolicy retryPolicy, Runnable action);
}
