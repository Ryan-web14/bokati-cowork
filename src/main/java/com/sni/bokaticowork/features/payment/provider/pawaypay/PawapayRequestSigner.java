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
     * Lit la cle privee PEM · le format d'OpenSSL ({@code EC PRIVATE KEY}, celui de
     * {@code openssl ecparam -genkey}) comme le PKCS#8 ({@code PRIVATE KEY}).
     *
     * <p>Les sauts de ligne ecrits {@code \n} sont acceptes · certaines interfaces de configuration
     * ne gardent pas les vrais sauts de ligne d'une variable.</p>
     */
    static PrivateKey parsePrivateKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("La cle privee de signature PawaPay est vide "
                    + "(bokati.payment.pawaypay.signing.private-key)");
        }
        String normalized = pem.trim().replace("\\n", "\n");
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
            throw new IllegalStateException("La cle de signature PawaPay n'est pas une cle privee PEM lisible");
        } catch (IOException ex) {
            throw new IllegalStateException("La cle de signature PawaPay est illisible", ex);
        }
    }
}
