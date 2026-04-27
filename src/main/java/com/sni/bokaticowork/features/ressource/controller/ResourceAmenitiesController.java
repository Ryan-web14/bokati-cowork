package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.CreateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAmenitiesService;
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
@RequestMapping(ApiPath.V1 + "/resource-amenities")
@RequiredArgsConstructor
public class ResourceAmenitiesController {

    private final ResourceAmenitiesService resourceAmenitiesService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_AMENITY", ressource = "resource_amenity")
    @Idempotent(operation = "RESOURCE_AMENITY_CREATE")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateAmenityRequest request) {
        resourceAmenitiesService.createAmenity(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE_AMENITY", ressource = "resource_amenity")
    @Idempotent(operation = "RESOURCE_AMENITY_UPDATE")
    public ResponseEntity<Void> update(@PathVariable String code, @Valid @RequestBody UpdateAmenityRequest request) {
        resourceAmenitiesService.updateAmenity(code, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    @Audited(module = "RESOURCE", action = "DELETE_AMENITY", ressource = "resource_amenity")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        resourceAmenitiesService.deleteAmenity(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<AmenityResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(resourceAmenitiesService.getAmenity(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AmenityResponse>> list(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(resourceAmenitiesService.list(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<AmenityResponse>> search(@RequestParam("query") String query) {
        return ResponseEntity.ok(resourceAmenitiesService.search(query));
    }

    @GetMapping("/search/basic")
    public ResponseEntity<PaginatedResponse<AmenityResponse>> basicSearch(@RequestParam("query") String query) {
        return ResponseEntity.ok(resourceAmenitiesService.search(query));
    }
}
