package com.sni.bokaticowork.security.authentication.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    /**
     * Espace demande (facultatif) : {@code BACK_OFFICE} refuse un compte sans role d'equipe, avant
     * l'ouverture de la session. Sans valeur (portail client), rien ne change.
     */
    private String space;

    public LoginRequest(String email, String password) {
        this(email, password, null);
    }

}
