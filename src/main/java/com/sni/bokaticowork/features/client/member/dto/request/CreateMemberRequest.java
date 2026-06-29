package com.sni.bokaticowork.features.client.member.dto.request;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateMemberRequest {

    @Size(max = 300)
    private String existingCustomerId;

    @Size(max = 30)
    private String customerType;

    @Size(max = 350)
    private String firstname;

    @Size(max = 350)
    private String lastname;

    @Email
    @NotBlank
    @Size(max = 250)
    private String email;

    @NotBlank
    @Size(max = 30)
    private String phone;

    @Size(max = 30)
    private String whatsappPhone;

    @Size(max = 45)
    private String password;

    private String companyName;

    @Email
    @Size(max = 250)
    private String billingEmail;

    @Valid
    private AddressRequest address;

    private LocalDate birthDate;

    @Size(max = 20)
    private String gender;

    @Size(max = 30)
    private String preferredCommunicationChannel;

    private boolean generatePassword;

}
