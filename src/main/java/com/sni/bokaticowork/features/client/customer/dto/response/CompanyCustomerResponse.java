package com.sni.bokaticowork.features.client.customer.dto.response;


import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CompanyCustomerResponse {

    private String companyName;
    private String email;
    private String billingEmail;
    private String phone;
    private String whatsappPhone;
    private AddressResponse address;
    private String status;
    private String createdAt;
}
