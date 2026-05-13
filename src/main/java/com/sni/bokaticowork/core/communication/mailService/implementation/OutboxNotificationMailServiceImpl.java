package com.sni.bokaticowork.core.communication.mailService.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OutboxNotificationMailService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.service.support.DocumentFileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
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
        sendHtmlBlocking(to, "Notification document", "email/document-event", variables);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> sendKycNotification(String to, Map<String, Object> variables) {
        sendHtmlBlocking(to, "Notification KYC", "email/kyc-event", variables);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> sendKycDocumentNotification(String to, Map<String, Object> variables) {
        sendHtmlBlocking(to, "Document KYC — action requise", "email/kyc-expiry-reminder", variables);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> sendContractNotification(String to, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        String html = templateEngine.process("email/contract-event", context);
        String documentCode = variables.get("documentCode") == null ? null : variables.get("documentCode").toString();

        if (StringUtils.hasText(documentCode)) {
            try {
                DocumentFileResult document = documentFileReader.read(documentCode);
                String fileName = StringUtils.hasText(document.fileName()) ? document.fileName() : documentCode + ".pdf";
                emailSender.sendHtmlEmailWithPdfAttachmentBlocking(to, "Notification contrat", html, fileName, document.content());
                return CompletableFuture.completedFuture(true);
            } catch (Exception ex) {
                throw new IllegalStateException("Could not attach contract PDF " + documentCode, ex);
            }
        }

        emailSender.sendHtmlEmailBlocking(to, "Notification contrat", html);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Envoi synchrone bloquant. Lance une RuntimeException si l'envoi échoue,
     * ce qui permet à l'outbox de marquer l'event FAILED et de retenter.
     */
    private void sendHtmlBlocking(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        String html = templateEngine.process(templateName, context);
        boolean sent = emailSender.sendHtmlEmailBlocking(to, subject, html);
        if (!sent) {
            throw new RuntimeException("Email delivery returned false for recipient: " + to);
        }
    }
}
