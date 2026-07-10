package com.sni.bokaticowork.features.crm.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.CreateLeadRequest;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.LeadResponse;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint public pour la création de leads depuis le formulaire de contact
 * du site web · aucune authentification requise.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/public/crm")
public class PublicCrmController {

    private final CrmService service;

    @PostMapping("/leads")
    public ResponseEntity<Map<String, String>> submitContactForm(
            @Valid @RequestBody CreateLeadRequest request) {
        LeadResponse lead = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "leadNumber", lead.leadNumber(),
                        "message", "Votre demande a bien été reçue. Notre équipe vous contactera sous 24h."
                ));
    }
}
