package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.ChangeTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.LinkAmenityToResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateActiveRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateBookingEnabledRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateDisplayOrderRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdatePortalVisibleRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceSummaryResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
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

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE", ressource = "resource")
    @Idempotent(operation = "RESOURCE_CREATE", required = false)
    public ResponseEntity<Void> create(@Valid @RequestBody ResourceRequest request) {
        resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<ResourceResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(resourceService.getResource(code));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "false") boolean summary,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String policyCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (summary) {
            if (typeCode != null) {
                return ResponseEntity.ok(resourceService.listSummaryByType(typeCode, pageable));
            }
            if (groupCode != null) {
                return ResponseEntity.ok(resourceService.listSummaryByGroup(groupCode, pageable));
            }
            if (policyCode != null) {
                return ResponseEntity.ok(resourceService.listSummaryByPolicy(policyCode, pageable));
            }
            return ResponseEntity.ok(resourceService.listSummary(pageable));
        }

        return ResponseEntity.ok(resourceService.list(pageable));
    }

    @GetMapping("/search/basic")
    public ResponseEntity<List<ResourceSummaryResponse>> basicSearch(@RequestParam("query") String query) {
        return ResponseEntity.ok(resourceService.basicSearch(query));
    }

    @PostMapping("/search")
    public ResponseEntity<PaginatedResponse<ResourceSummaryResponse>> search(
            @RequestBody(required = false) ResourceSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(resourceService.search(criteria, pageable));
    }

    @PutMapping("/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE", ressource = "resource")
    @Idempotent(operation = "RESOURCE_UPDATE")
    public ResponseEntity<Void> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateResourceRequest request) {
        resourceService.updateResource(code, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/classification")
    @Audited(module = "RESOURCE", action = "CHANGE_CLASSIFICATION", ressource = "resource")
    @Idempotent(operation = "RESOURCE_CHANGE_CLASSIFICATION")
    public ResponseEntity<Void> changeClassification(
            @PathVariable String code,
            @Valid @RequestBody ChangeTypeRequest request) {
        resourceService.changeClassification(code, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/status")
    @Audited(module = "RESOURCE", action = "UPDATE_STATUS", ressource = "resource")
    @Idempotent(operation = "RESOURCE_UPDATE_STATUS", requestBodyArgIndex = -1)
    public ResponseEntity<Void> updateStatus(
            @PathVariable String code,
            @RequestParam ResourceStatus status) {
        resourceService.updateStatus(code, status);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/booking-enabled")
    @Audited(module = "RESOURCE", action = "UPDATE_BOOKING_ENABLED", ressource = "resource")
    @Idempotent(operation = "RESOURCE_UPDATE_BOOKING_ENABLED", requestBodyArgIndex = 1)
    public ResponseEntity<Void> updateBookingEnabled(
            @PathVariable String code,
            @Valid @RequestBody UpdateBookingEnabledRequest request) {
        resourceService.updateBookingEnabled(code, request.getBookingEnabled());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/portal-visible")
    @Audited(module = "RESOURCE", action = "UPDATE_PORTAL_VISIBLE", ressource = "resource")
    @Idempotent(operation = "RESOURCE_UPDATE_PORTAL_VISIBLE", requestBodyArgIndex = 1)
    public ResponseEntity<Void> updatePortalVisible(
            @PathVariable String code,
            @Valid @RequestBody UpdatePortalVisibleRequest request) {
        resourceService.updatePortalVisible(code, request.getVisible());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/active")
    @Audited(module = "RESOURCE", action = "UPDATE_ACTIVE", ressource = "resource")
    @Idempotent(operation = "RESOURCE_UPDATE_ACTIVE", requestBodyArgIndex = 1)
    public ResponseEntity<Void> updateActive(
            @PathVariable String code,
            @Valid @RequestBody UpdateActiveRequest request) {
        resourceService.updateActive(code, request.getActive());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{code}/display-order")
    @Audited(module = "RESOURCE", action = "UPDATE_DISPLAY_ORDER", ressource = "resource")
    @Idempotent(operation = "RESOURCE_UPDATE_DISPLAY_ORDER", requestBodyArgIndex = 1)
    public ResponseEntity<Void> updateDisplayOrder(
            @PathVariable String code,
            @Valid @RequestBody UpdateDisplayOrderRequest request) {
        resourceService.updateDisplayOrder(code, request.getDisplayOrder());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/amenities/link")
    @Audited(module = "RESOURCE", action = "LINK_AMENITY", ressource = "resource_amenity_link")
    @Idempotent(operation = "RESOURCE_LINK_AMENITY")
    public ResponseEntity<Void> linkAmenity(@Valid @RequestBody LinkAmenityToResourceRequest request) {
        resourceService.linkAmenity(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{code}/amenities/{amenityCode}")
    @Audited(module = "RESOURCE", action = "UNLINK_AMENITY", ressource = "resource_amenity_link")
    public ResponseEntity<Void> unlinkAmenity(
            @PathVariable String code,
            @PathVariable String amenityCode) {
        resourceService.unlinkAmenity(code, amenityCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}/amenities")
    public ResponseEntity<List<AmenityResponse>> listAmenities(@PathVariable String code) {
        return ResponseEntity.ok(resourceService.listAmenities(code));
    }

    @DeleteMapping("/{code}")
    @Audited(module = "RESOURCE", action = "DELETE", ressource = "resource")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        resourceService.deleteResource(code);
        return ResponseEntity.noContent().build();
    }
}
