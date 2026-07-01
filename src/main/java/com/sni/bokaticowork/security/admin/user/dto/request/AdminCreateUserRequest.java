package com.sni.bokaticowork.security.admin.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AdminCreateUserRequest {
    @Email
    @NotBlank
    private String email;
    private String firstname;
    private String lastname;
    private String password;
    private Boolean generatePassword;
    private List<String> roleNames;
}
