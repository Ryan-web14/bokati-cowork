package com.sni.bokaticowork.features.company.dto.response;

import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BusinessEntityResponse {

    private String code;
    private String name;
    private String status;
    private String legalForm;
    private String niuNumber;
    private String rccmNumber;
    private String taxId;
    private String activity;
    private String baseCurrencyCode;
    private AddressResponse address;
    private String phone;
    private String email;
}
