package com.sni.bokaticowork.core.communication.mailService.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class GraphApiRateLimiter {

    @Value("${bokati.graph-api.rate-limit.limit-for-period:800}")
    private int limitForPeriod;

    @Value("${bokati.graph-api.rate-limit.limit-refresh-period-ms:60000}")
    private long refreshPeriodMs;

    @Value("${bokati.graph-api.rate-limit.timeout-ms:30000}")
    private long timeoutMs;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private static final String REFRESH_PERIOD_KEY = "bokati.graph-api.rate-limit.limit-refresh-period-ms";
    private static final String TIMEOUT_KEY = "bokati.graph-api.rate-limit.timeout-ms";

    private long refreshPeriodMs() {
        return platformSettings == null ? refreshPeriodMs : platformSettings.number(REFRESH_PERIOD_KEY, refreshPeriodMs);
    }

    private long timeoutMs() {
        return platformSettings == null ? timeoutMs : platformSettings.number(TIMEOUT_KEY, timeoutMs);
    }

    private Semaphore semaphore;
    private final AtomicLong lastRefresh = new AtomicLong(0);

    @PostConstruct
    public void init() {
        semaphore = new Semaphore(limitForPeriod);
        lastRefresh.set(System.currentTimeMillis());
        log.info("Microsoft Graph rate limiter initialized: {}/{}ms", limitForPeriod, refreshPeriodMs);
    }

    public void acquirePermission() {
        refreshIfNeeded();
        long timeoutMs = timeoutMs();
        try {
            if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(
                        "Microsoft Graph rate limit exceeded · could not acquire permit within " + timeoutMs + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate limit permit", e);
        }
    }

    private void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        long last = lastRefresh.get();
        if (now - last >= refreshPeriodMs() && lastRefresh.compareAndSet(last, now)) {
            int permitsToRelease = limitForPeriod - semaphore.availablePermits();
            if (permitsToRelease > 0) {
                semaphore.release(permitsToRelease);
            }
        }
    }
}
