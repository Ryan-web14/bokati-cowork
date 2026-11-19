package com.sni.bokaticowork.core.reference.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.EnumGroup;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ListPatch;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ListSummary;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.Value;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ValuePatch;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ValueRequest;
import com.sni.bokaticowork.core.reference.service.ReferenceEnumRegistry;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/**
 * Socle des listes de choix (lot L0) : valeurs fixes et listes de l'administrateur.
 *
 * <p>Lecture ouverte a toute l'equipe (un formulaire en a besoin) ; modification sous
 * {@code SYSTEM:REFERENCE_LISTS}.</p>
 */
@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceEnumRegistry enums;
    private final ReferenceLists lists;

    /** Valeurs fixes, par module et groupe (tout sans filtre). */
    @GetMapping("/reference/enums")
    public ResponseEntity<List<EnumGroup>> enums(@RequestParam(required = false) String module, @RequestParam(required = false) String group) {
        return ResponseEntity.ok(enums.groups(module, group));
    }

    /** Listes de l'administrateur, vides comprises, avec leurs compteurs. */
    @GetMapping("/reference-lists")
    public ResponseEntity<List<ListSummary>> lists(@RequestParam(required = false) String module) {
        return ResponseEntity.ok(lists.lists(module));
    }

    /** Valeurs d'une liste ; actives seulement, toutes avec {@code includeInactive=true} (administration). */
    @GetMapping("/reference-lists/{listCode}")
    public ResponseEntity<List<Value>> values(@PathVariable String listCode, @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(lists.values(listCode, includeInactive));
    }

    @Audited(module = "REFERENCE", action = "ADD_VALUE", ressource = "reference_value")
    @PostMapping("/reference-lists/{listCode}/values")
    public ResponseEntity<Value> add(@PathVariable String listCode, @Valid @RequestBody ValueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lists.add(listCode, request));
    }

    @Audited(module = "REFERENCE", action = "UPDATE_VALUE", ressource = "reference_value")
    @PatchMapping("/reference-lists/{listCode}/values/{code}")
    public ResponseEntity<Value> update(@PathVariable String listCode, @PathVariable String code, @Valid @RequestBody ValuePatch patch) {
        return ResponseEntity.ok(lists.update(listCode, code, patch));
    }

    /** Remplace toute la liste ; une valeur absente est desactivee, jamais supprimee. */
    @Audited(module = "REFERENCE", action = "REPLACE_LIST", ressource = "reference_value")
    @PutMapping("/reference-lists/{listCode}")
    public ResponseEntity<List<Value>> replace(@PathVariable String listCode, @Valid @RequestBody List<ValueRequest> values) {
        return ResponseEntity.ok(lists.replace(listCode, values));
    }

    @Audited(module = "REFERENCE", action = "UPDATE_LIST", ressource = "reference_list")
    @PatchMapping("/reference-lists/{listCode}")
    public ResponseEntity<ListSummary> updateList(@PathVariable String listCode, @Valid @RequestBody ListPatch patch) {
        return ResponseEntity.ok(lists.updateList(listCode, patch));
    }
}
