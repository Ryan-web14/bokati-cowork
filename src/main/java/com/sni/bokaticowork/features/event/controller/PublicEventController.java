package com.sni.bokaticowork.features.event.controller;

import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventRegistrationResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventSummaryResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.PublicEventRegistrationRequest;
import com.sni.bokaticowork.features.event.service.interfaces.EventRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publics · aucune authentification requise. Réutilisables pour tout
 * événement nécessitant une confirmation de présence (identifié par son {@code eventCode}) :
 * il suffit qu'un {@code Event} actif existe avec ce code (ex: "OUVERTURE").
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/public/events")
public class PublicEventController {

    private final EventRegistrationService service;

    @GetMapping("/{eventCode}")
    public ResponseEntity<EventSummaryResponse> getEvent(@PathVariable String eventCode) {
        return ResponseEntity.ok(service.getEvent(eventCode));
    }

    @PostMapping("/{eventCode}/registrations")
    @Idempotent(operation = "EVENT_REGISTRATION_CREATE", required = false)
    public ResponseEntity<EventRegistrationResponse> register(
            @PathVariable String eventCode,
            @Valid @RequestBody PublicEventRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(eventCode, request));
    }
}
