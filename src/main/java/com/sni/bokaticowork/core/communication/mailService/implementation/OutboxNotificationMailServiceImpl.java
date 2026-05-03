package com.sni.bokaticowork.core.communication.mailService.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OutboxNotificationMailService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.service.support.DocumentFileReader;
import jakarta.mail.MessagingException;
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
        return sendHtml(to, "Notification document", "document-event", variables);
    }

    @Override
    public CompletableFuture<Boolean> sendKycNotification(String to, Map<String, Object> variables) {
        return sendHtml(to, "Notification KYC", "kyc-event", variables);
    }

    @Override
    public CompletableFuture<Boolean> sendContractNotification(String to, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        String html = templateEngine.process("contract-event", context);
        String documentCode = variables.get("documentCode") == null ? null : variables.get("documentCode").toString();

        if (!StringUtils.hasText(documentCode)) {
            return sendHtml(to, "Notification contrat", "contract-event", variables);
        }

        try {
            DocumentFileResult document = documentFileReader.read(documentCode);
            return emailSender.sendHtmlEmailWithPdfAttachment(
                    to,
                    "Notification contrat",
                    html,
                    StringUtils.hasText(document.fileName()) ? document.fileName() : documentCode + ".pdf",
                    document.content()
            );
        } catch (Exception ex) {
            log.error("Failed to send contract notification with attachment to {}", to, ex);
            return CompletableFuture.completedFuture(false);
        }
    }

    private CompletableFuture<Boolean> sendHtml(String to,
                                                String subject,
                                                String templateName,
                                                Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        String html = templateEngine.process(templateName, context);

        try {
            return emailSender.sendHtmlEmail(to, subject, html);
        } catch (MessagingException ex) {
            log.error("Failed to send outbox notification email to {}", to, ex);
            return CompletableFuture.completedFuture(false);
        }
    }
}
