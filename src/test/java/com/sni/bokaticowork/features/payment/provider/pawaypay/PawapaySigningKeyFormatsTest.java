package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositRequest;
import com.sun.net.httpserver.HttpServer;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La production est tombee au demarrage · la cle de signature collee dans Heroku ne se lisait pas
 * comme un PEM, et la creation du client PawaPay emportait toute l'application.
 *
 * <p>Une variable d'environnement abime volontiers une cle multi-ligne. Ces formes-la se lisent
 * desormais ; une cle vraiment illisible ne bloque plus le demarrage et n'envoie jamais d'appel
 * financier non signe.</p>
 */
class PawapaySigningKeyFormatsTest {

    private static KeyPair rsa;
    private static String pkcs1Pem;
    private static String pkcs8Pem;

    @BeforeAll
    static void generateKeys() throws Exception {
        // Generee en memoire a chaque execution · aucune cle privee dans le depot.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                new org.bouncycastle.jce.provider.BouncyCastleProvider());
        generator.initialize(2048);
        rsa = generator.generateKeyPair();
        pkcs1Pem = pem(rsa);
        pkcs8Pem = pem(new JcaPKCS8Generator(rsa.getPrivate(), null));
    }

    private static String pem(Object value) throws Exception {
        StringWriter out = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
            writer.writeObject(value);
        }
        return out.toString();
    }

    private static void assertSameKey(String variable) {
        PrivateKey key = PawapayRequestSigner.parsePrivateKey(variable);
        assertThat(key.getEncoded()).isEqualTo(rsa.getPrivate().getEncoded());
    }

    @Test
    @DisplayName("PKCS#1 et PKCS#8 tels que generes")
    void cleanPem() {
        assertSameKey(pkcs1Pem);
        assertSameKey(pkcs8Pem);
    }

    @Test
    @DisplayName("Cle aplatie sur une ligne, sauts de ligne remplaces par des espaces")
    void flattenedWithSpaces() {
        assertSameKey(pkcs1Pem.replace("\r\n", " ").replace("\n", " "));
        assertSameKey(pkcs8Pem.replace("\n", " "));
    }

    @Test
    @DisplayName("Sauts de ligne ecrits antislash-n, guillemets autour")
    void escapedNewlinesAndQuotes() {
        assertSameKey("\"" + pkcs1Pem.replace("\n", "\\n") + "\"");
        assertSameKey("'" + pkcs8Pem.replace("\r\n", "\\r\\n").replace("\n", "\\n") + "'");
    }

    @Test
    @DisplayName("Fins de ligne Windows")
    void crlf() {
        assertSameKey(pkcs8Pem.replace("\n", "\r\n"));
    }

    @Test
    @DisplayName("Corps PKCS#8 sans lignes BEGIN et END")
    void bodyWithoutHeader() {
        assertSameKey(Base64.getEncoder().encodeToString(rsa.getPrivate().getEncoded()));
    }

    @Test
    @DisplayName("Cle RSA PKCS#1 sans ligne BEGIN, avec ou sans sa ligne END")
    void pkcs1WithoutBegin() {
        String withoutBegin = pkcs1Pem.substring(pkcs1Pem.indexOf('\n') + 1);
        assertSameKey(withoutBegin);
        String bodyOnly = withoutBegin.replaceAll("-----END RSA PRIVATE KEY-----", "");
        assertSameKey(bodyOnly);
        assertSameKey(bodyOnly.replace("\n", " "));
    }

    @Test
    @DisplayName("Le message d'une cle aplatie illisible ne recopie pas la cle")
    void flattenedUnreadableKeyDoesNotLeak() {
        String broken = "-----BEGIN RSA PRIVATE KEY----- AAAAsecretBBBB -----END RSA PRIVATE KEY-----";
        assertThatThrownBy(() -> PawapayRequestSigner.parsePrivateKey(broken))
                .isInstanceOf(IllegalStateException.class)
                .message().doesNotContain("secret").contains("BEGIN RSA PRIVATE KEY");
    }

    @Test
    @DisplayName("PEM entier encode en base64")
    void base64OfWholePem() {
        assertSameKey(Base64.getEncoder().encodeToString(pkcs1Pem.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @DisplayName("Une cle illisible est decrite sans que son contenu apparaisse dans le message")
    void unreadableKeyDoesNotLeak() {
        String garbage = "-----BEGIN RSA PRIVATE KEY-----\nTOUTFAUXsecretABCDEF\n-----END RSA PRIVATE KEY-----";
        assertThatThrownBy(() -> PawapayRequestSigner.parsePrivateKey(garbage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("La cle de signature PawaPay")
                .message().doesNotContain("TOUTFAUXsecret");
    }

    @Test
    @DisplayName("Cle illisible · le client demarre, refuse les depots, ne les envoie jamais non signes")
    void unreadableKeyKeepsAppUpAndBlocksDeposits() throws Exception {
        AtomicInteger received = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            received.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            PawapayProperties properties = new PawapayProperties();
            properties.setApiKey("cle-de-test");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getSigning().setPrivateKey("pas une cle");
            properties.getSigning().setKeyId("BOKATI_KEY_1");

            PawapayClient client = new PawapayClient(properties, new ObjectMapper());

            PawapayDepositRequest request = new PawapayDepositRequest("d-1",
                    new PawapayDepositRequest.Payer("MMO", new PawapayDepositRequest.AccountDetails("242061234567", "MTN_MOMO_COG")),
                    "10", "XAF", null, "TX-1", "Paiement", java.util.List.of());
            assertThatThrownBy(() -> client.initiateDeposit(request))
                    .satisfies(error -> assertThat(PawapaySigningUnavailableException.find(error)).isNotNull());
            assertThat(received.get()).isZero();
        } finally {
            server.stop(0);
        }
    }
}
