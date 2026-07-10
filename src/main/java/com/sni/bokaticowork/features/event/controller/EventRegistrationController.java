package com.sni.bokaticowork.features.event.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventRegistrationResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.RejectEventRegistrationRequest;
import com.sni.bokaticowork.features.event.enums.RegistrationStatus;
import com.sni.bokaticowork.features.event.service.interfaces.EventRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Back-office · validation/rejet des précomptes créés via les inscriptions publiques
 * (voir {@link PublicEventController}). Réutilisable pour tout événement.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/event-registrations")
public class EventRegistrationController {

    private final EventRegistrationService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<PaginatedResponse<EventRegistrationResponse>> search(
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(eventCode, status, searchText, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<EventRegistrationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PatchMapping("/{id}/validate")
    @Audited(module = "EVENT_REGISTRATION", action = "VALIDATE", ressource = "event_registration")
    @PreAuthorize("hasAnyAuthority('CRM:CONVERT','CRM_CONVERT')")
    public ResponseEntity<EventRegistrationResponse> validate(@PathVariable Long id) {
        return ResponseEntity.ok(service.validate(id));
    }

    @PatchMapping("/{id}/reject")
    @Audited(module = "EVENT_REGISTRATION", action = "REJECT", ressource = "event_registration")
    @PreAuthorize("hasAnyAuthority('CRM:CONVERT','CRM_CONVERT')")
    public ResponseEntity<EventRegistrationResponse> reject(@PathVariable Long id,
                                                             @RequestBody(required = false) RejectEventRegistrationRequest request) {
        return ResponseEntity.ok(service.reject(id, request));
    }
}
