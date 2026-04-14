package com.sni.bokaticowork.security.admin.role.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleToUserRequest(
        @NotBlank String roleName
) {
}
