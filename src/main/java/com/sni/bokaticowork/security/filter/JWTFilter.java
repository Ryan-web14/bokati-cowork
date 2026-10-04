package com.sni.bokaticowork.security.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.config.PublicPaths;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.service.tokenService.implementation.JWTService;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final CustomUserDetailService userDetailService;
    private final UserSessionService sessionService;
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${app.documents.public-preview-enabled:false}")
    private boolean publicDocumentPreviewEnabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/ws")) {
            return true;
        }
        return isPublicApiRequest(request);
    }

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {

            try {
                String token = extractToken(request);
                if (token == null) {
                    // Sans jeton, aucune identite · la chaine de securite decidera si le chemin
                    // est public. Ce point authentifiait auparavant en administrateur, ce qui
                    // rendait toute l'API accessible a une requete sans en-tete.
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

            } catch (ExpiredJwtException e) {
                handleJwtAuthenticationFailure(request, response, filterChain, "JWT token expired", "Expired JWT token");
            } catch (JwtException | JWTVerificationException e) {
                handleJwtAuthenticationFailure(request, response, filterChain, "Invalid JWT Token", "Invalid JWT token");
            } catch (Exception e) {
                logger.error("Error processing JWT token", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing JWT token");
            }
    }

    private void handleJwtAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String responseMessage,
            String logMessage
    ) throws IOException, ServletException {
        SecurityContextHolder.clearContext();

        if (isPublicApiRequest(request)) {
            logger.debug(logMessage + " ignored for public endpoint " + request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug(logMessage + " for " + request.getRequestURI());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, responseMessage);
    }


    /**
     * N'authentifie qu'avec un jeton d'acces.
     *
     * <p>Le type n'etait pas verifie : un jeton de rafraichissement, valable sept jours, ou un
     * jeton de verification d'adresse authentifiaient n'importe quel appel d'API · ce qui annulait
     * l'interet d'un jeton d'acces court.</p>
     */
    private void processToken(String token){
        if (!jwtService.isAccessToken(token)) {
            throw new JwtException("Only an access token can authenticate an API call");
        }
        String email = jwtService.extractEmail(token);

        if(email != null && SecurityContextHolder.getContext().getAuthentication() == null){
            setAuthenticatedPrincipal((UserPrincipal) userDetailService.loadUserByUsername(email));
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

    /**
     * Les chemins publics · une seule liste, celle que la configuration de securite consomme.
     *
     * <p>Il y en avait deux, et elles divergeaient : trois chemins ouverts par
     * {@code SecurityConfig} manquaient ici, et ne fonctionnaient que par le repli sur
     * l'administrateur automatique. Voir {@link PublicPaths}.</p>
     */
    private boolean isPublicApiRequest(HttpServletRequest request) {
        return PublicPaths.matches(request) || isPublicDocumentPreview(request, request.getRequestURI());
    }

    /**
     * Token-gated document preview reachable without a bearer so browsers can load images
     * directly (&lt;img src&gt;). Only GET .../documents/{code}/signed-preview, which requires a
     * valid signed ?token=; plain /preview and /download stay authenticated. Toggle off with
     * app.documents.public-preview-enabled=false.
     */
    private boolean isPublicDocumentPreview(HttpServletRequest request, String uri) {
        return publicDocumentPreviewEnabled
                && "GET".equalsIgnoreCase(request.getMethod())
                && uri.startsWith(ApiPath.V1 + "/documents/")
                && uri.endsWith("/signed-preview");
    }
}
