package com.sni.bokaticowork.core.communication.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppClient;
import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppProperties;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Webhook des statuts WhatsApp envoyes par Infobip (remise, lecture), seule entree ouverte de ce
 * service. Chaque appel doit porter la signature HMAC du corps brut ({@code X-Hub-Signature}) avec
 * le secret de configuration ; sinon il est refuse. Il ne renvoie aucune donnee.
 */
@Slf4j
@RestController
@RequestMapping(ApiPath.V1 + "/public/webhooks/infobip")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final InfobipWhatsAppProperties properties;
    private final WhatsAppService whatsapp;
    private final ObjectMapper objectMapper;

    @PostMapping("/whatsapp")
    public ResponseEntity<Void> receive(@RequestBody(required = false) String body,
                                        @RequestHeader(value = "X-Hub-Signature", required = false) String signature) {
        if (!verify(properties.getWebhookSecret(), properties.getWebhookAlgorithm(), body, signature)) {
            log.warn("Webhook WhatsApp refuse : signature absente ou fausse");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            JsonNode results = objectMapper.readTree(body).path("results");
            int applied = 0;
            for (JsonNode item : results) {
                if (item.hasNonNull("seenAt")) {
                    applied += whatsapp.applySeen(item.path("messageId").asText(null), InfobipWhatsAppClient.instant(item.path("seenAt").asText(null))) ? 1 : 0;
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = objectMapper.convertValue(item, Map.class);
                    applied += whatsapp.applyReport(InfobipWhatsAppClient.report(map)) ? 1 : 0;
                }
            }
            log.debug("Webhook WhatsApp : {} statut(s) applique(s) sur {}", applied, results.size());
        } catch (Exception exception) {
            log.warn("Webhook WhatsApp illisible : {}", exception.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Signature HMAC du corps brut, en hexadecimal ou en Base64, avec ou sans prefixe
     * d'algorithme ({@code sha256=...}). Comparaison a temps constant. Sans secret : refus.
     */
    static boolean verify(String secret, String algorithm, String body, String signature) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(signature) || body == null) {
            return false;
        }
        String java = switch (algorithm == null ? "HMAC_SHA_256" : algorithm.toUpperCase(Locale.ROOT)) {
            case "HMAC_SHA_1" -> "HmacSHA1";
            case "HMAC_SHA_224" -> "HmacSHA224";
            case "HMAC_SHA_384" -> "HmacSHA384";
            case "HMAC_SHA_512" -> "HmacSHA512";
            case "HMAC_MD5" -> "HmacMD5";
            default -> "HmacSHA256";
        };
        try {
            Mac mac = Mac.getInstance(java);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), java));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String value = signature.trim();
            int equals = value.indexOf('=');
            if (equals > 0 && equals < 12 && value.substring(0, equals).matches("[A-Za-z0-9]+")) {
                value = value.substring(equals + 1);
            }
            byte[] given;
            if (value.matches("[0-9a-fA-F]+") && value.length() == expected.length * 2) {
                given = HexFormat.of().parseHex(value);
            } else {
                given = Base64.getDecoder().decode(value);
            }
            return MessageDigest.isEqual(expected, given);
        } catch (Exception exception) {
            return false;
        }
    }
}
