package com.sni.bokaticowork.security.admin.role.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.role.dto.request.PermissionRequest;
import com.sni.bokaticowork.security.admin.role.dto.response.PermissionResponse;
import com.sni.bokaticowork.security.admin.role.service.interfaces.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/permissions")
public class PermissionAdminController {

    private final PermissionService permissionService;

    @PostMapping
    @Audited(module = "PERMISSION", action = "CREATE", ressource = "permission")
    @Idempotent(operation = "PERMISSION_CREATE")
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.createPermission(request));
    }

    @PutMapping("/{name}")
    @Audited(module = "PERMISSION", action = "UPDATE", ressource = "permission")
    @Idempotent(operation = "PERMISSION_UPDATE")
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<PermissionResponse> update(@PathVariable String name, @Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.ok(permissionService.updatePermissionByName(name, request));
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<PermissionResponse> get(@PathVariable String name) {
        return ResponseEntity.ok(permissionService.getPermissionByName(name));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<PaginatedResponse<PermissionResponse>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "false") boolean systemOnly,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        if (systemOnly) {
            return ResponseEntity.ok(new PaginatedResponse<>(new org.springframework.data.domain.PageImpl<>(permissionService.getSystemPermissions())));
        }
        if (activeOnly) {
            return ResponseEntity.ok(new PaginatedResponse<>(new org.springframework.data.domain.PageImpl<>(permissionService.getAllActivePermissions())));
        }
        return ResponseEntity.ok(permissionService.getPaginatedPermissions(pageable));
    }

    @GetMapping("/search/by-name")
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<List<PermissionResponse>> searchByName(@RequestParam("query") String query) {
        return ResponseEntity.ok(permissionService.searchByName(query));
    }

    @PatchMapping("/{name}/activate")
    @Audited(module = "PERMISSION", action = "ACTIVATE", ressource = "permission")
    @Idempotent(operation = "PERMISSION_ACTIVATE", requestBodyArgIndex = -1)
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<Void> activate(@PathVariable String name) {
        permissionService.activatePermissionByName(name);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{name}/deactivate")
    @Audited(module = "PERMISSION", action = "DEACTIVATE", ressource = "permission")
    @Idempotent(operation = "PERMISSION_DEACTIVATE", requestBodyArgIndex = -1)
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<Void> deactivate(@PathVariable String name) {
        permissionService.deactivatePermissionByName(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}")
    @Audited(module = "PERMISSION", action = "DELETE", ressource = "permission")
    @PreAuthorize("hasAuthority('SYSTEM:PERMISSIONS')")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        permissionService.deletePermissionByName(name);
        return ResponseEntity.noContent().build();
    }
}
