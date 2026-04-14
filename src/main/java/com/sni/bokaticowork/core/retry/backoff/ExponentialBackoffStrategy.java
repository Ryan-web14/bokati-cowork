package com.sni.bokaticowork.core.retry.backoff;

public class ExponentialBackoffStrategy implements BackoffStrategy {

    private final long initialDelayMillis;
    private final double multiplier;
    private final long maxDelayMillis;
    private final double jitterFactor;

    public ExponentialBackoffStrategy(long initialDelayMillis, double multiplier, long maxDelayMillis, double jitterFactor) {
        this.initialDelayMillis = initialDelayMillis;
        this.multiplier = multiplier;
        this.maxDelayMillis = maxDelayMillis;
        this.jitterFactor = jitterFactor;
    }

    @Override
    public long nextDelayMillis(int attempt) {
        if (attempt <= 1) {
            return applyJitter(initialDelayMillis);
        }

        double computed = initialDelayMillis * Math.pow(multiplier, attempt - 1);
        long capped = Math.min((long) computed, maxDelayMillis);
        return applyJitter(capped);
    }

    private long applyJitter(long baseDelay) {
        if (jitterFactor <= 0) {
            return baseDelay;
        }

        double delta = baseDelay * jitterFactor;
        double randomOffset = (Math.random() * (delta * 2)) - delta;
        return Math.max(0L, Math.round(baseDelay + randomOffset));
    }
}
