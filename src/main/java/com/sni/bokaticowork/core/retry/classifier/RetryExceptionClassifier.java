package com.sni.bokaticowork.core.retry.classifier;

public interface RetryExceptionClassifier {

    boolean isRetryable(Throwable throwable);
}
