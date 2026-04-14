package com.sni.bokaticowork.security.admin.provisioning.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.provisioning.service.interfaces.UserProvisioningService;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.dto.response.UserResponse;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/provisioning")
public class UserProvisioningController {

    private final UserProvisioningService userProvisioningService;
    private final UserService userService;

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<UserResponse> bootstrapAdmin(@Valid @RequestBody UserRequest request) {
        var user = userProvisioningService.initializeGlobalAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.getUserByEmail(user.getEmail()));
    }

    @PostMapping("/staff")
    public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody UserRequest request, Authentication authentication) {
        var user = userProvisioningService.createStaff(request, actor(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.getUserByEmail(user.getEmail()));
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "SYSTEM"
                : authentication.getName();
    }
}
