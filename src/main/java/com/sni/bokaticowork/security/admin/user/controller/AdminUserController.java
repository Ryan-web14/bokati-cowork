package com.sni.bokaticowork.security.admin.user.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminCreateUserRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminResetUserPasswordRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.AdminUpdateUserRequest;
import com.sni.bokaticowork.security.admin.user.dto.response.AdminUserResponse;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @Audited(module = "USER", action = "ADMIN_CREATE", ressource = "user")
    @Idempotent(operation = "ADMIN_USER_CREATE", required = false)
    @PostMapping
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminCreateUserRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userAdminService.createUser(request, actor(authentication)));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminUserResponse>> list(@RequestParam(required = false) String email,
                                                                     @RequestParam(required = false) Boolean enabled,
                                                                     @RequestParam(required = false) Boolean locked,
                                                                     @RequestParam(required = false) Boolean deleted,
                                                                     Pageable pageable) {
        return ResponseEntity.ok(userAdminService.list(email, enabled, locked, deleted, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.getById(id));
    }

    @GetMapping("/by-email")
    public ResponseEntity<AdminUserResponse> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userAdminService.getByEmail(email));
    }

    @Audited(module = "USER", action = "ADMIN_UPDATE", ressource = "user")
    @Idempotent(operation = "ADMIN_USER_UPDATE", required = false)
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody AdminUpdateUserRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(userAdminService.update(id, request, actor(authentication)));
    }

    @Audited(module = "USER", action = "ADMIN_ACTIVATE", ressource = "user")
    @Idempotent(operation = "ADMIN_USER_ACTIVATE", required = false)
    @PatchMapping("/{id}/activate")
    public ResponseEntity<AdminUserResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.activate(id));
    }

    @Audited(module = "USER", action = "ADMIN_DEACTIVATE", ressource = "user")
    @Idempotent(operation = "ADMIN_USER_DEACTIVATE", required = false)
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<AdminUserResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.deactivate(id));
    }

    @Audited(module = "USER", action = "ADMIN_RESET_PASSWORD", ressource = "user")
    @Idempotent(operation = "ADMIN_USER_RESET_PASSWORD", required = false)
    @PatchMapping("/{id}/password/reset")
    public ResponseEntity<AdminUserResponse> resetPassword(@PathVariable Long id,
                                                           @RequestBody(required = false) AdminResetUserPasswordRequest request) {
        return ResponseEntity.ok(userAdminService.resetPassword(id, request));
    }

    @Audited(module = "USER", action = "ADMIN_ARCHIVE", ressource = "user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        userAdminService.archive(id);
        return ResponseEntity.noContent().build();
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "SYSTEM"
                : authentication.getName();
    }
}
