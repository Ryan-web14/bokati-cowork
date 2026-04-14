package com.sni.bokaticowork.features.client.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMemberRequest {

    @Size(max = 350)
    private String firstname;

    @Size(max = 350)
    private String lastname;

    @Email
    @Size(max = 250)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 30)
    private String whatsappPhone;

}
