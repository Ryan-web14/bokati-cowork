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
                .requestInterceptors(interceptors -> {
                    if (properties.getSigning().isConfigured()) {
                        interceptors.add(new PawapaySigningInterceptor(new PawapayRequestSigner(
                                properties.getSigning().getPrivateKey(),
                                properties.getSigning().getKeyId(),
                                properties.getSigning().getAlgorithm())));
                    }
                })
                .build();
        if (properties.getSigning().isConfigured()) {
            log.info("PawaPay · requetes financieres signees (keyid={}, alg={})",
                    properties.getSigning().getKeyId(), properties.getSigning().getAlgorithm());
        } else {
            log.warn("PawaPay · aucune cle de signature configuree, les requetes partent non signees. "
                    + "Si les appels signes sont exiges chez PawaPay, depots et remboursements seront refuses.");
        }
    }

    public PawapayDepositResponse initiateDeposit(PawapayDepositRequest request) {
        log.debug("Initiating PawaPay deposit: depositId={}, provider={}",
                request.depositId(), request.payer().accountDetails().provider());
        try {
            return post("/v2/deposits", request, PawapayDepositResponse.class);
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
     * Envoie un POST et lit la reponse avec l'{@link ObjectMapper} Jackson 2 du client.
     *
     * <p>Spring Boot 4 convertit le JSON avec Jackson 3, qui ne sait pas fabriquer un
     * {@code com.fasterxml.jackson.databind.JsonNode} · chaque lecture de statut levait une exception,
     * rangee en UNKNOWN, et un depot paye restait « en cours ». Le client echange donc des octets et
     * fait lui-meme la conversion. Bonus : la signature porte sur exactement les octets envoyes.</p>
     */
    private <T> T post(String uri, Object request, Class<T> responseType) {
        byte[] response = restClient.post()
                .uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(writeBytes(request))
                .retrieve()
                .body(byte[].class);
        return read(response, responseType);
    }

    private byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Requete PawaPay non serialisable", ex);
        }
    }

    private <T> T read(byte[] body, Class<T> type) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(body, type);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Reponse PawaPay illisible", ex);
        }
    }

    private JsonNode readTree(byte[] body) {
        return read(body, JsonNode.class);
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
            body = readTree(restClient.get()
                    .uri("/v2/deposits/{depositId}", depositId)
                    .retrieve()
                    .body(byte[].class));
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

    /** L'operateur d'un numero, selon PawaPay · {@code null} s'il ne sait pas le dire. */
    public record ProviderPrediction(String country, String provider, String phoneNumber) {
    }

    /**
     * Devine l'operateur d'un numero · le payeur n'a plus a le choisir.
     *
     * <p>Un 4xx (numero illisible, pays non couvert) rend {@code null} : la page demande alors
     * l'operateur au payeur. Une panne remonte en {@link ProviderUnreachableException}.</p>
     */
    public ProviderPrediction predictProvider(String phoneNumber) {
        try {
            JsonNode body = post("/v2/predict-provider", java.util.Map.of("phoneNumber", phoneNumber), JsonNode.class);
            if (body == null || !body.hasNonNull("provider")) {
                return null;
            }
            return new ProviderPrediction(body.path("country").asText(null), body.path("provider").asText(null),
                    body.path("phoneNumber").asText(null));
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable pour la prediction d'operateur", ex);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ProviderUnreachableException("PawaPay en erreur " + ex.getStatusCode().value(), ex);
            }
            log.info("Operateur non devine pour un numero · HTTP {}", ex.getStatusCode().value());
            return null;
        }
    }

    public PawapayPaymentPageResponse createPaymentPage(PawapayPaymentPageRequest request) {
        log.debug("Creating PawaPay payment page: depositId={}", request.depositId());
        try {
            return post("/v2/paymentpage", request, PawapayPaymentPageResponse.class);
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable pour la page de paiement", ex);
        }
    }

    public PawapayRefundResponse initiateRefund(PawapayRefundRequest request) {
        log.debug("Initiating PawaPay refund: refundId={}, depositId={}", request.refundId(), request.depositId());
        try {
            return post("/v2/refunds", request, PawapayRefundResponse.class);
        } catch (ResourceAccessException ex) {
            throw new ProviderUnreachableException("PawaPay injoignable pour le remboursement", ex);
        }
    }

    /** Le statut d'un remboursement · meme enveloppe que les depots. */
    public DepositStatus getRefundStatus(String refundId) {
        JsonNode body;
        try {
            body = readTree(restClient.get()
                    .uri("/v2/refunds/{refundId}", refundId)
                    .retrieve()
                    .body(byte[].class));
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
