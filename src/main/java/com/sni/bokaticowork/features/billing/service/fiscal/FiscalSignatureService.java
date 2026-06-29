package com.sni.bokaticowork.features.billing.service.fiscal;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class FiscalSignatureService {

    public static final String ALGORITHM = "HmacSHA256";

    /**
     * Clé HMAC stockée uniquement en variable d'environnement BILLING_FISCAL_SIGNING_KEY.
     * Ne doit jamais apparaître dans le code source, les logs ou la base de données.
     * Générer avec : openssl rand -hex 32
     */
    @Value("${billing.fiscal.signing-key:}")
    private String signingKey;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(signingKey)) {
            throw new IllegalStateException(
                    "BILLING_FISCAL_SIGNING_KEY n'est pas configuré. " +
                    "Générer une clé avec : openssl rand -hex 32");
        }
    }

    public String sign(String hash) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(hash.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Échec de la signature fiscale HMAC", e);
        }
    }

    public boolean verify(String hash, String signature) {
        if (hash == null || signature == null) return false;
        return MessageDigest.isEqual(
                sign(hash).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
