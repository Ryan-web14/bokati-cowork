package com.sni.bokaticowork.security.authentication.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.authentication.dto.request.EmailRequest;
import com.sni.bokaticowork.security.authentication.dto.request.LoginRequest;
import com.sni.bokaticowork.security.authentication.dto.request.PasswordResetConfirmationRequest;
import com.sni.bokaticowork.security.authentication.dto.request.RefreshTokenRequest;
import com.sni.bokaticowork.security.authentication.dto.request.RegisterMemberRequest;
import com.sni.bokaticowork.security.authentication.dto.request.ValidateOttRequest;
import com.sni.bokaticowork.security.authentication.dto.response.CurrentUserResponse;
import com.sni.bokaticowork.security.authentication.dto.response.LoginResponse;
import com.sni.bokaticowork.security.authentication.dto.response.OttResponse;
import com.sni.bokaticowork.security.authentication.service.interfaces.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/auth")
public class AuthenticationController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authenticationService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authenticationService.refreshToken(request, httpRequest));
    }

    @PostMapping("/ott/request")
    public ResponseEntity<OttResponse> requestOtt(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authenticationService.ottLogin(request.getEmail()));
    }

    @PostMapping("/ott/validate")
    public ResponseEntity<LoginResponse> validateOtt(@Valid @RequestBody ValidateOttRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authenticationService.validateOttLogin(request, httpRequest));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        authenticationService.resetPassword(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmationRequest request) {
        authenticationService.validateResetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authenticationService.currentUser(authentication));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authenticationService.logout(extractToken(authorizationHeader));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<OttResponse> register(@Valid @RequestBody RegisterMemberRequest request) {
        return ResponseEntity.status(201).body(authenticationService.register(request));
    }

    @PostMapping("/unlock-account")
    public ResponseEntity<OttResponse> requestUnlockAccount(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authenticationService.requestUnlockAccount(request.getEmail()));
    }

    @PostMapping("/unlock-account/confirm")
    public ResponseEntity<Void> confirmUnlockAccount(@Valid @RequestBody ValidateOttRequest request) {
        authenticationService.confirmUnlockAccount(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verify/resend")
    public ResponseEntity<OttResponse> resendEmailVerification(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authenticationService.resendEmailVerification(request.getEmail()));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        return token.isBlank() ? null : token;
    }
}
