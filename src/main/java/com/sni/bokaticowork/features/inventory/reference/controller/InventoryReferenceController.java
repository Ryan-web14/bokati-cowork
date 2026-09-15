package com.sni.bokaticowork.features.inventory.reference.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.reference.dto.InventoryOptionResponse;
import com.sni.bokaticowork.features.inventory.reference.service.interfaces.InventoryReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Listes de valeurs du module inventaire, servies au front pour alimenter les champs deroulants.
 *
 * <p>Regle de conception : tout champ dont le domaine de valeurs est connu du backend doit etre une
 * selection, jamais une saisie libre. Ces endpoints sont la source unique de ces listes.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/reference")
public class InventoryReferenceController {

    private final InventoryReferenceService service;

    /**
     * Toutes les enumerations du module. Contenu fige entre deux livraisons, donc cachable.
     */
    @GetMapping("/enums")
    public ResponseEntity<Map<String, List<InventoryOptionResponse>>> enums() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(service.enums());
    }

    /**
     * Une seule enumeration, pour un ecran qui n'a besoin que d'une liste.
     */
    @GetMapping("/enums/{group}")
    public ResponseEntity<List<InventoryOptionResponse>> enumGroup(@PathVariable String group) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(service.enumGroup(group));
    }

    /**
     * Noms des groupes disponibles.
     */
    @GetMapping("/enums/groups")
    public ResponseEntity<List<String>> groups() {
        return ResponseEntity.ok(service.groupNames());
    }

    /**
     * Referentiels vivants : unites, categories, emplacements et fournisseurs actifs.
     * Non cachable, ces listes evoluent en exploitation.
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, List<InventoryOptionResponse>>> data() {
        return ResponseEntity.ok(service.data());
    }
}
