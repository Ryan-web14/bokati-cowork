package com.sni.bokaticowork.core.communication.mailService.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OutboxNotificationMailService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.service.support.DocumentFileReader;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxNotificationMailServiceImpl implements OutboxNotificationMailService {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final DocumentFileReader documentFileReader;

    @Override
    public CompletableFuture<Boolean> sendDocumentNotification(String to, Map<String, Object> variables) {
        String html = renderTemplate("email/document-event", variables);
        return sendViaQueue(to, "Notification document", html, EmailPriority.NORMAL);
    }

    @Override
    public CompletableFuture<Boolean> sendKycNotification(String to, Map<String, Object> variables) {
        String html = renderTemplate("email/kyc-event", variables);
        return sendViaQueue(to, "Notification KYC", html, EmailPriority.NORMAL);
    }

    @Override
    public CompletableFuture<Boolean> sendKycNotification(String to, String subject, Map<String, Object> variables) {
        String html = renderTemplate("email/kyc-event", variables);
        return sendViaQueue(to, subject, html, EmailPriority.NORMAL);
    }

    @Override
    public CompletableFuture<Boolean> sendKycDocumentNotification(String to, Map<String, Object> variables) {
        String html = renderTemplate("email/kyc-expiry-reminder", variables);
        return sendViaQueue(to, "Document KYC · action requise", html, EmailPriority.BULK);
    }

    @Override
    public CompletableFuture<Boolean> sendContractNotification(String to, Map<String, Object> variables) {
        String html = renderTemplate("email/contract-event", variables);
        String documentCode = variables.get("documentCode") == null ? null : variables.get("documentCode").toString();
        String subject = contractSubject(variables.get("eventType"));

        if (StringUtils.hasText(documentCode)) {
            try {
                DocumentFileResult document = documentFileReader.read(documentCode);
                String fileName = StringUtils.hasText(document.fileName()) ? document.fileName() : documentCode + ".pdf";
                return emailSender.sendHtmlEmailWithPdfAttachment(to, subject, html, fileName, document.content());
            } catch (Exception ex) {
                throw new IllegalStateException("Could not attach contract PDF " + documentCode, ex);
            }
        }

        return sendViaQueue(to, subject, html, EmailPriority.HIGH);
    }

    private String contractSubject(Object eventType) {
        String type = eventType == null ? "" : eventType.toString();
        return switch (type) {
            case "CONTRACT_AUTO_ACTIVATED"  -> "Votre contrat est actif · Elle A Osé";
            case "CONTRACT_AUTO_EXPIRED"    -> "Votre contrat a expiré · Elle A Osé";
            case "CONTRACT_AUTO_RENEWED"    -> "Votre contrat a été renouvelé · Elle A Osé";
            case "CONTRACT_EXPIRY_ALERT"    -> "Votre contrat expire bientôt · Elle A Osé";
            case "CONTRACT_DRAFT_GENERATED" -> "Votre contrat est prêt · Elle A Osé";
            default -> "Notification contrat · Elle A Osé";
        };
    }

    @Override
    public CompletableFuture<Boolean> sendContractSigningNotification(String to, String subject, Map<String, Object> variables) {
        String html = renderTemplate("email/contract-signing-request", variables);
        return sendViaQueue(to, subject, html, EmailPriority.HIGH);
    }

    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }

    private CompletableFuture<Boolean> sendViaQueue(String to, String subject, String html, EmailPriority priority) {
        try {
            return emailSender.sendHtmlEmail(to, subject, html, priority);
        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to queue email to " + to, ex);
        }
    }
}