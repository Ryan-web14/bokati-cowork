package com.sni.bokaticowork.features.portal.document.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientContractSummaryResponse {
    private String contractCode;
    private String title;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant signedAt;
    private boolean hasPdf;
}
