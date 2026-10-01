package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Le client HTTP de PawaPay · avec des delais, et une distinction que tout le reste exige.
 *
 * <p>Deux echecs n'ont rien a voir : l'operateur a repondu « non » (le paiement n'aura pas lieu),
 * ou l'operateur n'a pas repondu du tout (le paiement a peut-etre lieu quand meme). Le premier est
 * un {@code REJECTED} ; le second remonte en {@link ProviderUnreachableException} et ne doit
 * jamais etre lu comme un refus. Sans delai, un appel pouvait aussi rester suspendu sans fin.</p>
 */
@Slf4j
public class PawapayClient {

    /** L'operateur n'a pas repondu · reseau, delai, ou 5xx. L'issue est inconnue, pas negative. */
    public static class ProviderUnreachableException extends RuntimeException {
        public ProviderUnreachableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    PawapayClient(PawapayProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public PawapayDepositResponse initiateDeposit(PawapayDepositRequest request) {
        log.debug("Initiating PawaPay deposit: depositId={}, provider={}",
                request.depositId(), request.payer().accountDetails().provider());
        try {
            return restClient.post()
                    .uri("/v2/deposits")
                    .body(request)
                    .retrieve()
                    .body(PawapayDepositResponse.class);
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable à l'initiation", ex);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ProviderUnreachableException("PawaPay en erreur " + ex.getStatusCode().value(), ex);
            }
            throw ex;
        }
    }

    /**
     * Le statut d'un depot · v2 enveloppe la reponse ({@code status: FOUND, data: {...}}), v1 la
     * rendait a plat. On lit les deux, et {@code NOT_FOUND} est une reponse a part entiere : le
     * depot n'a jamais atteint l'operateur.
     */
    public DepositStatus getDepositStatus(String depositId) {
        log.debug("Checking PawaPay deposit status: depositId={}", depositId);
        JsonNode body;
        try {
            body = restClient.get()
                    .uri("/v2/deposits/{depositId}", depositId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable pour le statut", ex);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return DepositStatus.notFound(depositId);
            }
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ProviderUnreachableException("PawaPay en erreur " + ex.getStatusCode().value(), ex);
            }
            throw ex;
        }
        return DepositStatus.parse(depositId, body, objectMapper);
    }

    public PawapayPaymentPageResponse createPaymentPage(PawapayPaymentPageRequest request) {
        log.debug("Creating PawaPay payment page: depositId={}", request.depositId());
        try {
            return restClient.post()
                    .uri("/v2/paymentpage")
                    .body(request)
                    .retrieve()
                    .body(PawapayPaymentPageResponse.class);
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable pour la page de paiement", ex);
        }
    }

    public PawapayRefundResponse initiateRefund(PawapayRefundRequest request) {
        log.debug("Initiating PawaPay refund: refundId={}, depositId={}", request.refundId(), request.depositId());
        try {
            return restClient.post()
                    .uri("/v2/refunds")
                    .body(request)
                    .retrieve()
                    .body(PawapayRefundResponse.class);
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable pour le remboursement", ex);
        }
    }

    /** Le statut d'un remboursement · meme enveloppe que les depots. */
    public DepositStatus getRefundStatus(String refundId) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri("/v2/refunds/{refundId}", refundId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable pour le statut du remboursement", ex);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return DepositStatus.notFound(refundId);
            }
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ProviderUnreachableException("PawaPay en erreur " + ex.getStatusCode().value(), ex);
            }
            throw ex;
        }
        return DepositStatus.parse(refundId, body, objectMapper);
    }

    /** Ce qu'on retient d'une reponse de statut, quelle qu'en soit la forme. */
    public record DepositStatus(String depositId, boolean found, String providerStatus, JsonNode failureReason,
                                String providerTransactionId, JsonNode raw) {

        public static DepositStatus notFound(String depositId) {
            return new DepositStatus(depositId, false, "NOT_FOUND", null, null, null);
        }

        static DepositStatus parse(String depositId, JsonNode body, ObjectMapper mapper) {
            if (body == null || body.isNull()) {
                return notFound(depositId);
            }
            JsonNode node = body;
            if (body.isArray()) {
                if (body.isEmpty()) {
                    return notFound(depositId);
                }
                node = body.get(0);
            } else if (body.hasNonNull("data") && body.get("data").isObject()) {
                if ("NOT_FOUND".equalsIgnoreCase(text(body, "status"))) {
                    return notFound(depositId);
                }
                node = body.get("data");
            } else if ("NOT_FOUND".equalsIgnoreCase(text(body, "status")) && !body.has("depositId")) {
                return notFound(depositId);
            }
            String status = text(node, "status");
            if ("FOUND".equalsIgnoreCase(status) && node.hasNonNull("data")) {
                node = node.get("data");
                status = text(node, "status");
            }
            return new DepositStatus(depositId, true, status, node.get("failureReason"), text(node, "providerTransactionId"), node);
        }

        private static String text(JsonNode node, String field) {
            return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
        }
    }
}
