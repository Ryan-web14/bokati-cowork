package com.sni.bokaticowork.security.admin.role.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.role.dto.request.RoleRequest;
import com.sni.bokaticowork.security.admin.role.dto.response.RoleResponse;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/roles")
public class RoleAdminController {

    private final RoleService roleService;

    @PostMapping
    @Audited(module = "ROLE", action = "CREATE", ressource = "role")
    @Idempotent(operation = "ROLE_CREATE")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.addRole(request));
    }

    @PutMapping("/{name}")
    @Audited(module = "ROLE", action = "UPDATE", ressource = "role")
    @Idempotent(operation = "ROLE_UPDATE")
    public ResponseEntity<RoleResponse> update(@PathVariable String name, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(name, request));
    }

    @GetMapping("/{name}")
    public ResponseEntity<RoleResponse> get(@PathVariable String name) {
        return ResponseEntity.ok(roleService.getRole(name));
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleService.getAllRolesAdmin());
    }

    @PatchMapping("/{name}/activate")
    @Audited(module = "ROLE", action = "ACTIVATE", ressource = "role")
    @Idempotent(operation = "ROLE_ACTIVATE", requestBodyArgIndex = -1)
    public ResponseEntity<Void> activate(@PathVariable String name) {
        roleService.activateRole(name);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{name}/deactivate")
    @Audited(module = "ROLE", action = "DEACTIVATE", ressource = "role")
    @Idempotent(operation = "ROLE_DEACTIVATE", requestBodyArgIndex = -1)
    public ResponseEntity<Void> deactivate(@PathVariable String name) {
        roleService.deactivateRole(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}")
    @Audited(module = "ROLE", action = "DELETE", ressource = "role")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        roleService.deleteRole(name);
        return ResponseEntity.noContent().build();
    }
}
