package com.sni.bokaticowork.core.retry.backoff;

public interface BackoffStrategy {

    long nextDelayMillis(int attempt);
}
