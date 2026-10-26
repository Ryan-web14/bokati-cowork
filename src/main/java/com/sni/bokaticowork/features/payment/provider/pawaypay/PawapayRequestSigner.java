package com.sni.bokaticowork.features.payment.provider.pawaypay;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Signe les requetes envoyees a PawaPay, selon la RFC 9421 telle que PawaPay l'applique.
 *
 * <p>Une fois la cle publique deposee et les appels signes exiges dans le tableau de bord, PawaPay
 * refuse toute demande financiere (depot, remboursement, versement) qui n'est pas signee par la cle
 * privee correspondante. Une cle API volee ne suffit alors plus a deplacer de l'argent.</p>
 *
 * <p>Ce qui est signe, et dans quel ordre, suit l'exemple officiel de PawaPay
 * ({@code pawaPay/signatures-node-example}) :</p>
 * <ul>
 *   <li>{@code Content-Digest} · SHA-512 des octets exacts du corps, {@code sha-512=:<base64>:} ;</li>
 *   <li>{@code Signature-Date} · l'instant de la signature, ISO 8601 en UTC avec millisecondes ;</li>
 *   <li>la base de signature couvre {@code @method}, {@code @authority}, {@code @path},
 *       {@code signature-date}, {@code content-digest} et {@code content-type} ;</li>
 *   <li>{@code Signature-Input} et {@code Signature} sous le libelle {@code sig-pp} ; la signature
 *       ECDSA est au format DER, celui que produisent Java et Node.</li>
 * </ul>
 *
 * <p>La cle privee n'est jamais journalisee ni renvoyee · elle vient de l'environnement seulement.</p>
 */
public class PawapayRequestSigner {

    static final String LABEL = "sig-pp";
    static final long VALIDITY_SECONDS = 60;

    private static final DateTimeFormatter SIGNATURE_DATE = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
            .withZone(java.time.ZoneOffset.UTC);

    /** Les algorithmes que PawaPay accepte, avec leur nom JCA. */
    enum Algorithm {
        ECDSA_P256_SHA256("ecdsa-p256-sha256", "SHA256withECDSA"),
        ECDSA_P384_SHA384("ecdsa-p384-sha384", "SHA384withECDSA"),
        /** RSASSA-PSS, SHA-512 pour le condensat et pour MGF1, sel de 64 octets (RFC 9421 3.3.1). */
        RSA_PSS_SHA512("rsa-pss-sha512", "RSASSA-PSS"),
        RSA_V1_5_SHA256("rsa-v1_5-sha256", "SHA256withRSA");

        final String httpName;
        final String jcaName;

        Algorithm(String httpName, String jcaName) {
            this.httpName = httpName;
            this.jcaName = jcaName;
        }

        static Algorithm fromHttpName(String name) {
            for (Algorithm algorithm : values()) {
                if (algorithm.httpName.equalsIgnoreCase(name == null ? "" : name.trim())) {
                    return algorithm;
                }
            }
            throw new IllegalStateException("Algorithme de signature PawaPay non pris en charge : " + name
                    + " (attendu : ecdsa-p256-sha256, ecdsa-p384-sha384, rsa-pss-sha512 ou rsa-v1_5-sha256)");
        }
    }

    private final PrivateKey privateKey;
    private final String keyId;
    private final Algorithm algorithm;
    private final Clock clock;

    public PawapayRequestSigner(String privateKeyPem, String keyId, String algorithm) {
        this(parsePrivateKey(privateKeyPem), keyId, algorithm);
    }

    private PawapayRequestSigner(PrivateKey key, String keyId, String algorithm) {
        this(key, keyId, resolveAlgorithm(key, algorithm), Clock.systemUTC());
    }

    /**
     * L'algorithme configure, ou celui qui convient a la cle quand rien n'est configure.
     *
     * <p>Une cle RSA annoncee en ECDSA serait refusee par PawaPay sans explication · on le dit au
     * demarrage plutot qu'au premier paiement.</p>
     */
    static Algorithm resolveAlgorithm(PrivateKey key, String configured) {
        boolean rsa = "RSA".equalsIgnoreCase(key.getAlgorithm());
        if (configured == null || configured.isBlank()) {
            return rsa ? Algorithm.RSA_PSS_SHA512 : Algorithm.ECDSA_P256_SHA256;
        }
        Algorithm algorithm = Algorithm.fromHttpName(configured);
        boolean rsaAlgorithm = algorithm == Algorithm.RSA_PSS_SHA512 || algorithm == Algorithm.RSA_V1_5_SHA256;
        if (rsa != rsaAlgorithm) {
            throw new IllegalStateException("La cle de signature PawaPay est " + key.getAlgorithm()
                    + " mais l'algorithme configure est " + algorithm.httpName);
        }
        return algorithm;
    }

