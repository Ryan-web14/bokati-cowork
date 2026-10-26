package com.sni.bokaticowork.features.payment.provider.pawaypay;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Les rappels signes par PawaPay se reconnaissent · et rien d'autre ne passe pour signe.
 *
 * <p>Les rappels sont signes ici comme PawaPay les signe (meme base RFC 9421, meme libelle
 * {@code sig-pp}), avec des cles generees en memoire a chaque execution · aucune cle dans le depot.</p>
 */
class PawapaySignatureVerifierTest {

    private static final URI CALLBACK = URI.create("https://app.elleaose.com/sni/api/v1/payments/mobile-money/pawapay/callback");
    private static final String BODY = "{\"depositId\":\"dep-1\",\"status\":\"COMPLETED\",\"amount\":\"10\",\"currency\":\"XAF\"}";
    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");

    private static KeyPair pawapayEc;
    private static KeyPair pawapayRsa;
    private static KeyPair stranger;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(new ECGenParameterSpec("secp256r1"));
        pawapayEc = ec.generateKeyPair();
        stranger = ec.generateKeyPair();
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(2048);
        pawapayRsa = rsa.generateKeyPair();
    }

    @SuppressWarnings("unchecked")
    private PawapaySignatureVerifier verifier(Map<String, java.security.PublicKey> known, Instant now) {
        PawapaySignatureVerifier verifier = new PawapaySignatureVerifier(mock(ObjectProvider.class),
                Clock.fixed(now, ZoneOffset.UTC));
        verifier.useKeys(known);
        return verifier;
    }

    private PawapaySignatureVerifier verifier() {
        return verifier(Map.of("HTTP_EC_P256_KEY:1", pawapayEc.getPublic(), "HTTP_RSA_KEY:1", pawapayRsa.getPublic()), NOW);
    }

    /** Un rappel signe comme PawaPay le fait. */
    private static Map<String, String> signedHeaders(KeyPair pair, PawapayRequestSigner.Algorithm algorithm, String keyId, String body) {
        PawapayRequestSigner signer = new PawapayRequestSigner(pair.getPrivate(), keyId, algorithm,
                Clock.fixed(NOW, ZoneOffset.UTC));
        Map<String, String> headers = new LinkedHashMap<>(signer.sign("POST", CALLBACK, "application/json",
                body.getBytes(StandardCharsets.UTF_8)));
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private static PawapaySignatureVerifier.CallbackRequest request(Map<String, String> headers, String body, String... authorities) {
        return new PawapaySignatureVerifier.CallbackRequest("POST", List.of(authorities), CALLBACK.getRawPath(),
                headers, body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Un rappel signe ECDSA P-256 par PawaPay est reconnu")
    void acceptsAnEcdsaSignedCallback() {
        Map<String, String> headers = signedHeaders(pawapayEc, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        assertThat(verifier().verify(request(headers, BODY, "app.elleaose.com"))).isTrue();
    }

    @Test
    @DisplayName("Un rappel signe RSA-PSS est reconnu, meme sous un keyid qui n'est pas celui de la cle")
    void acceptsAnRsaPssSignedCallbackUnderAnUnknownKeyId() {
        Map<String, String> headers = signedHeaders(pawapayRsa, PawapayRequestSigner.Algorithm.RSA_PSS_SHA512, "CUSTOMER_TEST_KEY", BODY);
        assertThat(verifier().verify(request(headers, BODY, "app.elleaose.com"))).isTrue();
    }

    @Test
    @DisplayName("Un corps modifie apres signature est refuse")
    void rejectsATamperedBody() {
        Map<String, String> headers = signedHeaders(pawapayEc, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        String tampered = BODY.replace("\"10\"", "\"50000\"");
        assertThat(verifier().verify(request(headers, tampered, "app.elleaose.com"))).isFalse();
    }

    @Test
    @DisplayName("Un condensat recalcule pour un corps modifie ne suffit pas · la signature le couvre")
    void rejectsARecomputedDigest() throws Exception {
        Map<String, String> headers = signedHeaders(pawapayEc, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        String tampered = BODY.replace("\"10\"", "\"50000\"");
        headers.put("Content-Digest", "sha-512=:" + Base64.getEncoder().encodeToString(
                java.security.MessageDigest.getInstance("SHA-512").digest(tampered.getBytes(StandardCharsets.UTF_8))) + ":");
        assertThat(verifier().verify(request(headers, tampered, "app.elleaose.com"))).isFalse();
    }

    @Test
    @DisplayName("Une signature d'une cle qui n'est pas a PawaPay est refusee")
    void rejectsAStrangerKey() {
        Map<String, String> headers = signedHeaders(stranger, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        assertThat(verifier().verify(request(headers, BODY, "app.elleaose.com"))).isFalse();
    }

    @Test
    @DisplayName("Une signature expiree est refusee · au-dela de cinq minutes de tolerance")
    void rejectsAnExpiredSignature() {
        Map<String, String> headers = signedHeaders(pawapayEc, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        PawapaySignatureVerifier late = verifier(Map.of("k", pawapayEc.getPublic()), NOW.plusSeconds(60 + 301));
        PawapaySignatureVerifier slightlyLate = verifier(Map.of("k", pawapayEc.getPublic()), NOW.plusSeconds(60 + 120));
        assertThat(late.verify(request(headers, BODY, "app.elleaose.com"))).isFalse();
        assertThat(slightlyLate.verify(request(headers, BODY, "app.elleaose.com"))).isTrue();
    }

    @Test
    @DisplayName("Derriere un proxy, la bonne autorite parmi celles essayees suffit")
    void triesEachAuthority() {
        Map<String, String> headers = signedHeaders(pawapayEc, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        assertThat(verifier().verify(request(headers, BODY, "bokati-cowork.herokuapp.com", "app.elleaose.com"))).isTrue();
        assertThat(verifier().verify(request(headers, BODY, "bokati-cowork.herokuapp.com"))).isFalse();
    }

    @Test
    @DisplayName("ECDSA au format brut r||s de la RFC 9421 est accepte aussi")
    void acceptsRawEcdsaSignatures() throws Exception {
        Map<String, String> headers = signedHeaders(pawapayEc, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        String params = headers.get("Signature-Input").substring("sig-pp=".length());
        String base = "\"@method\": POST\n\"@authority\": app.elleaose.com\n\"@path\": " + CALLBACK.getRawPath() + "\n"
                + "\"signature-date\": " + headers.get("Signature-Date") + "\n"
                + "\"content-digest\": " + headers.get("Content-Digest") + "\n"
                + "\"content-type\": application/json\n"
                + "\"@signature-params\": " + params;
        Signature raw = Signature.getInstance("SHA256withECDSAinP1363Format");
        raw.initSign(pawapayEc.getPrivate());
        raw.update(base.getBytes(StandardCharsets.UTF_8));
        headers.put("Signature", "sig-pp=:" + Base64.getEncoder().encodeToString(raw.sign()) + ":");
        assertThat(verifier().verify(request(headers, BODY, "app.elleaose.com"))).isTrue();
    }

    @Test
    @DisplayName("Sans signature, ou sans cle PawaPay connue, rien n'est repute signe")
    void unsignedOrKeylessIsNotTrusted() {
        assertThat(verifier().verify(request(Map.of("Content-Type", "application/json"), BODY, "app.elleaose.com"))).isFalse();
        Map<String, String> headers = signedHeaders(pawapayEc, PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, "HTTP_EC_P256_KEY:1", BODY);
        assertThat(verifier(Map.of(), NOW).verify(request(headers, BODY, "app.elleaose.com"))).isFalse();
    }
}
