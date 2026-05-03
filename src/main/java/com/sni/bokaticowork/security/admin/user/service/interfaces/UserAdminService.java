package com.sni.bokaticowork.security.admin.user.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminCreateUserRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminResetUserPasswordRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminUpdateUserRequest;
import com.sni.bokaticowork.security.admin.user.dto.response.AdminUserResponse;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {
    AdminUserResponse createUser(AdminCreateUserRequest request, String assignedBy);
    PaginatedResponse<AdminUserResponse> list(String email, Boolean enabled, Boolean locked, Boolean deleted, Pageable pageable);
    java.util.List<AdminUserResponse> searchByName(String query);
    AdminUserResponse getById(Long id);
    AdminUserResponse getByEmail(String email);
    AdminUserResponse update(Long id, AdminUpdateUserRequest request, String assignedBy);
    AdminUserResponse activate(Long id);
    AdminUserResponse deactivate(Long id);
    AdminUserResponse unlock(Long id);
    AdminUserResponse resetPassword(Long id, AdminResetUserPasswordRequest request);
    void archive(Long id);
}
