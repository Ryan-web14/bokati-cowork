package com.sni.bokaticowork.security.authentication.service.interfaces;


import com.sni.bokaticowork.security.authentication.dto.request.LoginRequest;
import com.sni.bokaticowork.security.authentication.dto.request.PasswordResetConfirmationRequest;
import com.sni.bokaticowork.security.authentication.dto.request.RefreshTokenRequest;
import com.sni.bokaticowork.security.authentication.dto.request.ValidateOttRequest;
import com.sni.bokaticowork.security.authentication.dto.request.RegisterMemberRequest;
import com.sni.bokaticowork.security.authentication.dto.response.CurrentUserResponse;
import com.sni.bokaticowork.security.authentication.dto.response.LoginResponse;
import com.sni.bokaticowork.security.authentication.dto.response.OttResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpReq);
    LoginResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpReq);
    void logout(String token);
    OttResponse ottLogin(String email);
    LoginResponse validateOttLogin(ValidateOttRequest request, HttpServletRequest httpReq);
    void resetPassword(String email);
    void validateResetPassword(PasswordResetConfirmationRequest request);
    CurrentUserResponse currentUser(Authentication authentication);

    OttResponse register(RegisterMemberRequest request);
    OttResponse requestUnlockAccount(String email);
    void confirmUnlockAccount(ValidateOttRequest request);
    OttResponse resendEmailVerification(String email);

}
