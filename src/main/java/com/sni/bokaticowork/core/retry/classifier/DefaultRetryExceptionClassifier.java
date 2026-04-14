package com.sni.bokaticowork.core.retry.classifier;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.Set;

public class DefaultRetryExceptionClassifier implements RetryExceptionClassifier {

    private final Set<Class<? extends Throwable>> retryableTypes;

    public DefaultRetryExceptionClassifier(Set<Class<? extends Throwable>> retryableTypes) {
        this.retryableTypes = retryableTypes;
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        if (throwable instanceof TransientDataAccessException
                || throwable instanceof ResourceAccessException
                || throwable instanceof SocketTimeoutException) {
            return true;
        }

        return retryableTypes.stream().anyMatch(type -> type.isAssignableFrom(throwable.getClass()));
    }
}
