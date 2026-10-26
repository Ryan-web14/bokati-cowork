package com.sni.bokaticowork.features.payment.provider.pawaypay;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifie la signature des rappels de PawaPay, selon la RFC 9421 telle que PawaPay l'applique.
 *
 * <p>Le code precedent attendait un HMAC dans un en-tete {@code X-PawaPay-Signature} que PawaPay
 * n'envoie pas : aucun rappel n'etait jamais reconnu signe, et chacun coutait une relecture du
 * statut. PawaPay signe ses rappels (une fois les rappels signes actives dans son tableau de bord)
 * avec {@code Signature}, {@code Signature-Input} et {@code Content-Digest}, verifiables avec ses
 * cles publiques ({@code GET /v2/public-key/http}).</p>
 *
 * <p>Ce qui est controle :</p>
 * <ul>
 *   <li>le condensat {@code Content-Digest} correspond aux octets exacts du corps, et il est couvert
 *       par la signature · sans quoi le corps pourrait etre change sans toucher a la signature ;</li>
 *   <li>la signature n'a pas expire ({@code expires}, avec cinq minutes de tolerance d'horloge) ;</li>
 *   <li>la base de signature reconstruite (composants dans l'ordre de {@code Signature-Input}, puis
 *       {@code @signature-params}) est signee par une cle publique de PawaPay.</li>
 * </ul>
 *
 * <p>Un refus ici n'est jamais fatal : la passerelle relit alors le statut chez PawaPay avec notre
 * cle d'API, et c'est cette relecture qui fait foi. Une signature valide epargne seulement cet appel
 * et rend le rappel immediatement exploitable.</p>
 */
@Slf4j
@Component
public class PawapaySignatureVerifier {

    static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
    static final Duration KEY_REFRESH_INTERVAL = Duration.ofMinutes(5);

    private static final Pattern MEMBER = Pattern.compile("([A-Za-z0-9_*.-]+)=(\\([^)]*\\)[^,]*)");
    private static final Pattern COMPONENT = Pattern.compile("\"([^\"]+)\"");

    /** Ce que le rappel apporte · l'appelant HTTP le construit, la verification n'en sait pas plus. */
    public record CallbackRequest(String method, List<String> authorities, String path,
                                  Map<String, String> headers, byte[] body) {

        public CallbackRequest {
            Map<String, String> lower = new LinkedHashMap<>();
            if (headers != null) {
                headers.forEach((name, value) -> {
                    if (name != null && value != null) {
                        lower.put(name.toLowerCase(Locale.ROOT), value);
                    }
                });
            }
            headers = lower;
            authorities = authorities == null ? List.of() : authorities;
            body = body == null ? new byte[0] : body;
        }

        public String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        /** Le rappel porte-t-il une signature RFC 9421 ? */
        public boolean signed() {
            return StringUtils.hasText(header("signature")) && StringUtils.hasText(header("signature-input"));
        }
    }

    /** Une cle publique de PawaPay, telle que {@code /v2/public-key/http} la rend. */
    public record PublicKeyEntry(String id, String key) {
    }

    private final ObjectProvider<PawapayClient> client;
    private final Clock clock;

    private volatile Map<String, PublicKey> keys = Map.of();
    private volatile Instant keysFetchedAt;

    @Autowired
    public PawapaySignatureVerifier(ObjectProvider<PawapayClient> client) {
        this(client, Clock.systemUTC());
    }

