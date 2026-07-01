package com.sni.bokaticowork.features.contract.service.interfaces;

import com.sni.bokaticowork.features.contract.dto.response.ContractAuditEventResponse;

import java.util.List;

public interface ContractAuditEventService {
    void record(String contractCode, String amendmentCode, String eventType,
                Long actorId, String actorType, String actorName,
                String justification, String payload);
    List<ContractAuditEventResponse> getAuditTrail(String contractCode);
}
