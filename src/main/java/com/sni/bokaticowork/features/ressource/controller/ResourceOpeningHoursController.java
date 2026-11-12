package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.service.support.ResourceOpeningHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Les horaires d'une ressource · les memes droits que le domaine des parametres qui les porte. */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/resources/{resourceCode}/opening-hours")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
public class ResourceOpeningHoursController {

    private final ResourceOpeningHoursService service;

    @GetMapping
    public ResponseEntity<ResourceOpeningHoursService.View> get(@PathVariable String resourceCode) {
        return ResponseEntity.ok(service.get(resourceCode));
    }

    @PutMapping
    public ResponseEntity<ResourceOpeningHoursService.View> replace(@PathVariable String resourceCode,
                                                                    @RequestBody List<ResourceOpeningHoursService.Range> hours,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(service.replace(resourceCode, hours, authentication.getName()));
    }

    @DeleteMapping
    public ResponseEntity<ResourceOpeningHoursService.View> clear(@PathVariable String resourceCode) {
        return ResponseEntity.ok(service.clear(resourceCode));
    }
}
