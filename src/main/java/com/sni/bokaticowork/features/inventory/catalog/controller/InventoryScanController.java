package com.sni.bokaticowork.features.inventory.catalog.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryScanResponse;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entree unique des terminaux de scan.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/scan")
public class InventoryScanController {

    private final InventoryScanService service;

    /**
     * Identifie ce qui a ete scanne et renvoie les actions possibles.
     *
     * <p>Un code inconnu n'est pas une erreur : la reponse le signale et propose de le rattacher a
     * un article existant ou d'en creer un.</p>
     */
    @GetMapping("/{code}")
    public ResponseEntity<InventoryScanResponse> resolve(@PathVariable String code) {
        return ResponseEntity.ok(service.resolve(code));
    }
}
