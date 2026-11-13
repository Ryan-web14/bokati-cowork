package com.sni.bokaticowork.core.communication.whatsapp.infobip;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP d'Infobip pour WhatsApp : texte libre, message par modele, creation et lecture des
 * modeles. Authentification {@code Authorization: App <cle>}.
 */
@Slf4j
public class InfobipWhatsAppClient {

    /** Reponse d'envoi : identifiant et statut. */
    public record Sent(String messageId, String statusGroup, String statusName, String description) {
    }

    /** Modele tel qu'Infobip le connait. */
    public record RemoteTemplate(String name, String language, String category, String status) {
    }

    private final RestClient restClient;

    public InfobipWhatsAppClient(InfobipWhatsAppProperties properties) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs())).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim().replaceAll("/+$", "");
        this.restClient = RestClient.builder()
                .baseUrl(base.startsWith("http") ? base : "https://" + base)
                .requestFactory(factory)
                .defaultHeader("Authorization", "App " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /** Texte libre ({@code POST /whatsapp/1/message/text}). */
    public Sent sendText(String from, String to, String text, String messageId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", to);
        body.put("messageId", messageId);
        body.put("content", Map.of("text", text));
        return sent(call("/whatsapp/1/message/text", body), messageId);
    }

    /** Message par modele ({@code POST /whatsapp/1/message/template}). */
    @SuppressWarnings("unchecked")
    public Sent sendTemplate(String from, String to, String templateName, String language, List<String> placeholders, String messageId) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("from", from);
        message.put("to", to);
        message.put("messageId", messageId);
        message.put("content", Map.of("templateName", templateName, "templateData", Map.of("body", Map.of("placeholders", placeholders)),
                "language", language));
        Map<String, Object> response = call("/whatsapp/1/message/template", Map.of("messages", List.of(message)));
        Object messages = response.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return sent((Map<String, Object>) first, messageId);
        }
        return sent(response, messageId);
    }

    /** Cree un modele, soumis a Meta ({@code POST /whatsapp/2/senders/{sender}/templates}). */
    public RemoteTemplate createTemplate(String sender, String name, String language, String category, String text, List<String> examples) {
        Map<String, Object> bodyPart = new LinkedHashMap<>();
        bodyPart.put("text", text);
        if (!examples.isEmpty()) {
            bodyPart.put("examples", examples);
        }
        Map<String, Object> body = Map.of("name", name, "language", language, "category", category, "structure", Map.of("body", bodyPart));
        Map<String, Object> response = call("/whatsapp/2/senders/" + sender + "/templates", body);
        return new RemoteTemplate(name, language, category, text(response.get("status"), "PENDING"));
    }

    /** Modeles de l'expediteur et leurs statuts ({@code GET /whatsapp/2/senders/{sender}/templates}). */
    @SuppressWarnings("unchecked")
    public List<RemoteTemplate> templates(String sender) {
        try {
            Map<String, Object> response = restClient.get().uri("/whatsapp/2/senders/" + sender + "/templates").retrieve().body(Map.class);
            // La liste arrive sous templates (v2) ; results selon les versions de l'API.
            Object templates = response == null ? null : response.get("templates") != null ? response.get("templates") : response.get("results");
            if (!(templates instanceof List<?> list)) {
                return List.of();
            }
            return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item)
                    .map(item -> new RemoteTemplate(text(item.get("name"), null), text(item.get("language"), null), text(item.get("category"), null),
                            text(item.get("status"), null)))
                    .toList();
        } catch (RestClientResponseException exception) {
            throw failure(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String path, Object body) {
        try {
            Map<String, Object> response = restClient.post().uri(path).body(body).retrieve().body(Map.class);
            return response == null ? Map.of() : response;
        } catch (RestClientResponseException exception) {
            throw failure(exception);
        }
    }

    private static BadRequestException failure(RestClientResponseException exception) {
        String detail = exception.getResponseBodyAsString();
        log.warn("Infobip WhatsApp · {} : {}", exception.getStatusCode(), detail);
        return new BadRequestException("Infobip a refuse la demande (" + exception.getStatusCode().value() + ") : "
                + (detail.length() > 400 ? detail.substring(0, 400) : detail));
    }

    @SuppressWarnings("unchecked")
    private static Sent sent(Map<String, Object> response, String messageId) {
        Map<String, Object> status = response.get("status") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        return new Sent(text(response.get("messageId"), messageId), text(status.get("groupName"), null), text(status.get("name"), null),
                text(status.get("description"), null));
    }

    private static String text(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }
}
