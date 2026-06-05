package com.sni.bokaticowork.features.support.service.implementation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class CsatTokenService {

    @Value("${app.security.token-hash.secret}")
    private String secret;

    public String generate(String ticketNumber, int score) {
        return hmac(ticketNumber + "|" + score);
    }

    public boolean validate(String ticketNumber, int score, String token) {
        if (!StringUtils.hasText(token)) return false;
        String expected = generate(ticketNumber, score);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("CSAT token generation failed", ex);
        }
    }
}
