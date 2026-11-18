package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.ResourcePhotoRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePhotoResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/resources/{resourceCode}/photos")
public class ResourcePhotoController {

    private final ResourcePhotoService service;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('RESOURCE:GALLERY','RESOURCE_GALLERY')")
    @com.sni.bokaticowork.core.audit.aop.Audited(module = "RESOURCE", action = "ADD_PHOTO", ressource = "resource")
    public ResponseEntity<ResourcePhotoResponse> add(@PathVariable String resourceCode,
                                                     @Valid @RequestBody ResourcePhotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(resourceCode, request));
    }

    @GetMapping
    public ResponseEntity<List<ResourcePhotoResponse>> list(@PathVariable String resourceCode) {
        return ResponseEntity.ok(service.list(resourceCode));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RESOURCE:GALLERY','RESOURCE_GALLERY')")
    @com.sni.bokaticowork.core.audit.aop.Audited(module = "RESOURCE", action = "UPDATE_PHOTO", ressource = "resource")
    public ResponseEntity<ResourcePhotoResponse> update(@PathVariable String resourceCode, @PathVariable Long id,
                                                        @Valid @RequestBody ResourcePhotoRequest request) {
        return ResponseEntity.ok(service.update(resourceCode, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RESOURCE:GALLERY','RESOURCE_GALLERY')")
    @com.sni.bokaticowork.core.audit.aop.Audited(module = "RESOURCE", action = "DELETE_PHOTO", ressource = "resource")
    public ResponseEntity<Void> delete(@PathVariable String resourceCode, @PathVariable Long id) {
        service.delete(resourceCode, id);
        return ResponseEntity.noContent().build();
    }
}
