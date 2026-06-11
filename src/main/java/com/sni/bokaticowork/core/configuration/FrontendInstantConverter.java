package com.sni.bokaticowork.core.configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class FrontendInstantConverter implements Converter<String, Instant> {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Lagos");

    private final ZoneId zoneId;

    public FrontendInstantConverter() {
        this(DEFAULT_ZONE);
    }

    public FrontendInstantConverter(String zoneId) {
        this(zoneId == null ? DEFAULT_ZONE : ZoneId.of(zoneId));
    }

    public FrontendInstantConverter(ZoneId zoneId) {
        this.zoneId = zoneId == null ? DEFAULT_ZONE : zoneId;
    }

    @Override
    public Instant convert(@NonNull String source) {
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(trimmed);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(trimmed).atZone(zoneId).toInstant();
        } catch (Exception ignored) {
        }
        return LocalDate.parse(trimmed).atStartOfDay(zoneId).toInstant();
    }
}
