package com.sni.bokaticowork.features.contract.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ContractAuditEventResponse {
    private Long id;
    private String contractCode;
    private String amendmentCode;
    private String eventType;
    private Long actorId;
    private String actorType;
    private String actorName;
    private Instant occurredAt;
    private String previousHash;
    private String eventHash;
    private String payload;
    private String justification;
}
