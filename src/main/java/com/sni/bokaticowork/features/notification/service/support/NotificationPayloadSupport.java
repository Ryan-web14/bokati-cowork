package com.sni.bokaticowork.features.notification.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationPayloadSupport {

    private final ObjectMapper objectMapper;

    public String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize notification payload", ex);
        }
    }

    public Map<String, Object> toMap(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read notification payload", ex);
        }
    }
}
