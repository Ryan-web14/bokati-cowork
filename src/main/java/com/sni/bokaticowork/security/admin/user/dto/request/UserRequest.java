package com.sni.bokaticowork.security.admin.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserRequest {

    @Email
    @NotBlank
    private String email;

    private String firstname;

    private String lastname;

    @NotBlank
    private String password;

    private boolean generatePassword;

    /**
     * Secret d'amorcage · n'a de sens que pour la creation du premier administrateur.
     *
     * <p>Il est confronte a {@code app.security.bootstrap.secret}. Ignore partout ailleurs · la
     * creation d'un agent ou d'un membre passe par un compte authentifie, pas par un secret.</p>
     */
    private String bootstrapSecret;
}
