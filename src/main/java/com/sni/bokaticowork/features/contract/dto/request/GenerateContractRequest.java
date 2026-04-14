package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class GenerateContractRequest {

    @NotBlank
    private String templateCode;

    @NotNull
    private DocumentOwnerType ownerType;

    @NotBlank
    private String ownerCode;

    private String businessCode;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long uploadedBy;

    private LocalDate effectiveDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private String signatoryName;

    private String signatoryRole;

    private List<String> clauses;

    private Map<String, String> variables;
}
