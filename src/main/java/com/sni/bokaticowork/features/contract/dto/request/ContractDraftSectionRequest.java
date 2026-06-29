package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.contract.enums.ContractSectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContractDraftSectionRequest {

    private String title;

    @NotBlank
    private String content;

    @NotNull
    private ContractSectionType sectionType;

    private Integer sectionOrder;

    private Boolean active;
}
