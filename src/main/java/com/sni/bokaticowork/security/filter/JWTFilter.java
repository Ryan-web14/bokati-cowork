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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final CustomUserDetailService userDetailService;
    private final UserSessionService sessionService;
    private static final String BEARER_PREFIX = "Bearer ";

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {

            try {
                String token = extractToken(request);
                if (token == null) {
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
            UserPrincipal userPrincipal = (UserPrincipal) userDetailService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentification = new UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    userPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentification);
            logger.debug("Authenticated user: " + userPrincipal.getUser().getEmail());
        }
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
        return uri != null
                && uri.startsWith(ApiPath.V1)
                && !uri.startsWith(ApiPath.V1 + "/admin/");
    }
}

