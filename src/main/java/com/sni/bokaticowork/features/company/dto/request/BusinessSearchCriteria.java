package com.sni.bokaticowork.features.company.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BusinessSearchCriteria {

    private String code;
    private String name;
    private String legalForm;
    private String niuNumber;
    private String rccmNumber;
    private String taxId;
    private String activity;
    private String phone;
    private String email;
    private String baseCurrencyCode;
    private String status;
}
