package com.sni.bokaticowork.features.contract.dto.response;

import com.sni.bokaticowork.features.contract.enums.ContractSectionType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ContractDraftSectionResponse {
    private Long id;
    private String title;
    private String content;
    private ContractSectionType sectionType;
    private Integer sectionOrder;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