    PawapayRequestSigner(PrivateKey privateKey, String keyId, Algorithm algorithm, Clock clock) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("L'identifiant de la cle de signature PawaPay est vide "
                    + "(bokati.payment.pawaypay.signing.key-id)");
        }
        this.privateKey = privateKey;
        this.keyId = keyId.trim();
        this.algorithm = algorithm;
        this.clock = clock;
    }

    /**
     * Les quatre en-tetes a ajouter a la requete.
     *
     * @param contentType le {@code Content-Type} tel qu'il part · il est couvert par la signature
     */
    public Map<String, String> sign(String method, URI uri, String contentType, byte[] body) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        long created = now.getEpochSecond();
        String signatureDate = SIGNATURE_DATE.format(now);
        String contentDigest = "sha-512=:" + base64(digest(body == null ? new byte[0] : body)) + ":";
        String type = contentType == null ? "" : contentType;

        String params = "(\"@method\" \"@authority\" \"@path\" \"signature-date\" \"content-digest\" \"content-type\")"
                + ";alg=\"" + algorithm.httpName + "\""
                + ";keyid=\"" + keyId + "\""
                + ";created=" + created
                + ";expires=" + (created + VALIDITY_SECONDS);

        String base = signatureBase(method, uri, signatureDate, contentDigest, type, params);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Digest", contentDigest);
        headers.put("Signature-Date", signatureDate);
        headers.put("Signature-Input", LABEL + "=" + params);
        headers.put("Signature", LABEL + "=:" + base64(signBytes(base.getBytes(StandardCharsets.UTF_8))) + ":");
        return headers;
    }

    /** La base de signature RFC 9421 · une ligne par composant, sans saut final. */
    static String signatureBase(String method, URI uri, String signatureDate, String contentDigest,
                                String contentType, String params) {
        return "\"@method\": " + method.toUpperCase(Locale.ROOT) + "\n"
                + "\"@authority\": " + authority(uri) + "\n"
                + "\"@path\": " + path(uri) + "\n"
                + "\"signature-date\": " + signatureDate + "\n"
                + "\"content-digest\": " + contentDigest + "\n"
                + "\"content-type\": " + contentType + "\n"
                + "\"@signature-params\": " + params;
    }

    /** L'hote en minuscules, et le port seulement s'il n'est pas celui du schema (RFC 9421 2.2.3). */
    static String authority(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443)
                || ("http".equalsIgnoreCase(uri.getScheme()) && port == 80);
        return defaultPort ? host : host + ":" + port;
    }

    /** Le chemin tel qu'il part, sans la requete · {@code /} pour un chemin vide (RFC 9421 2.2.6). */
    static String path(URI uri) {
        String path = uri.getRawPath();
        return path == null || path.isEmpty() ? "/" : path;
    }

    private byte[] signBytes(byte[] data) {
        try {
            Signature signature = Signature.getInstance(algorithm.jcaName);
            if (algorithm == Algorithm.RSA_PSS_SHA512) {
                signature.setParameter(new java.security.spec.PSSParameterSpec(
                        "SHA-512", "MGF1", java.security.spec.MGF1ParameterSpec.SHA512, 64, 1));
            }
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Signature PawaPay impossible", ex);
        }
    }

    private static byte[] digest(byte[] body) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(body);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-512 indisponible", ex);
        }
    }

    private static String base64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    /**
     * Lit la cle privee PEM · le format d'OpenSSL ({@code EC PRIVATE KEY}, {@code RSA PRIVATE KEY})
     * comme le PKCS#8 ({@code PRIVATE KEY}).
     *
     * <p>Une variable d'environnement ne garde pas toujours la cle telle qu'elle a ete generee.
     * Selon l'outil qui l'a recue (tableau de bord Heroku, fichier .env, CLI), on retrouve des sauts
     * de ligne ecrits antislash-n, des guillemets autour, la cle aplatie sur une seule ligne avec des
     * espaces, la cle sans ses lignes BEGIN et END, ou le PEM entier encode en base64. Tous ces cas
     * sont ramenes a un PEM propre avant lecture ; le message d'erreur dit ce qui a ete vu, jamais
     * le contenu de la cle.</p>
     */
    static PrivateKey parsePrivateKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("La cle privee de signature PawaPay est vide "
                    + "(bokati.payment.pawaypay.signing.private-key)");
        }
        String normalized = normalizePem(pem);
        try (PEMParser parser = new PEMParser(new StringReader(normalized))) {
            Object parsed = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (parsed instanceof PEMKeyPair pair) {
                PrivateKeyInfo info = pair.getPrivateKeyInfo();
                // Une cle EC au format OpenSSL peut ne porter la courbe que dans sa partie publique ·
                // on la reprend la, sans quoi la conversion echoue sur des parametres absents.
                if (info.getPrivateKeyAlgorithm().getParameters() == null && pair.getPublicKeyInfo() != null) {
                    info = new PrivateKeyInfo(pair.getPublicKeyInfo().getAlgorithm(), info.parsePrivateKey());
                }
                return converter.getPrivateKey(info);
            }
            if (parsed instanceof PrivateKeyInfo info) {
                return converter.getPrivateKey(info);
            }
            throw new IllegalStateException("La cle de signature PawaPay n'est pas une cle privee PEM lisible ("
                    + describe(normalized, parsed) + ")");
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof IllegalStateException state && state.getMessage() != null
                    && state.getMessage().startsWith("La cle de signature PawaPay")) {
                throw state;
            }
            throw new IllegalStateException("La cle de signature PawaPay est illisible ("
                    + describe(normalized, null) + ")", ex);
        }
    }

    /** Ramene les formes abimees d'une cle PEM a un PEM de 64 colonnes, en-tete compris. */
    static String normalizePem(String raw) {
        String value = raw.trim();
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1).trim();
        }
        value = value.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\r\n", "\n").replace('\r', '\n');

        // Le PEM entier encode en base64 · on le decode puis on recommence.
        if (!value.contains("-----BEGIN")) {
            // Une ligne END restee seule apres qu'on a retire la ligne BEGIN dit encore le format.
            java.util.regex.Matcher end = java.util.regex.Pattern.compile("-----END ([A-Z0-9 ]+)-----").matcher(value);
            String declared = end.find() ? end.group(1).trim() : null;
            String compact = value.replaceAll("-----END [A-Z0-9 ]+-----", "").replaceAll("\\s", "");
            byte[] der;
            try {
                der = Base64.getMimeDecoder().decode(compact);
            } catch (IllegalArgumentException notBase64) {
                return value;
            }
            String decoded = new String(der, StandardCharsets.US_ASCII);
            if (decoded.contains("-----BEGIN")) {
                return normalizePem(decoded);
            }
            return wrap(declared != null ? declared : detectDerType(der), compact);
        }

        // En-tete present, mais lignes possiblement aplaties ou espacees · on reconstruit.
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("-----BEGIN ([A-Z0-9 ]+)-----(.*?)-----END \\1-----",java.util.regex.Pattern.DOTALL)
                .matcher(value);
        if (!matcher.find()) {
            return value;
        }
        String type = matcher.group(1).trim();
        String body = matcher.group(2);
        // Un PEM chiffre porte des en-tetes Proc-Type / DEK-Info · on le laisse tel quel, il echouera
        // avec un message clair plutot que d'etre corrompu.
        if (body.contains(":")) {
            return value;
        }
        return wrap(type, body.replaceAll("\\s", ""));
    }

    /**
     * Le format d'une cle reduite a son corps · PKCS#8 ({@code PRIVATE KEY}), RSA PKCS#1
     * ({@code RSA PRIVATE KEY}, celui d'« openssl genrsa ») ou EC SEC1 ({@code EC PRIVATE KEY}).
     */
    static String detectDerType(byte[] der) {
        try {
            org.bouncycastle.asn1.ASN1Sequence sequence = org.bouncycastle.asn1.ASN1Sequence.getInstance(der);
            if (sequence.size() >= 3 && sequence.getObjectAt(1) instanceof org.bouncycastle.asn1.ASN1Sequence) {
                return "PRIVATE KEY";
            }
            if (sequence.size() >= 9) {
                return "RSA PRIVATE KEY";
            }
            return "EC PRIVATE KEY";
        } catch (RuntimeException notAsn1) {
            return "PRIVATE KEY";
        }
    }

    private static String wrap(String type, String base64Body) {
        StringBuilder out = new StringBuilder("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64Body.length(); i += 64) {
            out.append(base64Body, i, Math.min(base64Body.length(), i + 64)).append('\n');
        }
        return out.append("-----END ").append(type).append("-----\n").toString();
    }

    /** Ce qu'on peut dire d'une cle illisible sans jamais en reveler le contenu. */
    private static String describe(String normalized, Object parsed) {
        // Seul le nom de l'en-tete est repris · jamais une ligne entiere, qui peut contenir la cle
        // quand celle-ci a ete aplatie sur une seule ligne.
        java.util.regex.Matcher begin = java.util.regex.Pattern.compile("-----BEGIN ([A-Z0-9 ]{1,40})-----").matcher(normalized);
        String header = begin.find() ? "BEGIN " + begin.group(1) : "absent";
        String kind = parsed == null ? "aucun objet reconnu" : parsed.getClass().getSimpleName();
        boolean encrypted = normalized.contains("ENCRYPTED") || normalized.contains("Proc-Type");
        return "en-tete \"" + header + "\", "
                + kind + (encrypted ? ", cle chiffree par mot de passe : fournir la cle en clair" : "")
                + ", " + normalized.length() + " caracteres apres normalisation";
    }
}
