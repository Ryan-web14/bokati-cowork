package com.sni.bokaticowork.features.portal.profile.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientCustomerResponse {
    private String customerId;
    private String type;
    private String displayName;
    private String email;
    private String billingEmail;
    private String phone;
    private String companyName;
    private AddressResponse address;
}
