package com.sni.bokaticowork.core.settings.platform;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Les parametres de la plateforme · {@code SUPER_ADMIN} et {@code ADMIN}, jamais {@code MANAGER}. Le
 * service restreint l'{@code ADMIN} aux domaines qui lui sont ouverts.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/settings")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
public class PlatformSettingsController {

    private final PlatformSettingsAdminService service;

    public record UpdateRequest(String value, @Size(max = 500) String reason) {
    }

    @GetMapping("/domains")
    public ResponseEntity<List<PlatformSettingsAdminService.DomainView>> domains(Authentication authentication) {
        return ResponseEntity.ok(service.domains(authentication));
    }

    @GetMapping
    public ResponseEntity<List<PlatformSettingsAdminService.SettingView>> list(@RequestParam(required = false) SettingDomain domain,
                                                                               Authentication authentication) {
        return ResponseEntity.ok(service.list(domain, authentication));
    }

    @PutMapping("/{key}")
    public ResponseEntity<PlatformSettingsAdminService.SettingView> update(@PathVariable String key,
                                                                           @jakarta.validation.Valid @RequestBody UpdateRequest request,
                                                                           Authentication authentication) {
        return ResponseEntity.ok(service.update(key, request.value(), request.reason(), authentication));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<PlatformSettingsAdminService.SettingView> reset(@PathVariable String key,
                                                                          @RequestParam(required = false) String reason,
                                                                          Authentication authentication) {
        return ResponseEntity.ok(service.reset(key, reason, authentication));
    }

    @GetMapping("/{key}/history")
    public ResponseEntity<List<PlatformSettingsAdminService.ChangeView>> history(@PathVariable String key,
                                                                                 Authentication authentication) {
        return ResponseEntity.ok(service.history(key, authentication));
    }
}
