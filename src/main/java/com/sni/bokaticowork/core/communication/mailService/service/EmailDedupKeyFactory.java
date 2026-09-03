package com.sni.bokaticowork.core.communication.mailService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Builds the deduplication key stored on {@code email_delivery_log}.
 *
 * <p>Two policies, picked from the data the caller already supplies:
 *
 * <ul>
 *   <li><b>Business</b> · the caller passes a related type <i>and</i> a related code
 *       (invoice, contract, KYC case…). Those are transactional mails tied to one record,
 *       so the same subject for the same record and recipient ships once per day.</li>
 *   <li><b>Generic</b> · no business reference. Keyed on recipient and subject alone, so the
 *       window stays short: it only has to absorb a worker re-firing, not suppress a genuine
 *       second mail hours later.</li>
 * </ul>
 *
 * <p>The key embeds a time bucket so it can be enforced by a unique index. Bucket boundaries
 * would otherwise let a duplicate slip through when two attempts straddle them, so lookups
 * check the previous bucket as well · that turns the discrete bucket into a true sliding window
 * while keeping the race protection a unique index gives.
 */
@Slf4j
@Component
public class EmailDedupKeyFactory {

    private static final String BUSINESS_PREFIX = "B";
    private static final String GENERIC_PREFIX = "G";

    @Value("${bokati.email.dedup.enabled:true}")
    private boolean enabled;

    @Value("${bokati.email.dedup.business-window-seconds:86400}")
    private long businessWindowSeconds;

    @Value("${bokati.email.dedup.generic-window-seconds:3600}")
    private long genericWindowSeconds;

    public Keys build(String recipient, String subject, String relatedType, String relatedCode) {
        if (!enabled || !StringUtils.hasText(recipient)) {
            return Keys.disabled();
        }

        boolean business = StringUtils.hasText(relatedType) && StringUtils.hasText(relatedCode);
        long window = business ? businessWindowSeconds : genericWindowSeconds;
        if (window <= 0) {
            return Keys.disabled();
        }

        String logical = business
                ? String.join("|", BUSINESS_PREFIX, norm(relatedType), norm(relatedCode), norm(recipient), norm(subject))
                : String.join("|", GENERIC_PREFIX, norm(recipient), norm(subject));

        long bucket = Instant.now().getEpochSecond() / window;
        String current = hash(logical, bucket);
        return new Keys(current, List.of(current, hash(logical, bucket - 1)));
    }

    private String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String logical, long bucket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((logical + "|" + bucket).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    /**
     * @param key         the key persisted on the new row, or null when dedup is off
     * @param lookupKeys  current and previous bucket, both checked before queueing
     */
    public record Keys(String key, List<String> lookupKeys) {

        public static Keys disabled() {
            return new Keys(null, List.of());
        }

        public boolean active() {
            return key != null;
        }
    }
}
