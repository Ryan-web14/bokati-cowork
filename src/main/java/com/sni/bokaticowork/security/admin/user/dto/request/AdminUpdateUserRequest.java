package com.sni.bokaticowork.security.admin.user.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

@Data
public class AdminUpdateUserRequest {
    @Email
    private String email;
    private Boolean isAccountEnabled;
    private Boolean isAccountLocked;
    private List<String> roleNames;
}
