package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppClient;
import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppProperties;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.phone.PhoneNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Envoi WhatsApp par Infobip : modeles de message par objet, envoi par modele approuve (ou texte
 * libre si autorise), journal des envois. Sans activation ({@code bokati.whatsapp.infobip.enabled}),
 * chaque envoi est refuse avec un message clair.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class WhatsAppService implements WhatsAppMessageSender {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{(\\d+)}}");

    private final InfobipWhatsAppProperties properties;
    private final ObjectProvider<InfobipWhatsAppClient> client;
    private final WhatsAppTemplateRepository templates;
    private final WhatsAppMessageRepository messages;
    private final PhoneNumberService phones;
    private final org.springframework.context.ApplicationEventPublisher events;

    /** Bouton d'un modele : URL (lien, {{1}} en fin d'adresse pour un suffixe) ou COPY_CODE (code d'authentification). */
    public record Button(String type, String text, String url, String example) {
    }

    /** Modeles fournis avec le logiciel, a creer chez Infobip ({@link #createDefaults()}). */
    public record DefaultTemplate(String purpose, String name, String category, String text, List<String> examples, String footer, Button button,
                                  Integer codeExpirationMinutes) {

        public DefaultTemplate(String purpose, String name, String category, String text, List<String> examples) {
            this(purpose, name, category, text, examples, null, null, null);
        }
    }

    private static final String PORTAL = "{portal}";

    /** Textes de service : ils nomment l'entreprise et ne commencent ni ne finissent par une variable (regles de Meta). */
    public static final List<DefaultTemplate> DEFAULTS = List.of(
            new DefaultTemplate("BALANCE_CONFIRMATION", "confirmation_solde", "UTILITY",
                    "Bonjour, dans le cadre de la révision des comptes de {{1}}, le solde de votre compte au {{2}} est de {{3}} XAF {{4}} "
                            + "(référence {{5}}). Merci de nous confirmer ce solde, ou de nous indiquer le vôtre avec le détail de l'écart, en répondant à ce message.",
                    List.of("ELLE A OSE", "31/12/2026", "590 000", "en notre faveur", "CONF-2026-00001")),
            new DefaultTemplate("BOOKING_CONFIRMED", "reservation_confirmee", "UTILITY",
                    "Bonjour {{1}}, votre réservation chez ELLE A OSE est confirmée.\n\nEspace : {{2}}\nDate : {{3}}\nHoraire : {{4}}\nRéférence : {{5}}\n\n"
                            + "Merci de vous présenter 10 minutes avant le début de votre session. Pour modifier ou annuler, répondez à ce message.",
                    List.of("Awa", "Salle Kouilou", "14/11/2026", "09:00 - 11:00", "BKG-2026-00123")),
            new DefaultTemplate("OTP", "code_verification", "AUTHENTICATION", null, List.of("482913"), null,
                    new Button("COPY_CODE", "Copier le code", null, null), 10),
            new DefaultTemplate("BOOKING_UPDATED", "reservation_modifiee", "UTILITY",
                    "Bonjour {{1}}, votre réservation {{2}} chez ELLE A OSE a été modifiée.\n\nEspace : {{3}}\nDate : {{4}}\nHoraire : {{5}}\n\n"
                            + "Retrouvez le détail dans votre espace client. Pour toute question, répondez à ce message.",
                    List.of("Awa", "BKG-2026-00123", "Salle Kouilou", "15/11/2026", "14:00 - 16:00"), "ELLE A OSE",
                    new Button("URL", "Voir ma réservation", PORTAL + "/{{1}}", PORTAL + "/reservations"), null),
            new DefaultTemplate("PAYMENT_REMINDER", "rappel_paiement", "UTILITY",
                    "Bonjour {{1}}, la facture {{2}} de ELLE A OSE, d'un montant de {{3}} XAF, est à régler avant le {{4}}. "
                            + "Vous pouvez la payer depuis votre espace client, ou directement à l'accueil de l'espace ELLE A OSE.",
                    List.of("Awa", "FAC-2026-00012", "150 000", "30/11/2026"), "ELLE A OSE",
                    new Button("URL", "Payer ma facture", PORTAL + "/{{1}}", PORTAL + "/factures"), null),
            new DefaultTemplate("SUBSCRIPTION_ACTIVATED", "abonnement_actif", "UTILITY",
                    "Bonjour {{1}}, votre abonnement {{2}} chez ELLE A OSE est actif du {{3}} au {{4}}. "
                            + "Retrouvez vos accès et vos réservations dans votre espace client.",
                    List.of("Awa", "Résident mensuel", "01/12/2026", "31/12/2026"), "ELLE A OSE",
                    new Button("URL", "Mon espace client", PORTAL + "/{{1}}", PORTAL + "/abonnements"), null),
            new DefaultTemplate("INVOICE_PAID", "facture_payee", "UTILITY",
                    "Bonjour {{1}}, nous avons bien reçu votre paiement de {{2}} XAF : la facture {{3}} de ELLE A OSE est soldée. "
                            + "Vous pouvez la télécharger dans votre espace client.",
                    List.of("Awa", "150 000", "FAC-2026-00012"), "ELLE A OSE",
                    new Button("URL", "Télécharger ma facture", PORTAL + "/{{1}}", PORTAL + "/factures"), null));

    public record TemplateRequest(String purpose, String name, String language, String category, String text, List<String> examples, String footer,
                                  Button button, Integer codeExpirationMinutes) {

        public TemplateRequest(String purpose, String name, String language, String category, String text, List<String> examples) {
            this(purpose, name, language, category, text, examples, null, null, null);
        }
    }

    // ================================================================== envoi

    @Override
    public Result send(String phone, String purpose, List<String> placeholders, String text, String reference, String buttonParameter) {
        InfobipWhatsAppClient infobip = available();
        String to = phones.normalize(phone, properties.getDefaultCountry()).replace("+", "");
        String messageId = UUID.randomUUID().toString();
        Optional<WhatsAppTemplate> template = StringUtils.hasText(purpose)
                ? templates.findFirstByPurposeAndLanguageAndStatusOrderByCreatedAtDesc(purpose, properties.getLanguage(), "APPROVED")
                : Optional.empty();
        WhatsAppMessage log = WhatsAppMessage.builder().messageId(messageId).toPhone(to).purpose(purpose).reference(reference).createdBy(actor()).build();
        try {
            InfobipWhatsAppClient.Sent sent;
            if (template.isPresent()) {
                List<String> values = placeholders == null ? List.of() : placeholders;
                if (values.size() != template.get().getPlaceholders()) {
                    throw new BadRequestException("Le modele " + template.get().getName() + " attend " + template.get().getPlaceholders()
                            + " variable(s), " + values.size() + " fournie(s)");
                }
                String parameter = buttonParameter(template.get(), values, buttonParameter);
                log.setKind("TEMPLATE");
                log.setTemplateName(template.get().getName());
                log.setContent(String.join(" | ", values) + (parameter == null || "COPY_CODE".equals(template.get().getButtonType()) ? "" : " | " + parameter));
                sent = infobip.sendTemplate(properties.getSender(), to, template.get().getName(), template.get().getLanguage(), values, parameter, messageId);
            } else if (properties.isFreeTextAllowed() && StringUtils.hasText(text)) {
                log.setKind("TEXT");
                log.setContent(text);
                sent = infobip.sendText(properties.getSender(), to, text, messageId);
            } else {
                throw new BadRequestException("Aucun modele WhatsApp approuve pour " + purpose
                        + " : creez-le (modeles WhatsApp) et attendez l'approbation de Meta, ou choisissez un autre canal");
            }
            log.setStatusGroup(sent.statusGroup());
            log.setStatusName(sent.statusName());
            log.setStatusDescription(sent.description());
            log.setSentAt(Instant.now());
            log.setNotificationNumber("WA-" + messageId);
            messages.save(log);
            publish(log);
            return new Result(sent.messageId(), log.getKind(), log.getTemplateName(), sent.statusGroup(), sent.statusName());
        } catch (BadRequestException exception) {
            if (log.getKind() != null) {
                log.setError(exception.getMessage().length() > 1000 ? exception.getMessage().substring(0, 1000) : exception.getMessage());
                messages.save(log);
            }
            throw exception;
        }
    }

    // ================================================================== suivi de remise et de lecture

    /** Applique un rapport de remise (webhook ou journal) ; un message inconnu est ignore. */
    public boolean applyReport(InfobipWhatsAppClient.Report report) {
        if (report == null || report.messageId() == null) {
            return false;
        }
        Optional<WhatsAppMessage> found = messages.findByMessageId(report.messageId());
        if (found.isEmpty()) {
            return false;
        }
        WhatsAppMessage message = found.get();
        if (report.statusGroup() != null) {
            message.setStatusGroup(report.statusGroup());
            message.setStatusName(report.statusName());
            message.setStatusDescription(report.description());
        }
        if (report.sentAt() != null) {
            message.setSentAt(report.sentAt());
        }
        if (report.doneAt() != null && !"PENDING".equals(report.statusGroup())) {
            message.setDoneAt(report.doneAt());
        }
        if (report.error() != null) {
            message.setError(report.error().length() > 1000 ? report.error().substring(0, 1000) : report.error());
        }
        message.setCheckedAt(Instant.now());
        messages.save(message);
        publish(message);
        return true;
    }

    /** Applique une lecture (webhook SEEN). */
    public boolean applySeen(String messageId, Instant seenAt) {
        Optional<WhatsAppMessage> found = messageId == null ? Optional.empty() : messages.findByMessageId(messageId);
        if (found.isEmpty()) {
            return false;
        }
        WhatsAppMessage message = found.get();
        message.setSeenAt(seenAt == null ? Instant.now() : seenAt);
        messages.save(message);
        publish(message);
        return true;
    }

    /**
     * Relit chez Infobip le journal des messages encore en attente (secours du webhook ; les
     * journaux sont gardes 48 heures). Retourne le nombre de messages mis a jour.
     */
    public int poll(int limit) {
        InfobipWhatsAppClient infobip = client.getIfAvailable();
        if (!properties.isEnabled() || infobip == null) {
            return 0;
        }
        int updated = 0;
        for (WhatsAppMessage message : messages.findPending(Instant.now().minus(java.time.Duration.ofHours(47)),
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)))) {
            try {
                Optional<InfobipWhatsAppClient.Report> report = infobip.log(message.getMessageId());
                if (report.isPresent() && applyReport(report.get())) {
                    updated++;
                } else {
                    message.setCheckedAt(Instant.now());
                    messages.save(message);
                }
            } catch (RuntimeException exception) {
                org.slf4j.LoggerFactory.getLogger(WhatsAppService.class).warn("Journal Infobip illisible pour {} : {}", message.getMessageId(), exception.getMessage());
            }
        }
        return updated;
    }

    private void publish(WhatsAppMessage message) {
        events.publishEvent(new WhatsAppStatusEvent(message.getMessageId(), message.getNotificationNumber(), message.getToPhone(), message.getPurpose(),
                message.getTemplateName(), message.getReference(), message.getStatusGroup(), message.getStatusName(), message.getStatusDescription(),
                message.getError(), message.getSentAt(), message.getDoneAt(), message.getSeenAt()));
    }

    // ================================================================== abonnement aux webhooks

    /** Evenements suivis : remise, lecture, mise a jour des modeles. */
    public static final List<String> SUBSCRIBED_EVENTS = List.of("DELIVERY", "SEEN");

    /** Abonnement chez Infobip (vide s'il n'existe pas). */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> subscription() {
        return available().subscription(properties.getSubscriptionId());
    }

    /**
     * Cree (ou met a jour) chez Infobip l'abonnement aux statuts WhatsApp de l'expediteur, vers le
     * webhook de l'application, signe HMAC avec le secret de configuration.
     */
    public Map<String, Object> subscribe() {
        InfobipWhatsAppClient infobip = available();
        if (!StringUtils.hasText(properties.getWebhookSecret()) || properties.getWebhookSecret().length() < 24) {
            throw new BadRequestException("Renseignez INFOBIP_WEBHOOK_SECRET (24 caracteres au moins) avant de creer l'abonnement");
        }
        if (!StringUtils.hasText(properties.getWebhookBaseUrl()) || !properties.getWebhookBaseUrl().startsWith("https://")) {
            throw new BadRequestException("L'adresse publique de l'API (APP_BASE_URL) doit etre en https pour recevoir les webhooks");
        }
        boolean exists = infobip.subscription(properties.getSubscriptionId()).isPresent();
        return infobip.subscribe(properties, SUBSCRIBED_EVENTS, exists);
    }

    // ================================================================== modeles

    @Transactional(readOnly = true)
    public List<WhatsAppTemplate> templates() {
        return templates.findAllByOrderByPurposeAscNameAsc();
    }

    /** Parametre du bouton a l'envoi : le code pour un modele d'authentification, le suffixe d'un lien variable. */
    static String buttonParameter(WhatsAppTemplate template, List<String> values, String given) {
        if ("COPY_CODE".equals(template.getButtonType())) {
            return values.isEmpty() ? null : values.get(0);
        }
        if ("URL".equals(template.getButtonType()) && template.getButtonUrl() != null && template.getButtonUrl().contains("{{1}}")) {
            if (!StringUtils.hasText(given)) {
                throw new BadRequestException("Le modele " + template.getName() + " attend le suffixe du lien de son bouton");
            }
            return given.trim().replaceAll("^/+", "");
        }
        return null;
    }

    /** Cree un modele chez Infobip ; il part a la validation de Meta (statut PENDING). */
    public WhatsAppTemplate createTemplate(TemplateRequest request) {
        InfobipWhatsAppClient infobip = available();
        if (request == null || !StringUtils.hasText(request.purpose()) || !StringUtils.hasText(request.name())) {
            throw new BadRequestException("Indiquez l'objet et le nom du modele");
        }
        String name = request.name().trim().toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_]{1,120}")) {
            throw new BadRequestException("Nom de modele : minuscules, chiffres et soulignes seulement");
        }
        String language = StringUtils.hasText(request.language()) ? request.language().trim() : properties.getLanguage();
        String category = StringUtils.hasText(request.category()) ? request.category().trim().toUpperCase(Locale.ROOT) : "UTILITY";
        if (!List.of("UTILITY", "MARKETING", "AUTHENTICATION").contains(category)) {
            throw new BadRequestException("Categorie UTILITY, MARKETING ou AUTHENTICATION");
        }
        boolean authentication = "AUTHENTICATION".equals(category);
        List<String> examples = request.examples() == null ? List.of() : request.examples();
        int count;
        Map<String, Object> structure = new java.util.LinkedHashMap<>();
        if (authentication) {
            // Texte impose par Meta (« {{1}} est votre code de verification »), avec la recommandation de securite.
            count = 1;
            structure.put("body", Map.of("addSecurityRecommendation", true));
            structure.put("footer", Map.of("codeExpirationMinutes", request.codeExpirationMinutes() == null ? 10 : request.codeExpirationMinutes()));
            structure.put("buttons", List.of(Map.of("text", request.button() == null || !StringUtils.hasText(request.button().text())
                    ? "Copier le code" : request.button().text(), "otpType", "COPY_CODE")));
        } else {
            if (!StringUtils.hasText(request.text())) {
                throw new BadRequestException("Indiquez le texte du modele");
            }
            count = variables(request.text());
            if (examples.size() != count) {
                throw new BadRequestException("Le texte compte " + count + " variable(s) {{n}} : donnez autant d'exemples");
            }
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("text", request.text().trim());
            if (!examples.isEmpty()) {
                body.put("examples", examples);
            }
            structure.put("body", body);
            if (StringUtils.hasText(request.footer())) {
                structure.put("footer", Map.of("text", request.footer().trim()));
            }
            if (request.button() != null && "URL".equals(request.button().type())) {
                String url = portal(request.button().url());
                if (!url.startsWith("https://")) {
                    throw new BadRequestException("Le lien du bouton doit etre en https");
                }
                Map<String, Object> button = new java.util.LinkedHashMap<>();
                button.put("type", "URL");
                button.put("text", request.button().text());
                button.put("url", url);
                if (url.contains("{{1}}")) {
                    button.put("example", portal(request.button().example()));
                }
                structure.put("buttons", List.of(button));
            }
        }
        if (templates.findByNameAndLanguage(name, language).isPresent()) {
            throw new BadRequestException("Le modele " + name + " (" + language + ") existe deja");
        }
        InfobipWhatsAppClient.RemoteTemplate remote = infobip.createTemplate(properties.getSender(), name, language, category, structure);
        Button button = request.button();
        return templates.save(WhatsAppTemplate.builder().purpose(request.purpose().trim().toUpperCase(Locale.ROOT)).name(name).language(language)
                .category(category).bodyText(authentication ? "{{1}} est votre code de vérification." : request.text().trim())
                .examples(String.join("|", examples)).placeholders(count)
                .footerText(authentication ? null : request.footer())
                .buttonType(authentication ? "COPY_CODE" : button == null ? null : button.type())
                .buttonText(button == null ? (authentication ? "Copier le code" : null) : button.text())
                .buttonUrl(button == null || authentication ? null : portal(button.url()))
                .buttonExample(button == null || authentication ? null : portal(button.example()))
                .codeExpirationMinutes(authentication ? (request.codeExpirationMinutes() == null ? 10 : request.codeExpirationMinutes()) : null)
                .status(remote.status() == null ? "PENDING" : remote.status().toUpperCase(Locale.ROOT)).createdBy(actor()).createdAt(Instant.now()).build());
    }

    /** Remplace {portal} par l'adresse de l'espace client. */
    private String portal(String value) {
        return value == null ? null : value.replace(PORTAL, properties.getPortalUrl().replaceAll("/+$", ""));
    }

    /** Cree chez Infobip les modeles fournis qui n'existent pas encore. */
    public List<WhatsAppTemplate> createDefaults() {
        List<WhatsAppTemplate> created = new ArrayList<>();
        for (DefaultTemplate item : DEFAULTS) {
            if (templates.findByNameAndLanguage(item.name(), properties.getLanguage()).isEmpty()) {
                created.add(createTemplate(new TemplateRequest(item.purpose(), item.name(), properties.getLanguage(), item.category(), item.text(),
                        item.examples(), item.footer(), item.button(), item.codeExpirationMinutes())));
            }
        }
        return created;
    }

    /** Relit chez Infobip le statut des modeles (approuve, refuse...). */
    public List<WhatsAppTemplate> sync() {
        InfobipWhatsAppClient infobip = available();
        Map<String, InfobipWhatsAppClient.RemoteTemplate> remote = new java.util.HashMap<>();
        infobip.templates(properties.getSender()).forEach(item -> remote.put(item.name() + "|" + item.language(), item));
        for (WhatsAppTemplate template : templates.findAll()) {
            InfobipWhatsAppClient.RemoteTemplate found = remote.get(template.getName() + "|" + template.getLanguage());
            template.setStatus(found == null ? "UNKNOWN" : found.status() == null ? template.getStatus() : found.status().toUpperCase(Locale.ROOT));
            template.setSyncedAt(Instant.now());
            templates.save(template);
        }
        // Modeles fournis deja crees chez Infobip (hors de cette base) : importes avec leur statut.
        for (DefaultTemplate item : DEFAULTS) {
            InfobipWhatsAppClient.RemoteTemplate found = remote.get(item.name() + "|" + properties.getLanguage());
            if (found == null || templates.findByNameAndLanguage(item.name(), properties.getLanguage()).isPresent()) {
                continue;
            }
            boolean authentication = "AUTHENTICATION".equals(item.category());
            templates.save(WhatsAppTemplate.builder().purpose(item.purpose()).name(item.name()).language(properties.getLanguage()).category(item.category())
                    .bodyText(authentication ? "{{1}} est votre code de vérification." : item.text()).examples(String.join("|", item.examples()))
                    .placeholders(authentication ? 1 : variables(item.text())).footerText(item.footer())
                    .buttonType(item.button() == null ? null : item.button().type()).buttonText(item.button() == null ? null : item.button().text())
                    .buttonUrl(item.button() == null ? null : portal(item.button().url()))
                    .buttonExample(item.button() == null ? null : portal(item.button().example()))
                    .codeExpirationMinutes(item.codeExpirationMinutes())
                    .status(found.status() == null ? "PENDING" : found.status().toUpperCase(Locale.ROOT)).createdBy("INFOBIP")
                    .createdAt(Instant.now()).syncedAt(Instant.now()).build());
        }
        return templates();
    }

    @Transactional(readOnly = true)
    public List<WhatsAppMessage> messages(String reference) {
        return StringUtils.hasText(reference) ? messages.findAllByReferenceOrderByCreatedAtDesc(reference.trim()) : messages.findTop200ByOrderByCreatedAtDesc();
    }

    // ================================================================== outils

    /** Nombre de variables distinctes {{n}} d'un texte. */
    static int variables(String text) {
        Matcher matcher = VARIABLE.matcher(text == null ? "" : text);
        java.util.Set<String> found = new java.util.HashSet<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found.size();
    }

    private InfobipWhatsAppClient available() {
        InfobipWhatsAppClient infobip = client.getIfAvailable();
        if (!properties.isEnabled() || infobip == null || !StringUtils.hasText(properties.getSender())) {
            throw new BadRequestException("L'envoi WhatsApp n'est pas configure (Infobip) : choisissez le courriel ou l'impression");
        }
        return infobip;
    }

    private static String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "SYSTEM" : authentication.getName();
    }

    static List<String> split(String value) {
        return StringUtils.hasText(value) ? Arrays.asList(value.split("\\|")) : List.of();
    }
}
