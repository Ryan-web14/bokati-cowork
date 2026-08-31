package com.sni.bokaticowork.core.exception;

// Jackson 3 · le meme test ecrit avec com.fasterxml.jackson passait sans rien prouver :
// il exercait une classe d'exception homonyme, differente de celle que produit
// l'application. C'est exactement le defaut qu'il devait detecter.
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Une propriete inconnue doit etre refusee, et le champ fautif nomme.
 *
 * <p>Le refus seul ne suffirait pas : repondre « malformed » sans dire laquelle laisse chercher,
 * alors que la cause est presque toujours une clef mal orthographiee. C'est exactement le defaut
 * qui a laisse {@code catalogSourceCode} desarmer silencieusement le plancher de prix et le taux
 * de remise maximal.
 *
 * <p>Le test construit la chaine d'exceptions telle que Spring la produit, plutot que d'appeler
 * l'API : la propriete fautive etant dans une liste, Jackson enveloppe l'erreur d'un niveau, et
 * c'est precisement ce que le gestionnaire doit savoir traverser.
 */
class UnknownPropertyErrorTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(errorResponse());
    private final HttpServletRequest request = new MockHttpServletRequest("POST", "/sni/api/v1/billing/documents");

    @Test
    void shouldRejectAnUnknownPropertyOnALine() {
        ObjectMapper strict = strictMapper();

        assertThatThrownBy(() -> strict.readValue(
                "{\"description\":\"Salle\",\"unitPrice\":15000,\"itemCod\":\"SVC-000003\"}",
                CreateBillingDocumentLineRequest.class))
                .hasMessageContaining("itemCod");
    }

    @Test
    void shouldNameTheOffendingPropertyInTheResponse() {
        HttpMessageNotReadableException ex = notReadable(
                "{\"description\":\"Salle\",\"unitPrice\":15000,\"itemCod\":\"SVC-000003\"}");

        ResponseEntity<Object> response = handler.handleMessageNotReadable(ex, request);
        ApiError body = (ApiError) response.getBody();

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).contains("itemCod");
        // La liste des proprietes admises evite l'aller-retour vers la documentation.
        assertThat(body.getErrors()).anySatisfy(error ->
                assertThat(error).contains("itemCod").contains("itemCode"));
    }

    @Test
    void shouldTraverseTheWrappingWhenTheFaultIsInsideAList() {
        // Jackson enveloppe l'erreur d'un niveau quand la ligne fautive est dans une liste ·
        // ne regarder que la cause directe manquait le cas le plus courant.
        HttpMessageNotReadableException ex = notReadable(
                "{\"currency\":\"XAF\",\"lines\":[{\"description\":\"Salle\",\"unitPrice\":1,\"itemCod\":\"X\"}]}",
                com.sni.bokaticowork.features.billing.dto.request.SimulateBillingDocumentRequest.class);

        ApiError body = (ApiError) handler.handleMessageNotReadable(ex, request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getMessage()).contains("itemCod");
    }

    @Test
    void shouldKeepTheGenericMessageForAMalformedBody() {
        // Un corps illisible n'est pas une propriete inconnue · le message generique reste juste.
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error", new RuntimeException("truncated"), null);

        ApiError body = (ApiError) handler.handleMessageNotReadable(ex, request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Request body is missing or malformed.");
    }

    // =================================================================================

    private HttpMessageNotReadableException notReadable(String json) {
        return notReadable(json, CreateBillingDocumentLineRequest.class);
    }

    /** Reproduit l'enveloppe que Spring pose autour d'une erreur Jackson. */
    private HttpMessageNotReadableException notReadable(String json, Class<?> target) {
        ObjectMapper strict = strictMapper();
        try {
            strict.readValue(json, target);
            throw new IllegalStateException("la deserialisation aurait du echouer");
        } catch (tools.jackson.core.JacksonException cause) {
            return new HttpMessageNotReadableException("JSON parse error: " + cause.getMessage(),
                    cause, null);
        } catch (Exception other) {
            throw new IllegalStateException(other);
        }
    }

    private ObjectMapper strictMapper() {
        return JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private ErrorResponse errorResponse() {
        ErrorResponse response = new ErrorResponse();
        // devMode reste a false · on verifie le message rendu au client, pas la trace technique.
        return response;
    }
}
