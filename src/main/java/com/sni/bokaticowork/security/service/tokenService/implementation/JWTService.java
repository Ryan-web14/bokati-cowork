package com.sni.bokaticowork.security.service.tokenService.implementation;

import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JWTService {

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpiration;

    @Value("${app.security.jwt.refresh-token-expiration-ms:604800000}")
    @Getter
    private long refreshTokenExpiration;

    @Value("${app.security.jwt.verification-token-expiration-ms:900000}")
    private long verificationTokenExpiration;

    public String generateAccesToken(Users user, String sessionId) {
        return buildToken(user, sessionId, accessTokenExpiration);
    }

    public String generateRefreshToken(Users user, String sessionId) {
        return buildToken(user, sessionId, refreshTokenExpiration);
    }

    public String generateVerificationToken(Users user, String sessionId) {
        return buildToken(user, sessionId, verificationTokenExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, Users user) {
        String email = extractEmail(token);
        return email.equalsIgnoreCase(user.getEmail()) && !isTokenExpired(token);
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        if (claims.getExpiration().before(new Date())) {
            throw new JwtException("Token is expired");
        }
        return claims.getSubject();
    }

    public String extractSessionId(String token) {
        Claims claims = extractAllClaims(token);
        if (claims.getExpiration().before(new Date())) {
            throw new JwtException("Token is expired");
        }
        Object sessionId = claims.get("sessionId");
        if (sessionId == null) {
            throw new JwtException("Session identifier missing from token");
        }
        return sessionId.toString();
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtException("Invalid JWT token", ex);
        }
    }

    private String buildToken(Users user, String sessionId, long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        UserPrincipal principal = new UserPrincipal(user);

        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiration(expirationDate);

        if (!"VerificationSession".equals(sessionId)) {
            builder.claim("roles", principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());
        }

        return builder.signWith(getSigningKey()).compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
