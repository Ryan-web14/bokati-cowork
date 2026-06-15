package com.sni.bokaticowork.features.portal.profile.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClientProfileRequest {

    @Size(max = 350)
    private String firstname;

    @Size(max = 350)
    private String lastname;

    @Size(max = 30)
    private String phone;

    @Size(max = 30)
    private String whatsappPhone;
}
