package com.sni.bokaticowork.features.event.controller;

import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventRegistrationResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventSummaryResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.PublicEventRegistrationRequest;
import com.sni.bokaticowork.features.event.service.interfaces.EventRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * URL publique dédiée à la Grande Ouverture — plus simple à partager (site web, réseaux
 * sociaux, QR code) qu'un endpoint générique paramétré par {@code eventCode}. Délègue au
 * même {@link EventRegistrationService} générique, fixé sur l'événement configuré par
 * {@code app.event.opening-code} (pré-chargé en base sous le code "OUVERTURE").
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/public/opening")
public class PublicOpeningController {

    private final EventRegistrationService service;

    @Value("${app.event.opening-code:OUVERTURE}")
    private String openingEventCode;

    @GetMapping
    public ResponseEntity<EventSummaryResponse> getOpeningEvent() {
        return ResponseEntity.ok(service.getEvent(openingEventCode));
    }

    @PostMapping("/registrations")
    @Idempotent(operation = "OPENING_REGISTRATION_CREATE", required = false)
    public ResponseEntity<EventRegistrationResponse> register(
            @Valid @RequestBody PublicEventRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(openingEventCode, request));
    }
}
