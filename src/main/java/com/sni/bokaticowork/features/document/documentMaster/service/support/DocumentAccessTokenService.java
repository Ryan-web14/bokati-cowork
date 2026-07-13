package com.sni.bokaticowork.features.document.documentMaster.service.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues and verifies short-lived, document-scoped signed tokens so a browser can load a
 * document preview directly (&lt;img src&gt;) without a bearer token, while the URL remains
 * unguessable and time-limited. Token format: base64url(code:exp).base64url(HMAC-SHA256).
 */
@Service
public class DocumentAccessTokenService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    // Falls back to the JWT secret so no new env var is required in existing deployments.
    @Value("${app.documents.preview-token.secret:${app.security.jwt.secret:Q7mP2xL9vB4nH6sT1yK8dF5wR3cZ0aEQ7mP2xL9vB4nH6sT1yK8dF5wR3cZ0aE}}")
    private String secret;

    @Value("${app.documents.preview-token.ttl-seconds:300}")
    private long ttlSeconds;

    public IssuedToken issue(String documentCode) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String encodedPayload = B64.encodeToString((documentCode + ":" + exp).getBytes(StandardCharsets.UTF_8));
        String token = encodedPayload + "." + B64.encodeToString(hmac(encodedPayload));
        return new IssuedToken(token, Instant.ofEpochSecond(exp));
    }

    public boolean verify(String documentCode, String token) {
        if (!StringUtils.hasText(documentCode) || !StringUtils.hasText(token)) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return false;
        }
        String encodedPayload = token.substring(0, dot);
        byte[] providedSig;
        try {
            providedSig = B64D.decode(token.substring(dot + 1));
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (!MessageDigest.isEqual(providedSig, hmac(encodedPayload))) {
            return false;
        }
        String payload;
        try {
            payload = new String(B64D.decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        int sep = payload.lastIndexOf(':');
        if (sep <= 0) {
            return false;
        }
        if (!payload.substring(0, sep).equals(documentCode)) {
            return false;
        }
        try {
            return Instant.now().getEpochSecond() <= Long.parseLong(payload.substring(sep + 1));
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign document access token", ex);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
