package com.sni.bokaticowork.security.authentication.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LoginResponse {
    String accessToken;
    String refreshToken;
    /** L'adresse a-t-elle ete verifiee · sinon l'espace client refusera, et il faut le dire ici. */
    Boolean emailVerified;
    /** Les roles portes par le jeton · vide tant que le compte n'a pas ete active. */
    java.util.List<String> roles;
}
