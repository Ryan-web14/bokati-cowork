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

    /** Etat d'un message selon Infobip (rapport de remise ou journal). */
    public record Report(String messageId, String to, String statusGroup, String statusName, String description, String error,
                         java.time.Instant sentAt, java.time.Instant doneAt) {
    }

    /** Chemin du webhook des statuts WhatsApp, sous l'adresse publique de l'API. */
    public static final String WEBHOOK_PATH = "/sni/api/v1/public/webhooks/infobip/whatsapp";

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
        return sendTemplate(from, to, templateName, language, placeholders, null, messageId);
    }

    /** Message par modele, avec le parametre du bouton (suffixe d'un lien, ou code a copier). */
    @SuppressWarnings("unchecked")
    public Sent sendTemplate(String from, String to, String templateName, String language, List<String> placeholders, String buttonParameter,
                             String messageId) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("from", from);
        message.put("to", to);
        message.put("messageId", messageId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("body", Map.of("placeholders", placeholders));
        if (buttonParameter != null) {
            data.put("buttons", List.of(Map.of("type", "URL", "parameter", buttonParameter)));
        }
        message.put("content", Map.of("templateName", templateName, "templateData", data, "language", language));
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
        return createTemplate(sender, name, language, category, Map.of("body", bodyPart));
    }

    /** Cree un modele avec sa structure complete (corps, pied, boutons), soumis a Meta. */
    public RemoteTemplate createTemplate(String sender, String name, String language, String category, Map<String, Object> structure) {
        Map<String, Object> body = Map.of("name", name, "language", language, "category", category, "structure", structure);
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

    /** Journal d'un message ({@code GET /messages-api/1/logs}, 48 heures de retention). */
    @SuppressWarnings("unchecked")
    public java.util.Optional<Report> log(String messageId) {
        try {
            Map<String, Object> response = restClient.get().uri(uri -> uri.path("/messages-api/1/logs").queryParam("messageId", messageId).build())
                    .retrieve().body(Map.class);
            Object results = response == null ? null : response.get("results");
            if (results instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                return java.util.Optional.of(report((Map<String, Object>) first));
            }
            return java.util.Optional.empty();
        } catch (RestClientResponseException exception) {
            throw failure(exception);
        }
    }

    /** Lit un rapport de remise (webhook ou journal). */
    @SuppressWarnings("unchecked")
    public static Report report(Map<String, Object> item) {
        Map<String, Object> status = item.get("status") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        Map<String, Object> error = item.get("error") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        String errorGroup = text(error.get("groupName"), "OK");
        String errorText = "OK".equals(errorGroup) || "NO_ERROR".equals(text(error.get("name"), "")) ? null
                : text(error.get("name"), errorGroup) + " : " + text(error.get("description"), "");
        return new Report(text(item.get("messageId"), null), text(item.get("to"), text(item.get("destination"), null)), text(status.get("groupName"), null),
                text(status.get("name"), null), text(status.get("description"), null), errorText, instant(item.get("sentAt")), instant(item.get("doneAt")));
    }

    /** Horodatage d'Infobip : ISO avec Z, decalage +0000 ou +00:00. */
    public static java.time.Instant instant(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        String text = value.toString().trim().replaceAll("([+-]\\d{2})(\\d{2})$", "$1:$2");
        try {
            return java.time.OffsetDateTime.parse(text).toInstant();
        } catch (java.time.format.DateTimeParseException exception) {
            try {
                return java.time.Instant.parse(text);
            } catch (java.time.format.DateTimeParseException ignored) {
                return null;
            }
        }
    }

    /** Abonnement existant ({@code GET /subscriptions/1/subscription/WHATSAPP/{id}}), ou vide. */
    @SuppressWarnings("unchecked")
    public java.util.Optional<Map<String, Object>> subscription(String subscriptionId) {
        try {
            return java.util.Optional.ofNullable(restClient.get().uri("/subscriptions/1/subscription/WHATSAPP/" + subscriptionId).retrieve().body(Map.class));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return java.util.Optional.empty();
            }
            throw failure(exception);
        }
    }

    /**
     * Cree ou met a jour l'abonnement aux evenements WhatsApp de l'expediteur : webhook signe HMAC
     * (droit {@code subscriptions:manage} sur la cle).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> subscribe(InfobipWhatsAppProperties properties, List<String> events, boolean update) {
        String url = properties.getWebhookBaseUrl().trim().replaceAll("/+$", "") + WEBHOOK_PATH;
        Map<String, Object> security = Map.of("authId", properties.getAuthId(), "type", "HMAC",
                "credentials", Map.of("secretKey", properties.getWebhookSecret(), "algorithm", properties.getWebhookAlgorithm()));
        Map<String, Object> profile = Map.of("profileId", properties.getProfileId(), "webhook", Map.of("notifyUrl", url), "security", security);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subscriptionId", properties.getSubscriptionId());
        body.put("name", "Bokati Cowork, statuts WhatsApp");
        body.put("events", events);
        body.put("resources", List.of(properties.getSender()));
        body.put("profile", profile);
        try {
            Map<String, Object> response = update
                    ? restClient.put().uri("/subscriptions/1/subscription/WHATSAPP/" + properties.getSubscriptionId()).body(body).retrieve().body(Map.class)
                    : restClient.post().uri("/subscriptions/1/subscription/WHATSAPP").body(body).retrieve().body(Map.class);
            return response == null ? Map.of() : response;
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
