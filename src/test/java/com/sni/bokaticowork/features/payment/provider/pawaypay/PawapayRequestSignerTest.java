package com.sni.bokaticowork.features.payment.provider.pawaypay;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.http.HttpStatus;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La signature produite se verifie avec la cle publique · exactement ce que fera PawaPay.
 */
class PawapayRequestSignerTest {

    private static final URI DEPOSITS = URI.create("https://api.pawapay.io/v2/deposits");
    private static final byte[] BODY = "{\"depositId\":\"d-1\",\"amount\":\"10\",\"currency\":\"XAF\"}"
            .getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.parse("2026-10-26T09:30:15.123Z");

    private static KeyPair keys;
    private static String opensslPem;

    @BeforeAll
    static void generateKeys() throws Exception {
        // Une paire generee en memoire, a chaque execution · aucune cle privee dans le depot.
        // Le fournisseur BouncyCastle ecrit le meme PEM qu'« openssl ecparam -name P-256 -genkey »
        // (BEGIN EC PRIVATE KEY, courbe nommee incluse), la commande que PawaPay indique.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC",
                new org.bouncycastle.jce.provider.BouncyCastleProvider());
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keys = generator.generateKeyPair();
        StringWriter out = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
            writer.writeObject(keys);
        }
        opensslPem = out.toString();
    }

    private PawapayRequestSigner signer() {
        return new PawapayRequestSigner(PawapayRequestSigner.parsePrivateKey(opensslPem), "BOKATI_KEY_1",
                PawapayRequestSigner.Algorithm.ECDSA_P256_SHA256, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("La cle au format OpenSSL (EC PRIVATE KEY) se lit, y compris avec des \\n ecrits")
    void readsOpensslPem() {
        assertThat(opensslPem).contains("BEGIN EC PRIVATE KEY");
        assertThat(PawapayRequestSigner.parsePrivateKey(opensslPem).getAlgorithm()).isEqualTo("EC");
        assertThat(PawapayRequestSigner.parsePrivateKey(opensslPem.replace("\n", "\\n")).getAlgorithm())
                .isEqualTo("EC");
    }

    @Test
    @DisplayName("Les quatre en-tetes ont la forme attendue par PawaPay")
    void headersHaveThePawapayShape() throws Exception {
        Map<String, String> headers = signer().sign("POST", DEPOSITS, "application/json", BODY);

        String digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(BODY));
        assertThat(headers.get("Content-Digest")).isEqualTo("sha-512=:" + digest + ":");
        assertThat(headers.get("Signature-Date")).isEqualTo("2026-10-26T09:30:15.123Z");
        assertThat(headers.get("Signature-Input")).isEqualTo(
                "sig-pp=(\"@method\" \"@authority\" \"@path\" \"signature-date\" \"content-digest\" \"content-type\")"
                        + ";alg=\"ecdsa-p256-sha256\";keyid=\"BOKATI_KEY_1\";created=" + NOW.getEpochSecond()
                        + ";expires=" + (NOW.getEpochSecond() + 60));
        assertThat(headers.get("Signature")).startsWith("sig-pp=:").endsWith(":");
    }

    @Test
    @DisplayName("La signature se verifie avec la cle publique sur la base RFC 9421 reconstruite")
    void signatureVerifiesWithThePublicKey() throws Exception {
        Map<String, String> headers = signer().sign("POST", DEPOSITS, "application/json", BODY);
        String params = headers.get("Signature-Input").substring("sig-pp=".length());
        String base = "\"@method\": POST\n"
                + "\"@authority\": api.pawapay.io\n"
                + "\"@path\": /v2/deposits\n"
                + "\"signature-date\": " + headers.get("Signature-Date") + "\n"
                + "\"content-digest\": " + headers.get("Content-Digest") + "\n"
                + "\"content-type\": application/json\n"
                + "\"@signature-params\": " + params;
        String value = headers.get("Signature");
        byte[] signature = Base64.getDecoder().decode(value.substring("sig-pp=:".length(), value.length() - 1));

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(keys.getPublic());
        verifier.update(base.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(signature)).isTrue();
    }

    @Test
    @DisplayName("Un corps modifie apres signature ne correspond plus a l'empreinte")
    void tamperedBodyChangesTheDigest() {
        Map<String, String> original = signer().sign("POST", DEPOSITS, "application/json", BODY);
        Map<String, String> tampered = signer().sign("POST", DEPOSITS, "application/json",
                "{\"amount\":\"100000\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(tampered.get("Content-Digest")).isNotEqualTo(original.get("Content-Digest"));
    }

    @Test
    @DisplayName("L'autorite ne garde le port que s'il n'est pas celui du schema")
    void authorityAndPath() {
        assertThat(PawapayRequestSigner.authority(URI.create("https://API.pawapay.io:443/v2/refunds"))).isEqualTo("api.pawapay.io");
        assertThat(PawapayRequestSigner.authority(URI.create("http://localhost:8080/x"))).isEqualTo("localhost:8080");
        assertThat(PawapayRequestSigner.path(URI.create("https://api.pawapay.io/v2/refunds?x=1"))).isEqualTo("/v2/refunds");
    }

    @Test
    @DisplayName("Sans identifiant de cle, ou avec une cle illisible, l'application le dit au demarrage")
    void misconfigurationIsExplicit() {
        assertThatThrownBy(() -> new PawapayRequestSigner(opensslPem, " ", "ecdsa-p256-sha256"))
                .hasMessageContaining("key-id");
        assertThatThrownBy(() -> PawapayRequestSigner.parsePrivateKey("pas une cle"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PawapayRequestSigner(opensslPem, "K", "hmac-sha256"))
                .hasMessageContaining("non pris en charge");
    }

    @Test
    @DisplayName("Une cle RSA signe en RSASSA-PSS SHA-512, et se verifie avec sa cle publique")
    void rsaKeySignsWithPss() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair rsa = generator.generateKeyPair();
        StringWriter out = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
            writer.writeObject(rsa);
        }
        assertThat(out.toString()).contains("BEGIN RSA PRIVATE KEY");
        java.security.PrivateKey key = PawapayRequestSigner.parsePrivateKey(out.toString());
        assertThat(PawapayRequestSigner.resolveAlgorithm(key, null)).isEqualTo(PawapayRequestSigner.Algorithm.RSA_PSS_SHA512);
        assertThatThrownBy(() -> PawapayRequestSigner.resolveAlgorithm(key, "ecdsa-p256-sha256"))
                .hasMessageContaining("RSA");

        PawapayRequestSigner signer = new PawapayRequestSigner(key, "K", PawapayRequestSigner.Algorithm.RSA_PSS_SHA512,
                Clock.fixed(NOW, ZoneOffset.UTC));
        Map<String, String> headers = signer.sign("POST", DEPOSITS, "application/json", BODY);

        assertThat(headers.get("Signature-Input")).contains("alg=\"rsa-pss-sha512\"");
        assertThat(verifies(headers, "RSASSA-PSS", new java.security.spec.PSSParameterSpec(
                "SHA-512", "MGF1", java.security.spec.MGF1ParameterSpec.SHA512, 64, 1), rsa.getPublic())).isTrue();
    }

    private static boolean verifies(Map<String, String> headers, String jca, java.security.spec.AlgorithmParameterSpec spec,
                                    java.security.PublicKey publicKey) throws Exception {
        String params = headers.get("Signature-Input").substring("sig-pp=".length());
        String base = PawapayRequestSigner.signatureBase("POST", DEPOSITS, headers.get("Signature-Date"),
                headers.get("Content-Digest"), "application/json", params);
        String value = headers.get("Signature");
        Signature verifier = Signature.getInstance(jca);
        if (spec != null) {
            verifier.setParameter(spec);
        }
        verifier.initVerify(publicKey);
        verifier.update(base.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(value.substring("sig-pp=:".length(), value.length() - 1)));
    }

    private static java.security.PublicKey parsePublic(String pem) throws Exception {
        try (org.bouncycastle.openssl.PEMParser parser = new org.bouncycastle.openssl.PEMParser(new java.io.StringReader(pem))) {
            return new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                    .getPublicKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) parser.readObject());
        }
    }

    @Test
    @DisplayName("L'intercepteur signe un POST avec ses octets exacts, et laisse un GET tel quel")
    void interceptorSignsPostsOnly() throws Exception {
        PawapaySigningInterceptor interceptor = new PawapaySigningInterceptor(signer());
        ClientHttpRequestExecution execution = (request, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK);

        MockClientHttpRequest post = new MockClientHttpRequest(HttpMethod.POST, DEPOSITS);
        post.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        interceptor.intercept(post, BODY, execution);
        assertThat(post.getHeaders().getFirst("Signature")).startsWith("sig-pp=:");
        assertThat(post.getHeaders().getFirst("Content-Digest")).startsWith("sha-512=:");

        MockClientHttpRequest get = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.pawapay.io/v2/deposits/d-1"));
        interceptor.intercept(get, new byte[0], execution);
        assertThat(get.getHeaders().containsHeader("Signature")).isFalse();
    }
}
