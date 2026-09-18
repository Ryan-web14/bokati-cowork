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

    // Toutes les formes que les gens tapent : +, 00, espaces, points, tirets, parentheses.
    // La mise en forme internationale se fait ensuite, avec le pays si l'indicatif manque.
    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^[+0-9()\\s.\\-]{6,30}$", message = "Invalid phone number format")
    private String phone;

    /**
     * Pays du numero, quand celui-ci ne porte pas son indicatif · code ISO (CG, FR) ou indicatif
     * (+242). Ignore si le numero commence par un plus. Absent, le pays de l'etablissement.
     */
    @Size(max = 10)
    private String phoneCountry;

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
