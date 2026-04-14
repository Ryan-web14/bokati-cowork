package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePolicyResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/resource-policies")
@RequiredArgsConstructor
public class ResourcePolicyController {

    private final ResourcePolicyService resourcePolicyService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_POLICY", ressource = "resource_policy")
    @Idempotent(operation = "RESOURCE_POLICY_CREATE")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateResourcePolicyRequest request) {
        resourcePolicyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE_POLICY", ressource = "resource_policy")
    @Idempotent(operation = "RESOURCE_POLICY_UPDATE")
    public ResponseEntity<Void> update(@PathVariable String code, @Valid @RequestBody UpdateResourcePolicyRequest request) {
        resourcePolicyService.updatePolicy(code, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/status")
    @Audited(module = "RESOURCE", action = "UPDATE_POLICY_STATUS", ressource = "resource_policy")
    @Idempotent(operation = "RESOURCE_POLICY_UPDATE_STATUS", requestBodyArgIndex = -1)
    public ResponseEntity<Void> updateStatus(@PathVariable String code, @RequestParam Boolean active) {
        resourcePolicyService.updatePolicyStatus(code, active);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/allow-cancellation")
    @Audited(module = "RESOURCE", action = "UPDATE_POLICY_CANCELLATION", ressource = "resource_policy")
    @Idempotent(operation = "RESOURCE_POLICY_UPDATE_CANCELLATION", requestBodyArgIndex = -1)
    public ResponseEntity<Void> updateCancellationPolicy(@PathVariable String code, @RequestParam Boolean allowed) {
        resourcePolicyService.updateCancellationPolicy(code, allowed);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    @Audited(module = "RESOURCE", action = "DELETE_POLICY", ressource = "resource_policy")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        resourcePolicyService.deletePolicy(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<ResourcePolicyResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(resourcePolicyService.getPolicy(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ResourcePolicyResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(resourcePolicyService.list(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ResourcePolicyResponse>> search(@RequestParam("query") String query) {
        return ResponseEntity.ok(resourcePolicyService.search(query));
    }

    @GetMapping("/search/basic")
    public ResponseEntity<PaginatedResponse<ResourcePolicyResponse>> basicSearch(@RequestParam("query") String query) {
        return ResponseEntity.ok(resourcePolicyService.search(query));
    }
}
