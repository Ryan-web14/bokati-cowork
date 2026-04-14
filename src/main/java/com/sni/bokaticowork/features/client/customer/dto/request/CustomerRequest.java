package com.sni.bokaticowork.features.client.customer.dto.request;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CustomerRequest {

    @NotNull
    private String type;

    private String firstname;
    private String lastname;

    private String companyName;

    @Email
    private String email;
    @Email
    private String billingEmail;


    private AddressRequest address;
    @NotNull
    private String phone;
    private String whatsappPhone;


}
