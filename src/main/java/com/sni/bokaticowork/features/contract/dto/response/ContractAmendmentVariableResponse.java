package com.sni.bokaticowork.features.contract.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ContractAmendmentVariableResponse {
    private Long id;
    private String variableKey;
    private String previousValue;
    private String newValue;
    private Instant createdAt;
    private Instant updatedAt;
}
