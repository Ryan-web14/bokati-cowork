package com.sni.bokaticowork.features.payment.provider.pawaypay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifies the HMAC-SHA256 signature on PawaPay callback requests.
 *
 * Configure the shared secret from your PawaPay dashboard:
 *   bokati.payment.pawaypay.callback-secret=<secret>
 *
 * When no secret is configured, verification is skipped (useful during local dev).
 * PawaPay sends the signature in the header: X-PawaPay-Signature: sha256=<hex>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PawapaySignatureVerifier {

    private final PawapayProperties properties;

    public boolean isEnabled() {
        return StringUtils.hasText(properties.getCallbackSecret());
    }

    /**
     * Returns true when the signature is valid, or when verification is disabled.
     */
    public boolean verify(String rawBody, String signatureHeader) {
        if (!isEnabled()) {
            return true;
        }
        if (!StringUtils.hasText(signatureHeader)) {
            log.warn("PawaPay callback received without X-PawaPay-Signature header");
            return false;
        }
        try {
            String expected = hmacSha256(properties.getCallbackSecret(), rawBody);
            // PawaPay sends "sha256=<hex>" — strip the prefix if present
            String actual = signatureHeader.startsWith("sha256=")
                    ? signatureHeader.substring(7)
                    : signatureHeader;
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.error("PawaPay signature verification error", ex);
            return false;
        }
    }

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}