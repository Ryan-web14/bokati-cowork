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

    /** Modeles fournis avec le logiciel, a creer chez Infobip ({@link #createDefaults()}). */
    public record DefaultTemplate(String purpose, String name, String category, String text, List<String> examples) {
    }

    public static final List<DefaultTemplate> DEFAULTS = List.of(
            new DefaultTemplate("BALANCE_CONFIRMATION", "confirmation_solde", "UTILITY",
                    "Bonjour, dans le cadre de la revision des comptes de {{1}}, le solde de votre compte au {{2}} est de {{3}} XAF {{4}}. "
                            + "Merci de nous confirmer ce solde, ou de nous indiquer le votre avec le detail de l'ecart, en rappelant la reference {{5}}.",
                    List.of("ELLE A OSE", "31/12/2026", "590 000", "en notre faveur", "CONF-2026-00001")));

    public record TemplateRequest(String purpose, String name, String language, String category, String text, List<String> examples) {
    }

    // ================================================================== envoi

    @Override
    public Result send(String phone, String purpose, List<String> placeholders, String text, String reference) {
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
                log.setKind("TEMPLATE");
                log.setTemplateName(template.get().getName());
                log.setContent(String.join(" | ", values));
                sent = infobip.sendTemplate(properties.getSender(), to, template.get().getName(), template.get().getLanguage(), values, messageId);
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
            messages.save(log);
            return new Result(sent.messageId(), log.getKind(), log.getTemplateName(), sent.statusGroup(), sent.statusName());
        } catch (BadRequestException exception) {
            if (log.getKind() != null) {
                log.setError(exception.getMessage().length() > 1000 ? exception.getMessage().substring(0, 1000) : exception.getMessage());
                messages.save(log);
            }
            throw exception;
        }
    }

    // ================================================================== modeles

    @Transactional(readOnly = true)
    public List<WhatsAppTemplate> templates() {
        return templates.findAllByOrderByPurposeAscNameAsc();
    }

    /** Cree un modele chez Infobip ; il part a la validation de Meta (statut PENDING). */
    public WhatsAppTemplate createTemplate(TemplateRequest request) {
        InfobipWhatsAppClient infobip = available();
        if (request == null || !StringUtils.hasText(request.purpose()) || !StringUtils.hasText(request.name()) || !StringUtils.hasText(request.text())) {
            throw new BadRequestException("Indiquez l'objet, le nom et le texte du modele");
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
        int count = variables(request.text());
        List<String> examples = request.examples() == null ? List.of() : request.examples();
        if (examples.size() != count) {
            throw new BadRequestException("Le texte compte " + count + " variable(s) {{n}} : donnez autant d'exemples");
        }
        if (templates.findByNameAndLanguage(name, language).isPresent()) {
            throw new BadRequestException("Le modele " + name + " (" + language + ") existe deja");
        }
        InfobipWhatsAppClient.RemoteTemplate remote = infobip.createTemplate(properties.getSender(), name, language, category, request.text().trim(), examples);
        return templates.save(WhatsAppTemplate.builder().purpose(request.purpose().trim().toUpperCase(Locale.ROOT)).name(name).language(language)
                .category(category).bodyText(request.text().trim()).examples(String.join("|", examples)).placeholders(count)
                .status(remote.status() == null ? "PENDING" : remote.status().toUpperCase(Locale.ROOT)).createdBy(actor()).createdAt(Instant.now()).build());
    }

    /** Cree chez Infobip les modeles fournis qui n'existent pas encore. */
    public List<WhatsAppTemplate> createDefaults() {
        List<WhatsAppTemplate> created = new ArrayList<>();
        for (DefaultTemplate item : DEFAULTS) {
            if (templates.findByNameAndLanguage(item.name(), properties.getLanguage()).isEmpty()) {
                created.add(createTemplate(new TemplateRequest(item.purpose(), item.name(), properties.getLanguage(), item.category(), item.text(),
                        item.examples())));
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
