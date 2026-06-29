package com.sni.bokaticowork.security.authentication.dto.request;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMemberRequest {

    @NotBlank
    @Size(max = 350)
    private String firstname;

    @NotBlank
    @Size(max = 350)
    private String lastname;

    @Email
    @NotBlank
    @Size(max = 250)
    private String email;

    @NotBlank
    @Size(min = 8, max = 45, message = "Password must be between 8 and 45 characters")
    private String password;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Invalid phone number format")
    private String phone;

    @Size(max = 30)
    private String whatsappPhone;

    @Valid
    private AddressRequest address;

    private LocalDate birthDate;

    @Size(max = 20)
    private String gender;

    @Size(max = 30)
    private String preferredCommunicationChannel;
}
