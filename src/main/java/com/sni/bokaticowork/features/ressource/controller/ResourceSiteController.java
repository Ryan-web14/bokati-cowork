package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceLocationRequests;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceLocationResponses;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSites;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Lot R5b : sites, etages et plan en direct. */
@RestController
@RequiredArgsConstructor
public class ResourceSiteController {

    private static final String SITES = ApiPath.V1 + "/resource-sites";
    private static final String FLOORS = ApiPath.V1 + "/resource-floors";

    private final ResourceSites sites;

    @GetMapping(SITES)
    public ResponseEntity<List<ResourceLocationResponses.Site>> list() {
        return ResponseEntity.ok(sites.sites());
    }

    @GetMapping(SITES + "/{code}")
    public ResponseEntity<ResourceLocationResponses.Site> get(@PathVariable String code) {
        return ResponseEntity.ok(sites.site(code));
    }

    @PostMapping(SITES)
    @Audited(module = "RESOURCE", action = "CREATE_SITE", ressource = "resource_site")
    public ResponseEntity<ResourceLocationResponses.Site> create(@RequestBody ResourceLocationRequests.Site request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sites.createSite(request));
    }

    @PatchMapping(SITES + "/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE_SITE", ressource = "resource_site")
    public ResponseEntity<ResourceLocationResponses.Site> update(@PathVariable String code,
                                                                 @RequestBody ResourceLocationRequests.Site request) {
        return ResponseEntity.ok(sites.updateSite(code, request));
    }

    @DeleteMapping(SITES + "/{code}")
    @Audited(module = "RESOURCE", action = "DELETE_SITE", ressource = "resource_site")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        sites.deleteSite(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(SITES + "/{code}/floors")
    public ResponseEntity<List<ResourceLocationResponses.Floor>> floors(@PathVariable String code) {
        return ResponseEntity.ok(sites.floors(code));
    }

    @PostMapping(SITES + "/{code}/floors")
    @Audited(module = "RESOURCE", action = "CREATE_FLOOR", ressource = "resource_floor")
    public ResponseEntity<ResourceLocationResponses.Floor> createFloor(@PathVariable String code,
                                                                       @RequestBody ResourceLocationRequests.Floor request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sites.createFloor(code, request));
    }

    @PatchMapping(FLOORS + "/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE_FLOOR", ressource = "resource_floor")
    public ResponseEntity<ResourceLocationResponses.Floor> updateFloor(@PathVariable String code,
                                                                       @RequestBody ResourceLocationRequests.Floor request) {
        return ResponseEntity.ok(sites.updateFloor(code, request));
    }

    @DeleteMapping(FLOORS + "/{code}")
    @Audited(module = "RESOURCE", action = "DELETE_FLOOR", ressource = "resource_floor")
    public ResponseEntity<Void> deleteFloor(@PathVariable String code) {
        sites.deleteFloor(code);
        return ResponseEntity.noContent().build();
    }

    /** Plan de l'etage a un instant (maintenant par defaut), avec l'etat de chaque ressource. */
    @GetMapping(FLOORS + "/{code}/plan")
    public ResponseEntity<ResourceLocationResponses.FloorPlan> plan(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return ResponseEntity.ok(sites.plan(code, at));
    }
}
