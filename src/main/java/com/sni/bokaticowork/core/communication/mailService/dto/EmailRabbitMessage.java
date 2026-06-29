package com.sni.bokaticowork.core.communication.mailService.dto;

import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;

import java.io.Serializable;
import java.time.Instant;

public record EmailRabbitMessage(
        String emailNumber,
        String to,
        String from,
        String subject,
        String htmlContent,
        EmailPriority priority,
        String eventType,
        String aggregateType,
        String aggregateId,
        String relatedType,
        String relatedCode,
        String attachmentName,
        byte[] attachmentBytes,
        String inlineImageContentId,
        byte[] inlineImageBytes,
        int attemptCount,
        Instant createdAt
) implements Serializable {

    public String routingKey() {
        return priority().routingKey() + "." + safeEventType();
    }

    private String safeEventType() {
        return eventType != null ? eventType.toLowerCase().replace("_", ".") : "generic";
    }
}