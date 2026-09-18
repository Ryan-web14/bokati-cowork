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

    /**
     * Pays du numero, quand celui-ci ne porte pas son indicatif · code ISO (CG, FR) ou indicatif
     * (+242). Ignore si le numero commence par un plus. Absent, le pays de l'etablissement.
     */
    private String phoneCountry;

    @Size(max = 30)
    private String whatsappPhone;

}
