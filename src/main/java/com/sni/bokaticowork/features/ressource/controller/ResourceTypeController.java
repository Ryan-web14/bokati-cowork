package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceTypeResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping(ApiPath.V1 + "/resource-types")
@RequiredArgsConstructor
public class ResourceTypeController {

    private final ResourceTypeService resourceTypeService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_TYPE", ressource = "resource_type")
    @Idempotent(operation = "RESOURCE_TYPE_CREATE")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateResourceTypeRequest request) {
        resourceTypeService.createType(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE_TYPE", ressource = "resource_type")
    @Idempotent(operation = "RESOURCE_TYPE_UPDATE")
    public ResponseEntity<Void> update(@PathVariable String code, @Valid @RequestBody UpdateResourceTypeRequest request) {
        resourceTypeService.updateType(code, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    @Audited(module = "RESOURCE", action = "DELETE_TYPE", ressource = "resource_type")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        resourceTypeService.deleteType(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<ResourceTypeResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(resourceTypeService.getType(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ResourceTypeResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(resourceTypeService.list(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ResourceTypeResponse>> search(@RequestParam("query") String query) {
        return ResponseEntity.ok(resourceTypeService.search(query));
    }

    @GetMapping("/search/basic")
    public ResponseEntity<PaginatedResponse<ResourceTypeResponse>> basicSearch(@RequestParam("query") String query) {
        return ResponseEntity.ok(resourceTypeService.search(query));
    }
}
