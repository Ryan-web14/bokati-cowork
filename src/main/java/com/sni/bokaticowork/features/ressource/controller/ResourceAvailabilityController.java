package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.BulkCreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReleaseResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityGroupResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/resource-availabilities")
@RequiredArgsConstructor
public class ResourceAvailabilityController {

    private final ResourceAvailabilityService resourceAvailabilityService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_AVAILABILITY", ressource = "resource_availability")
    @Idempotent(operation = "RESOURCE_AVAILABILITY_CREATE")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateResourceAvailabilityRequest request) {
        resourceAvailabilityService.createAvailability(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Ouvre la meme plage sur plusieurs ressources.
     *
     * <p>Rend un compte rendu par ressource plutot qu'un tout-ou-rien : sur vingt ressources il
     * est normal que deux echouent · chevauchement, plafond d'un mois d'avance · et refuser
     * l'ensemble obligerait a les retirer une par une puis a relancer.
     */
    @PostMapping("/bulk")
    public ResponseEntity<BulkResourceOperationResponse> createBulk(
            @Valid @RequestBody BulkCreateResourceAvailabilityRequest request) {
        return ResponseEntity.ok(resourceAvailabilityService.createAvailabilityBulk(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ResourceAvailabilityResponse>> list(
            @RequestParam(required = false) String resourceCode,
            @PageableDefault(size = 50, sort = "startedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        if (resourceCode != null) {
            return ResponseEntity.ok(resourceAvailabilityService.listByResource(resourceCode, pageable));
        }
        return ResponseEntity.ok(resourceAvailabilityService.list(pageable));
    }

    @GetMapping("/grouped")
    public ResponseEntity<List<ResourceAvailabilityGroupResponse>> listGroupedByResource() {
        return ResponseEntity.ok(resourceAvailabilityService.listGroupedByResource());
    }

    @GetMapping("/remaining-slots")
    public ResponseEntity<List<ResourceAvailabilityWindowResponse>> remaining(
            @RequestParam String resourceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endedAt,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(defaultValue = "1") Integer quantity) {
        return ResponseEntity.ok(
                resourceAvailabilityService.findRemainingWindows(resourceCode, startedAt, endedAt, durationMinutes, quantity)
        );
    }

    @PostMapping("/reserve")
    @Audited(module = "RESOURCE", action = "RESERVE_AVAILABILITY", ressource = "resource_availability")
    @Idempotent(operation = "RESOURCE_AVAILABILITY_RESERVE")
    public ResponseEntity<Void> reserve(@Valid @RequestBody ReserveResourceAvailabilityRequest request) {
        resourceAvailabilityService.reserve(request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/release")
    @Audited(module = "RESOURCE", action = "RELEASE_AVAILABILITY", ressource = "resource_availability")
    @Idempotent(operation = "RESOURCE_AVAILABILITY_RELEASE")
    public ResponseEntity<Void> release(@Valid @RequestBody ReleaseResourceAvailabilityRequest request) {
        resourceAvailabilityService.release(request);
        return ResponseEntity.noContent().build();
    }
}
