package com.sni.bokaticowork.security.admin.user.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.user.dto.request.UpdateMySignatureRequest;
import com.sni.bokaticowork.security.admin.user.dto.request.UpdateStaffProfileRequest;
import com.sni.bokaticowork.security.admin.user.dto.response.StaffProfileResponse;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Fiche collaborateur (lot B12) : fonction, signature du visa, superieur direct.
 * L'administrateur gere toutes les fiches ; chacun depose sa propre signature.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1)
public class StaffProfileController {

    private final StaffProfileService staffProfileService;

    @GetMapping("/admin/users/{userCode}/staff-profile")
    @PreAuthorize("hasAuthority('SYSTEM:USERS')")
    public ResponseEntity<StaffProfileResponse> get(@PathVariable String userCode) {
        return ResponseEntity.ok(staffProfileService.get(userCode));
    }

    @Audited(module = "USER", action = "STAFF_PROFILE_UPDATE", ressource = "user")
    @PutMapping("/admin/users/{userCode}/staff-profile")
    @PreAuthorize("hasAuthority('SYSTEM:USERS')")
    public ResponseEntity<StaffProfileResponse> update(@PathVariable String userCode,
                                                       @Valid @RequestBody UpdateStaffProfileRequest request) {
        return ResponseEntity.ok(staffProfileService.update(userCode, request));
    }

    @GetMapping("/staff/me/profile")
    public ResponseEntity<StaffProfileResponse> me() {
        return ResponseEntity.ok(staffProfileService.me());
    }

    @Audited(module = "USER", action = "STAFF_SIGNATURE_UPDATE", ressource = "user")
    @PutMapping("/staff/me/signature")
    public ResponseEntity<StaffProfileResponse> updateMySignature(@Valid @RequestBody UpdateMySignatureRequest request) {
        return ResponseEntity.ok(staffProfileService.updateMySignature(request.signatureImageBase64()));
    }

    @Audited(module = "USER", action = "STAFF_SIGNATURE_CLEAR", ressource = "user")
    @DeleteMapping("/staff/me/signature")
    public ResponseEntity<StaffProfileResponse> clearMySignature() {
        return ResponseEntity.ok(staffProfileService.clearMySignature());
    }
}
