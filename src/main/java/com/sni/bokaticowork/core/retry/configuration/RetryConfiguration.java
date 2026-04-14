package com.sni.bokaticowork.core.retry.configuration;

import com.sni.bokaticowork.core.retry.backoff.BackoffStrategy;
import com.sni.bokaticowork.core.retry.backoff.ExponentialBackoffStrategy;
import com.sni.bokaticowork.core.retry.classifier.DefaultRetryExceptionClassifier;
import com.sni.bokaticowork.core.retry.classifier.RetryExceptionClassifier;
import com.sni.bokaticowork.core.retry.policy.RetryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class RetryConfiguration {

    @Bean
    public BackoffStrategy defaultBackoffStrategy() {
        return new ExponentialBackoffStrategy(100L, 2.0d, 2_000L, 0.15d);
    }

    @Bean
    public RetryExceptionClassifier retryExceptionClassifier() {
        return new DefaultRetryExceptionClassifier(Set.of());
    }

    @Bean
    public RetryPolicy defaultRetryPolicy(BackoffStrategy defaultBackoffStrategy) {
        return RetryPolicy.builder()
                .maxAttempts(3)
                .backoffStrategy(defaultBackoffStrategy)
                .throwOnExhausted(false)
                .build();
    }
}
