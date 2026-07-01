package com.sni.bokaticowork.security.admin.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String userId;
    private String email;
    private String firstname;
    private String lastname;
    private Boolean accountEnabled;
    private Boolean accountLocked;
    private Boolean accountExpired;
    private Boolean deleted;
    private Integer failedLoginAttempts;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roleNames;
    private String generatedPassword;
}
