package com.sni.bokaticowork.security.admin.role.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.role.dto.request.AssignRoleToUserRequest;
import com.sni.bokaticowork.security.admin.role.dto.request.UpdateRolePermissionsRequest;
import com.sni.bokaticowork.security.admin.role.dto.response.PermissionResponse;
import com.sni.bokaticowork.security.admin.role.dto.response.RoleResponse;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RolePermissionService;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleService;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin")
public class RoleAssignmentAdminController {

    private final RoleUserService roleUserService;
    private final RolePermissionService rolePermissionService;
    private final RoleService roleService;

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<List<RoleResponse>> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(roleUserService.getRoleUsers(userId));
    }

    @PostMapping("/users/{userId}/roles")
    @Audited(module = "ROLE_USER", action = "ASSIGN_ROLE", ressource = "role_user")
    @Idempotent(operation = "ROLE_USER_ASSIGN")
    public ResponseEntity<Void> assignRole(@PathVariable Long userId,
                                           @Valid @RequestBody AssignRoleToUserRequest request,
                                           Authentication authentication) {
        roleUserService.addRoleToUser(userId, request.roleName(), actor(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @Audited(module = "ROLE_USER", action = "REMOVE_ROLE", ressource = "role_user")
    public ResponseEntity<Void> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        roleUserService.deleteRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(rolePermissionService.getRolePermissions(roleId));
    }

    @PatchMapping("/roles/{roleId}/permissions")
    @Audited(module = "ROLE_PERMISSION", action = "REPLACE_PERMISSIONS", ressource = "role_permission")
    @Idempotent(operation = "ROLE_PERMISSION_REPLACE")
    public ResponseEntity<Void> replaceRolePermissions(@PathVariable Long roleId,
                                                       @Valid @RequestBody UpdateRolePermissionsRequest request) {
        rolePermissionService.deleteAllPermissionFromRole(roleId);
        rolePermissionService.addPermissionToRole(request.permissionIds(), roleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/roles/{roleId}/permissions")
    @Audited(module = "ROLE_PERMISSION", action = "CLEAR_PERMISSIONS", ressource = "role_permission")
    public ResponseEntity<Void> clearRolePermissions(@PathVariable Long roleId) {
        rolePermissionService.deleteAllPermissionFromRole(roleId);
        return ResponseEntity.noContent().build();
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "SYSTEM"
                : authentication.getName();
    }
}
