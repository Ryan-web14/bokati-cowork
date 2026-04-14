package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceGroupResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/resource-groups")
@RequiredArgsConstructor
public class ResourceGroupController {

    private final ResourceGroupService resourceGroupService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_GROUP", ressource = "resource_group")
    @Idempotent(operation = "RESOURCE_GROUP_CREATE")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateResourceGroupRequest request) {
        resourceGroupService.createResourceGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE_GROUP", ressource = "resource_group")
    @Idempotent(operation = "RESOURCE_GROUP_UPDATE")
    public ResponseEntity<Void> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateResourceGroupRequest request) {
        resourceGroupService.updateResourceGroup(code, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    @Audited(module = "RESOURCE", action = "DELETE_GROUP", ressource = "resource_group")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        resourceGroupService.deleteResourceGroup(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<ResourceGroupResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(resourceGroupService.getResourceGroup(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ResourceGroupResponse>> list() {
        return ResponseEntity.ok(resourceGroupService.list());
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ResourceGroupResponse>> search(
            @RequestParam("query") String query) {
        return ResponseEntity.ok(resourceGroupService.search(query));
    }

    @GetMapping("/search/basic")
    public ResponseEntity<PaginatedResponse<ResourceGroupResponse>> basicSearch(
            @RequestParam("query") String query) {
        return ResponseEntity.ok(resourceGroupService.search(query));
    }
}
