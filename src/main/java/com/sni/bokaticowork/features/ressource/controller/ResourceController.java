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
    public ResponseEntity<com.sni.bokaticowork.features.ressource.dto.response.ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createResource(request));
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
            @RequestParam(required = false) String siteCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // Lot R5b : filtre par site (resume comme detail), combinable avec type, groupe et politique.
        if (siteCode != null) {
            var criteria = com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria.builder()
                    .typeCode(typeCode).groupCode(groupCode).policyCode(policyCode).siteCode(siteCode).build();
            return summary ? ResponseEntity.ok(resourceService.search(criteria, pageable))
                    : ResponseEntity.ok(resourceService.listDetailed(criteria, pageable));
        }
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

        // Lot R4 : les filtres valent aussi pour la liste detaillee (avant : ignores hors resume).
        if (typeCode != null || groupCode != null || policyCode != null) {
            return ResponseEntity.ok(resourceService.listDetailed(
                    com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria.builder()
                            .typeCode(typeCode).groupCode(groupCode).policyCode(policyCode).build(), pageable));
        }
        return ResponseEntity.ok(resourceService.list(pageable));
    }

    // ------------------------------------------------------------------ lot R5b

    private com.sni.bokaticowork.features.ressource.service.support.ResourceFinder finder;
    private com.sni.bokaticowork.features.ressource.service.support.ResourceSites sites;
    private com.sni.bokaticowork.features.ressource.service.support.ResourceIcal ical;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setR5b(com.sni.bokaticowork.features.ressource.service.support.ResourceFinder finder,
                com.sni.bokaticowork.features.ressource.service.support.ResourceSites sites,
                com.sni.bokaticowork.features.ressource.service.support.ResourceIcal ical) {
        this.finder = finder;
        this.sites = sites;
        this.ical = ical;
    }

    /** Lot R5c : participants compris, prix d'un participant supplementaire, caution. */
    public record SalesSettings(Integer includedParticipants, Integer extraParticipantPrice, Integer depositAmount) {
    }

    /** Lot R5c : reglages de vente de la ressource ; un champ vide efface la valeur. */
    @PutMapping("/{code}/sales-settings")
    @Audited(module = "RESOURCE", action = "UPDATE_SALES_SETTINGS", ressource = "resource")
    public ResponseEntity<ResourceResponse> salesSettings(@PathVariable String code, @RequestBody SalesSettings request) {
        resourceService.updateSalesSettings(code, request.includedParticipants(), request.extraParticipantPrice(), request.depositAmount());
        return ResponseEntity.ok(resourceService.getResource(code));
    }

    /** Lot R5d : reglement d'usage et points de l'etat des lieux de fin. */
    public record UsageSettings(String usageRules, java.util.List<String> endChecklist) {
    }

    /** Lot R5d : un reglement modifie doit etre re-accepte (sa version change). */
    @PutMapping("/{code}/usage-settings")
    @Audited(module = "RESOURCE", action = "UPDATE_USAGE_SETTINGS", ressource = "resource")
    public ResponseEntity<ResourceResponse> usageSettings(@PathVariable String code, @RequestBody UsageSettings request) {
        resourceService.updateUsageSettings(code, request.usageRules(), request.endChecklist());
        return ResponseEntity.ok(resourceService.getResource(code));
    }

    /** Lot R5b : « Trouver une salle » par besoin, avec alternatives. */
    @PostMapping("/find")
    public ResponseEntity<com.sni.bokaticowork.features.ressource.dto.response.ResourceLocationResponses.FindResult> find(
            @RequestBody com.sni.bokaticowork.features.ressource.dto.request.ResourceLocationRequests.Find request) {
        return ResponseEntity.ok(finder.find(request));
    }

    /** Lot R5b : site, etage et position sur le plan. */
    @PutMapping("/{code}/placement")
    @Audited(module = "RESOURCE", action = "PLACE_RESOURCE", ressource = "resource")
    public ResponseEntity<ResourceResponse> place(@PathVariable String code,
            @RequestBody com.sni.bokaticowork.features.ressource.dto.request.ResourceLocationRequests.Placement request) {
        sites.place(code, request);
        return ResponseEntity.ok(resourceService.getResource(code));
    }

    /** Lot R5b : calendrier iCalendar de la ressource pour l'equipe (reservations et fermetures). */
    @GetMapping("/{code}/calendar.ics")
    public ResponseEntity<String> calendar(@PathVariable String code) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + code + ".ics\"")
                .body(ical.export(code));
    }

    /**
     * Lot R5b : nouveau lien d'abonnement (l'ancien cesse) ; utilisable seulement si le flux public est
     * active ({@code app.resources.ical.public-feed-enabled}).
     */
    @PostMapping("/{code}/ical-token")
    @Audited(module = "RESOURCE", action = "ROTATE_ICAL_TOKEN", ressource = "resource")
    public ResponseEntity<java.util.Map<String, Object>> icalToken(@PathVariable String code) {
        String token = ical.rotateToken(code);
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("token", token);
        body.put("path", com.sni.bokaticowork.core.utils.path.ApiPath.V1 + "/public/resource-calendars/" + token + ".ics");
        body.put("enabled", ical.publicFeedEnabled());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/search/basic")
    public ResponseEntity<PaginatedResponse<ResourceSummaryResponse>> basicSearch(
            @RequestParam("query") String query,
            @PageableDefault(size = 20) Pageable pageable) {
        // Lot R4 : paginee, comme les autres recherches du module.
        return ResponseEntity.ok(resourceService.basicSearch(query, pageable));
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
            @RequestParam ResourceStatus status,
            @RequestParam(defaultValue = "false") boolean cancelBookings) {
        resourceService.updateStatus(code, status, cancelBookings);
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
    public ResponseEntity<Void> delete(@PathVariable String code,
                                       @RequestParam(defaultValue = "false") boolean cancelBookings) {
        resourceService.deleteResource(code, cancelBookings);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lot R2 : reservations a venir touchees (toutes, ou sur une plage), a montrer avant une
     * suppression, une fermeture ou une maintenance.
     */
    @GetMapping("/{code}/impacted-bookings")
    public ResponseEntity<java.util.List<com.sni.bokaticowork.features.ressource.service.support.ResourceBookingImpact.ImpactedBooking>> impactedBookings(
            @PathVariable String code,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to) {
        return ResponseEntity.ok(resourceService.impactedBookings(code, from, to));
    }
}