    PawapaySignatureVerifier(ObjectProvider<PawapayClient> client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    /** Vrai seulement quand la signature est presente, couvre le corps, n'a pas expire, et est juste. */
    public boolean verify(CallbackRequest request) {
        if (request == null || !request.signed()) {
            return false;
        }
        try {
            return check(request);
        } catch (Exception ex) {
            log.warn("Signature du rappel PawaPay non verifiable · {}", ex.getMessage());
            return false;
        }
    }

    private boolean check(CallbackRequest request) throws Exception {
        Map<String, String> inputs = members(request.header("signature-input"));
        if (inputs.isEmpty()) {
            log.warn("Rappel PawaPay · Signature-Input illisible");
            return false;
        }
        String label = inputs.containsKey(PawapayRequestSigner.LABEL) ? PawapayRequestSigner.LABEL : inputs.keySet().iterator().next();
        String params = inputs.get(label).trim();
        byte[] signature = signatureValue(request.header("signature"), label);
        if (signature == null) {
            log.warn("Rappel PawaPay · aucune signature sous le libelle {}", label);
            return false;
        }

        List<String> components = components(params);
        if (!components.contains("content-digest")) {
            log.warn("Rappel PawaPay · la signature ne couvre pas le corps (content-digest absent)");
            return false;
        }
        if (!digestMatches(request.header("content-digest"), request.body())) {
            log.warn("Rappel PawaPay · Content-Digest ne correspond pas au corps recu");
            return false;
        }
        Long expires = longParam(params, "expires");
        if (expires != null && clock.instant().isAfter(Instant.ofEpochSecond(expires).plus(CLOCK_SKEW))) {
            log.warn("Rappel PawaPay · signature expiree depuis {}", Instant.ofEpochSecond(expires));
            return false;
        }

        String algorithm = stringParam(params, "alg");
        List<PublicKey> candidates = keysFor(stringParam(params, "keyid"));
        if (candidates.isEmpty()) {
            log.warn("Rappel PawaPay · aucune cle publique PawaPay disponible pour verifier");
            return false;
        }
        for (String authority : request.authorities()) {
            String base = signatureBase(request, authority, components, params);
            if (base == null) {
                return false;
            }
            for (PublicKey key : candidates) {
                if (verifyBytes(algorithm, key, base.getBytes(StandardCharsets.UTF_8), signature)) {
                    return true;
                }
            }
        }
        log.warn("Rappel PawaPay · signature invalide (alg={}, keyid={}, autorites essayees={})",
                algorithm, stringParam(params, "keyid"), request.authorities());
        return false;
    }

    /** La base de signature RFC 9421 · une ligne par composant, sans saut final, {@code null} si un composant manque. */
    static String signatureBase(CallbackRequest request, String authority, List<String> components, String params) {
        StringBuilder base = new StringBuilder();
        for (String component : components) {
            String value = switch (component) {
                case "@method" -> request.method() == null ? null : request.method().toUpperCase(Locale.ROOT);
                case "@authority" -> authority == null ? null : authority.toLowerCase(Locale.ROOT);
                case "@path" -> request.path() == null || request.path().isEmpty() ? "/" : request.path();
                default -> component.startsWith("@") ? null : trimmed(request.header(component));
            };
            if (value == null) {
                log.warn("Rappel PawaPay · composant signe absent ou non pris en charge : {}", component);
                return null;
            }
            base.append('"').append(component).append("\": ").append(value).append('\n');
        }
        return base.append("\"@signature-params\": ").append(params).toString();
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    static Map<String, String> members(String header) {
        Map<String, String> found = new LinkedHashMap<>();
        if (header == null) {
            return found;
        }
        Matcher matcher = MEMBER.matcher(header);
        while (matcher.find()) {
            found.put(matcher.group(1), matcher.group(2));
        }
        return found;
    }

    static List<String> components(String params) {
        List<String> found = new ArrayList<>();
        Matcher matcher = COMPONENT.matcher(params.substring(0, params.indexOf(')') + 1));
        while (matcher.find()) {
            found.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return found;
    }

    private static byte[] signatureValue(String header, String label) {
        if (header == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?:^|,)\\s*" + Pattern.quote(label) + "=:([^:]*):").matcher(header);
        return matcher.find() ? Base64.getDecoder().decode(matcher.group(1).trim()) : null;
    }

    private static String stringParam(String params, String name) {
        Matcher matcher = Pattern.compile(";\\s*" + name + "=\"([^\"]*)\"").matcher(params);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Long longParam(String params, String name) {
        Matcher matcher = Pattern.compile(";\\s*" + name + "=(\\d+)").matcher(params);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    /** {@code sha-512=:<base64>:} ou {@code sha-256=:<base64>:} · l'un des condensats annonces doit correspondre. */
    static boolean digestMatches(String header, byte[] body) throws Exception {
        if (header == null) {
            return false;
        }
        Matcher matcher = Pattern.compile("(sha-512|sha-256)=:([^:]*):", Pattern.CASE_INSENSITIVE).matcher(header);
        boolean any = false;
        while (matcher.find()) {
            any = true;
            String jca = matcher.group(1).equalsIgnoreCase("sha-512") ? "SHA-512" : "SHA-256";
            byte[] expected = Base64.getDecoder().decode(matcher.group(2).trim());
            if (!MessageDigest.isEqual(expected, MessageDigest.getInstance(jca).digest(body))) {
                return false;
            }
        }
        return any;
    }

    static boolean verifyBytes(String algorithm, PublicKey key, byte[] data, byte[] signature) {
        String alg = algorithm == null ? defaultAlgorithm(key) : algorithm.trim().toLowerCase(Locale.ROOT);
        try {
            return switch (alg) {
                case "ecdsa-p256-sha256" -> ecdsa("SHA256withECDSA", key, data, signature, 64);
                case "ecdsa-p384-sha384" -> ecdsa("SHA384withECDSA", key, data, signature, 96);
                case "rsa-pss-sha512" -> {
                    Signature verifier = Signature.getInstance("RSASSA-PSS");
                    verifier.setParameter(new java.security.spec.PSSParameterSpec(
                            "SHA-512", "MGF1", java.security.spec.MGF1ParameterSpec.SHA512, 64, 1));
                    yield verify(verifier, key, data, signature);
                }
                case "rsa-v1_5-sha256" -> verify(Signature.getInstance("SHA256withRSA"), key, data, signature);
                default -> {
                    log.warn("Rappel PawaPay · algorithme de signature non pris en charge : {}", alg);
                    yield false;
                }
            };
        } catch (Exception ex) {
            // Une cle d'un autre type que l'algorithme annonce · ce n'est pas la bonne cle.
            return false;
        }
    }

    private static String defaultAlgorithm(PublicKey key) {
        return "RSA".equalsIgnoreCase(key.getAlgorithm()) ? "rsa-pss-sha512" : "ecdsa-p256-sha256";
    }

    /**
     * ECDSA · DER d'abord, comme le produisent Java et l'exemple Node de PawaPay ; puis la forme
     * brute r||s de la RFC 9421 (3.3.4), qu'un autre signataire pourrait envoyer.
     */
    private static boolean ecdsa(String jca, PublicKey key, byte[] data, byte[] signature, int rawLength) throws Exception {
        try {
            if (verify(Signature.getInstance(jca), key, data, signature)) {
                return true;
            }
        } catch (java.security.SignatureException notDer) {
            // essaie la forme brute
        }
        return signature.length == rawLength
                && verify(Signature.getInstance(jca + "inP1363Format"), key, data, signature);
    }

    private static boolean verify(Signature verifier, PublicKey key, byte[] data, byte[] signature) throws Exception {
        verifier.initVerify(key);
        verifier.update(data);
        return verifier.verify(signature);
    }

    // -----------------------------------------------------------------------------------------
    // Cles publiques de PawaPay
    // -----------------------------------------------------------------------------------------

    /**
     * La cle annoncee par {@code keyid}, ou toutes les cles de PawaPay si elle n'y figure pas.
     *
     * <p>La documentation de PawaPay ne dit pas comment {@code keyid} se rapporte aux identifiants
     * de ses cles publiques · son exemple de rappel porte meme un identifiant cote marchand. Toutes
     * les cles candidates sont celles de PawaPay, essayer chacune ne donne rien a un faussaire.</p>
     */
    private List<PublicKey> keysFor(String keyId) {
        Map<String, PublicKey> current = keys;
        if (keyId != null && current.containsKey(keyId)) {
            return List.of(current.get(keyId));
        }
        if (current.isEmpty() || keysFetchedAt == null
                || keysFetchedAt.isBefore(clock.instant().minus(KEY_REFRESH_INTERVAL))) {
            current = refreshKeys();
        }
        if (keyId != null && current.containsKey(keyId)) {
            return List.of(current.get(keyId));
        }
        return List.copyOf(current.values());
    }

    private synchronized Map<String, PublicKey> refreshKeys() {
        PawapayClient pawapay = client.getIfAvailable();
        if (pawapay == null) {
            return keys;
        }
        try {
            Map<String, PublicKey> loaded = new LinkedHashMap<>();
            for (PublicKeyEntry entry : pawapay.publicKeys()) {
                if (entry != null && StringUtils.hasText(entry.key())) {
                    loaded.put(entry.id() == null ? "key-" + loaded.size() : entry.id(), parsePublicKey(entry.key()));
                }
            }
            keys = Map.copyOf(loaded);
            keysFetchedAt = clock.instant();
            log.info("PawaPay · {} cle(s) publique(s) de verification des rappels chargee(s) : {}", loaded.size(), loaded.keySet());
        } catch (Exception ex) {
            // On garde les cles deja connues · une panne passagere ne doit pas tout faire relire.
            keysFetchedAt = clock.instant();
            log.warn("PawaPay · cles publiques de verification indisponibles : {}", ex.getMessage());
        }
        return keys;
    }

    /** Pour les tests · des cles connues sans appel reseau. */
    void useKeys(Map<String, PublicKey> known) {
        keys = Map.copyOf(known);
        keysFetchedAt = clock.instant();
    }

    static PublicKey parsePublicKey(String pem) throws Exception {
        String value = pem.trim().replace("\\n", "\n");
        try (PEMParser parser = new PEMParser(new StringReader(value))) {
            Object parsed = parser.readObject();
            if (parsed instanceof SubjectPublicKeyInfo info) {
                return new JcaPEMKeyConverter().getPublicKey(info);
            }
            throw new IllegalStateException("Cle publique PawaPay illisible ("
                    + (parsed == null ? "aucun objet" : parsed.getClass().getSimpleName()) + ")");
        }
    }
}
