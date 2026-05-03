package com.sni.bokaticowork.core.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class FrontendInstantDeserializer extends JsonDeserializer<Instant> {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Lagos");

    private final ZoneId zoneId;

    public FrontendInstantDeserializer() {
        this(DEFAULT_ZONE);
    }

    public FrontendInstantDeserializer(String zoneId) {
        this(ZoneId.of(zoneId));
    }

    public FrontendInstantDeserializer(ZoneId zoneId) {
        this.zoneId = zoneId == null ? DEFAULT_ZONE : zoneId;
    }

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (Exception ignored) {
        }
        return LocalDateTime.parse(trimmed).atZone(zoneId).toInstant();
    }
}
