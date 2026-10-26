package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositRequest;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La lecture du statut d'un depot, contre la reponse exacte que PawaPay v2 renvoie.
 *
 * <p>Un depot paye restait « en cours » : le rappel arrivait, la relecture repondait UNKNOWN.
 * La reponse ci-dessous est celle de PawaPay pour ce depot, au caractere pres (numero masque).</p>
 */
class PawapayClientStatusTest {

    private static final String V2_COMPLETED = """
            {"data":{"depositId":"99893752-0127-468d-a694-978d369bc6cb","status":"COMPLETED","amount":"1.00",\
            "currency":"XAF","country":"COG","payer":{"type":"MMO","accountDetails":{"phoneNumber":"242060000000",\
            "provider":"MTN_MOMO_COG"}},"customerMessage":"MANUAL PAYMENT","clientReferenceId":"TXN-1",\
            "created":"2026-10-26T08:59:54Z","providerTransactionId":"8442462698","metadata":{"purpose":"MANUAL_PAYMENT"}},\
            "status":"FOUND"}""";

    private HttpServer server;
    private volatile String lastContentType;
    private volatile String lastBody;
    private volatile String nextDepositResponse = "{\"depositId\":\"d-1\",\"status\":\"ACCEPTED\",\"created\":\"2026-10-26T09:00:00Z\"}";
    private PawapayClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/deposits/", exchange -> {
            byte[] body = V2_COMPLETED.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/v2/deposits", exchange -> {
            lastContentType = exchange.getRequestHeaders().getFirst("Content-Type");
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = nextDepositResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        PawapayProperties properties = new PawapayProperties();
        properties.setApiKey("cle-de-test");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        client = new PawapayClient(properties, new ObjectMapper());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("Un depot COMPLETED enveloppe dans data / FOUND se lit COMPLETED, avec la reference operateur")
    void readsTheV2Envelope() {
        PawapayClient.DepositStatus status = client.getDepositStatus("99893752-0127-468d-a694-978d369bc6cb");

        assertThat(status.found()).isTrue();
        assertThat(status.providerStatus()).isEqualTo("COMPLETED");
        assertThat(status.providerTransactionId()).isEqualTo("8442462698");
    }

    @Test
    @DisplayName("Un depot part en JSON, et la reponse ACCEPTED se lit")
    void depositRoundTrip() {
        PawapayDepositRequest request = new PawapayDepositRequest("d-1",
                new PawapayDepositRequest.Payer("MMO", new PawapayDepositRequest.AccountDetails("242061234567", "MTN_MOMO_COG")),
                "10", "XAF", null, "TX-1", "Paiement Elle A Ose", java.util.List.of());

        PawapayDepositResponse response = client.initiateDeposit(request);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(lastContentType).startsWith("application/json");
        assertThat(lastBody).contains("\"depositId\":\"d-1\"").contains("\"amount\":\"10\"");
    }

    @Test
    @DisplayName("Un refus porte son motif lisible, celui que la conversion Jackson 3 faisait echouer")
    void rejectionReasonIsReadable() {
        nextDepositResponse = "{\"depositId\":\"d-2\",\"status\":\"REJECTED\",\"failureReason\":"
                + "{\"failureCode\":\"PAYER_LIMIT_REACHED\",\"failureMessage\":\"Limite atteinte\"}}";
        PawapayDepositRequest request = new PawapayDepositRequest("d-2",
                new PawapayDepositRequest.Payer("MMO", new PawapayDepositRequest.AccountDetails("242061234567", "MTN_MOMO_COG")),
                "10", "XAF", null, "TX-2", "Paiement", java.util.List.of());

        PawapayDepositResponse response = client.initiateDeposit(request);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.rejectionReason().get("failureCode").asText()).isEqualTo("PAYER_LIMIT_REACHED");
    }
}
