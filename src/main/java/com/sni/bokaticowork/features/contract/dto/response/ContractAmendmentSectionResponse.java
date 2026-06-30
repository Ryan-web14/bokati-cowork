package com.sni.bokaticowork.features.contract.dto.response;

import com.sni.bokaticowork.features.contract.enums.ContractAmendmentSectionAction;
import com.sni.bokaticowork.features.contract.enums.ContractSectionType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ContractAmendmentSectionResponse {
    private Long id;
    private ContractAmendmentSectionAction action;
    private String targetSectionRef;
    private ContractSectionType sectionType;
    private String title;
    private String content;
    private Integer sectionOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
