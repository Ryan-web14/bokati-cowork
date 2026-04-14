package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceClosureRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceClosureResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceClosureService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/resource-closures")
@RequiredArgsConstructor
public class ResourceClosureController {

    private final ResourceClosureService resourceClosureService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_CLOSURE", ressource = "resource_closure")
    @Idempotent(operation = "RESOURCE_CLOSURE_CREATE")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateResourceClosureRequest request) {
        resourceClosureService.createClosure(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceClosureResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(resourceClosureService.getClosure(id));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ResourceClosureResponse>> list(
            @RequestParam(required = false) String resourceCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (resourceCode != null) {
            return ResponseEntity.ok(resourceClosureService.listByResource(resourceCode, pageable));
        }
        return ResponseEntity.ok(resourceClosureService.list(pageable));
    }

    @PatchMapping("/{id}/active")
    @Audited(module = "RESOURCE", action = "UPDATE_CLOSURE_ACTIVE", ressource = "resource_closure")
    @Idempotent(operation = "RESOURCE_CLOSURE_UPDATE_ACTIVE", requestBodyArgIndex = -1)
    public ResponseEntity<Void> updateActive(@PathVariable Long id, @RequestParam Boolean active) {
        resourceClosureService.updateActive(id, active);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Audited(module = "RESOURCE", action = "DELETE_CLOSURE", ressource = "resource_closure")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceClosureService.deleteClosure(id);
        return ResponseEntity.noContent().build();
    }
}
