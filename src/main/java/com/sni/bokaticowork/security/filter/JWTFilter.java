package com.sni.bokaticowork.security.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.service.tokenService.implementation.JWTService;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final CustomUserDetailService userDetailService;
    private final UserSessionService sessionService;
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${app.security.auto-admin.enabled:true}")
    private boolean autoAdminEnabled;

    @Value("${app.security.auto-admin.email:admin@bokati.com}")
    private String autoAdminEmail;

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {

            try {
                String token = extractToken(request);
                if (token == null) {
                    authenticateAutoAdmin();
                    filterChain.doFilter(request, response);
                    return;
                }

                String sessionId  = jwtService.extractSessionId(token);
                request.setAttribute("sessionId", sessionId);
                if(sessionService.isSessionRevoked(sessionId)) {
                    logger.debug("Session revoked or missing for token, skipping authentication");
                    filterChain.doFilter(request, response);
                    return;
                }

                processToken(token);
                filterChain.doFilter(request, response);

            } catch (JWTVerificationException e) {
                if (isPublicApiRequest(request)) {
                    SecurityContextHolder.clearContext();
                    logger.debug("Ignoring invalid JWT for public endpoint " + request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
                logger.error("JWT Verification failed", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
            } catch (Exception e) {
                logger.error("Error processing JWT token", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing JWT token");
            }
    }


    private void processToken(String token){
        String email = jwtService.extractEmail(token);

        if(email != null && SecurityContextHolder.getContext().getAuthentication() == null){
            setAuthenticatedPrincipal((UserPrincipal) userDetailService.loadUserByUsername(email));
        }
    }

    private void authenticateAutoAdmin() {
        if (!autoAdminEnabled || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        if (!StringUtils.hasText(autoAdminEmail)) {
            logger.warn("Auto admin authentication is enabled but app.security.auto-admin.email is empty");
            return;
        }
        try {
            UserPrincipal userPrincipal = (UserPrincipal) userDetailService.loadUserByUsername(autoAdminEmail.trim());
            setAuthenticatedPrincipal(userPrincipal);
            logger.debug("Authenticated user: " + userPrincipal.getUser().getEmail());
        } catch (Exception ex) {
            logger.warn("Unable to load auto admin user " + autoAdminEmail, ex);
        }
    }

    private void setAuthenticatedPrincipal(UserPrincipal userPrincipal) {
        UsernamePasswordAuthenticationToken authentification = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentification);
    }

    public String extractToken(HttpServletRequest request) {
        String authBearer = request.getHeader("Authorization");

        if(authBearer != null && authBearer.startsWith(BEARER_PREFIX)){
            String token = authBearer.substring(BEARER_PREFIX.length());

            if(token.isBlank()){
                logger.warn("Invalid token format: Bearer");
                return null;
            }
            return token;
        }
        return null;
    }

    private boolean isPublicApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (
                uri.equals(ApiPath.V1 + "/auth/login")
                        || uri.equals(ApiPath.V1 + "/auth/refresh")
                        || uri.equals(ApiPath.V1 + "/auth/register")
                        || uri.startsWith(ApiPath.V1 + "/auth/ott/")
                        || uri.startsWith(ApiPath.V1 + "/auth/password-reset/")
                        || uri.equals(ApiPath.V1 + "/auth/unlock-account")
                        || uri.equals(ApiPath.V1 + "/auth/unlock-account/confirm")
                        || uri.equals(ApiPath.V1 + "/auth/email/verify/resend")
                        || uri.equals(ApiPath.V1 + "/payments/mobile-money/pawapay/callback")
                        || uri.equals(ApiPath.V1 + "/payments/mobile-money/pawaypay/callback")
                        || uri.equals(ApiPath.V1 + "/payments/mobile-money/pawapay/refund-callback")
                        || uri.equals(ApiPath.V1 + "/payments/mobile-money/pawaypay/refund-callback")
                        || uri.startsWith("/verify/")
        );
    }
}

