package com.sni.bokaticowork.security.authentication.service.implementation;


import com.sni.bokaticowork.core.audit.dto.response.UserSessionToken;
import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.core.exception.customs.AccountLockedException;
import com.sni.bokaticowork.core.exception.customs.BadCredentialException;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.provisioning.service.interfaces.UserProvisioningService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.authentication.dto.request.LoginRequest;
import com.sni.bokaticowork.security.authentication.dto.request.PasswordResetConfirmationRequest;
import com.sni.bokaticowork.security.authentication.dto.request.RefreshTokenRequest;
import com.sni.bokaticowork.security.authentication.dto.request.RegisterMemberRequest;
import com.sni.bokaticowork.security.authentication.dto.request.ValidateOttRequest;
import com.sni.bokaticowork.security.authentication.dto.response.CurrentUserResponse;
import com.sni.bokaticowork.security.authentication.dto.response.LoginResponse;
import com.sni.bokaticowork.security.authentication.dto.response.OttResponse;
import com.sni.bokaticowork.security.authentication.service.interfaces.AuthenticationService;
import com.sni.bokaticowork.security.authentication.service.interfaces.OneTimeTokenService;
import com.sni.bokaticowork.security.service.tokenService.implementation.JWTService;
import com.sni.bokaticowork.security.service.tokenService.interfaces.RefreshTokenService;
import com.sni.bokaticowork.security.service.passwordResetService.interfaces.PasswordResetService;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class   AuthenticationServiceImpl implements AuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
    private final UserSessionService sessionService;
    private final AuthenticationManager authenticationManager;
    private final OneTimeTokenService oneTimeTokenService;
    private final UserProvisioningService userProvisioningService;
    private final UserService userService;
    private final CustomUserDetailService userDetailService;
    private final PasswordResetService passwordResetService;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpReq) throws AuthenticationException{

        log.debug("Authenticating user {}", request.getEmail());
            UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(
                    request.getEmail(),request.getPassword());
            return authenticationAndCreateSesison(token, httpReq);
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpReq) {
        String oldRefreshToken = request.getRefreshToken();
        var storedRefreshToken = refreshTokenService.getActiveToken(oldRefreshToken);
        Users staleUser = storedRefreshToken.getUser();

        if (!jwtService.isTokenValid(oldRefreshToken, staleUser)) {
            refreshTokenService.revokeToken(oldRefreshToken);
            sessionService.invalidateSessionByToken(oldRefreshToken);
            throw new BadCredentialException(INVALID_CREDENTIALS_MESSAGE);
        }

        // Reload user within this transaction so all lazy associations (roles, permissions)
        // are initialized before buildAccessToken creates a new UserPrincipal.
        // open-in-view=false means the session from getActiveToken() is already closed.
        UserPrincipal freshPrincipal = (UserPrincipal) userDetailService.loadUserByUsername(staleUser.getEmail());
        Users freshUser = freshPrincipal.getUser();

        String sessionId = jwtService.extractSessionId(oldRefreshToken);
        String newAccessToken = jwtService.generateAccesToken(freshUser, sessionId);
        String newRefreshToken = jwtService.generateRefreshToken(freshUser, sessionId);

        refreshTokenService.rotateToken(oldRefreshToken, newRefreshToken);
        sessionService.refreshSession(sessionId, newRefreshToken);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokenService.revokeToken(token);
        try {
            sessionService.invalidateSessionByToken(token);
        } catch (ResourceNotFoundException ignored) {
            log.debug("No session found for logout token");
        }
    }

    @Override
    public OttResponse ottLogin(String email) {

        if(!userService.userExists(email)){
            log.error("No User with email {} exists", email);
        }

        String verificationToken = oneTimeTokenService.generateOneTimeToken(userService
                .getUserByEmailForService(email));

        return OttResponse.builder()
                .verificationToken(verificationToken)
                .message("OTP sent to email")
                .build();
    }

    @Override
    @Transactional
    public LoginResponse validateOttLogin(ValidateOttRequest ottRequest, HttpServletRequest request) {

        String email = oneTimeTokenService.validateOneTimeToken(ottRequest.getOttToken(), ottRequest.getVerificationToken()).get(true);

        if(email.isEmpty()){
            throw new BadCredentialException("Invalid token");
        }

        userProvisioningService.completePortalVerification(email, "OTT_VERIFICATION");
        UserPrincipal principal = (UserPrincipal) userDetailService.loadUserByUsername(email);

        return authenticationAndCreateSesison(principal, request);
    }

    @Override
    public void resetPassword(String email) {
        passwordResetService.generatePasswordResetToken(userService.getUserByEmailForService(email));
    }

    @Override
    public void validateResetPassword(PasswordResetConfirmationRequest request) {
        passwordResetService.validatePasswordResetToken(request.getToken(), request.getNewPassword());
    }

    @Override
    public CurrentUserResponse currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadCredentialException("User is not authenticated");
        }
        return CurrentUserResponse.builder()
                .userId(principal.getUser().getUserId())
                .email(principal.getUser().getEmail())
                .accountEnabled(Boolean.TRUE.equals(principal.getUser().getIsAccountEnabled()))
                .authorities(principal.getAuthorities().stream().map(Object::toString).sorted().toList())
                .build();
    }


    @Override
    @Transactional
    public OttResponse register(RegisterMemberRequest request) {
        CreateMemberRequest createReq = CreateMemberRequest.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(request.getPassword())
                .phone(request.getPhone())
                .phoneCountry(request.getPhoneCountry())
                .whatsappPhone(request.getWhatsappPhone())
                .customerType("PERSON")
                .address(request.getAddress())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .preferredCommunicationChannel(request.getPreferredCommunicationChannel())
                .generatePassword(false)
                .build();
        userProvisioningService.registerMemberFromPortal(createReq);
        Users user = userService.getUserByEmailForService(request.getEmail());
        String verificationToken = oneTimeTokenService.generateOneTimeToken(user);
        return OttResponse.builder()
                .verificationToken(verificationToken)
                .message("Registration successful. Please verify your email with the code sent to " + request.getEmail())
                .build();
    }

    @Override
    public OttResponse requestUnlockAccount(String email) {
        if (!userService.userExists(email)) {
            return OttResponse.builder()
                    .verificationToken(null)
                    .message("If an account exists for this email, an unlock code has been sent.")
                    .build();
        }
        Users user = userService.getUserByEmailForService(email);
        String verificationToken = oneTimeTokenService.generateOneTimeToken(user);
        return OttResponse.builder()
                .verificationToken(verificationToken)
                .message("If an account exists for this email, an unlock code has been sent.")
                .build();
    }

    @Override
    @Transactional
    public void confirmUnlockAccount(ValidateOttRequest request) {
        Map<Boolean, String> result = oneTimeTokenService.validateOneTimeToken(request.getOttToken(), request.getVerificationToken());
        String email = result.get(true);
        if (email == null || email.isEmpty()) {
            throw new BadCredentialException("Invalid or expired unlock code");
        }
        userService.unlockAccount(email);
    }

    @Override
    public OttResponse resendEmailVerification(String email) {
        if (!userService.userExists(email)) {
            return OttResponse.builder()
                    .verificationToken(null)
                    .message("If an account exists for this email, a verification code has been sent.")
                    .build();
        }
        Users user = userService.getUserByEmailForService(email);
        String verificationToken = oneTimeTokenService.generateOneTimeToken(user);
        return OttResponse.builder()
                .verificationToken(verificationToken)
                .message("Verification code resent to " + email)
                .build();
    }

    /**
     * Authenticates a user using the provided authentication token and establishes a new session, use for normal login(email, password)
     *
     * @param token the authentication token containing the user's credentials
     * @param request the HTTP servlet request associated with the current session
     * @return a {@code LoginResponse} object containing the new access token and refresh token
     * @throws BadCredentialException if the authentication process fails due to invalid credentials
     */
    private LoginResponse authenticationAndCreateSesison(UsernamePasswordAuthenticationToken token, HttpServletRequest request){
        try{
            Authentication authentication = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            sessionService.invalidateOldestSessionByUser(principal.getUser());
            return createSessionAndResponse(request, principal);
        } catch (LockedException e) {
            log.warn("Login attempt on locked account: {}", token.getPrincipal());
            throw new AccountLockedException("Your account is locked. Use the unlock-account flow to regain access.");
        } catch (AuthenticationException e){
            Object principal = token.getPrincipal();
            if (principal instanceof String email) {
                userService.recordLoginFailure(email);
            }
            log.warn("Error authenticating user", e);
            throw new BadCredentialException(INVALID_CREDENTIALS_MESSAGE);
        }
    }

    /**
     * Authenticates a user based on the provided {@code UserPrincipal} and creates a new session , use for ott
     *
     * @param principal the authenticated user principal containing user details and authorities
     * @param request the HTTP servlet request associated with the current session
     * @return a {@code LoginResponse} object containing the generated access token and refresh token
     */
    private LoginResponse authenticationAndCreateSesison(UserPrincipal principal, HttpServletRequest request){
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        authToken.setDetails((new WebAuthenticationDetailsSource().buildDetails(request)));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        return createSessionAndResponse(request, principal);
    }

    private LoginResponse createSessionAndResponse(HttpServletRequest request, UserPrincipal principal) {
        UserSessionToken tokens = sessionService.createSession(request, principal);
        userService.recordLoginSuccess(principal.getUser().getEmail());

        return LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .emailVerified(principal.getUser().emailVerified())
                .roles(principal.getAuthorities().stream().map(Object::toString).sorted().toList())
                .build();
    }

}
