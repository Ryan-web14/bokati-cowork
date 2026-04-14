package com.sni.bokaticowork.core.communication.mailService.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OutboxNotificationMailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return sendHtml(to, "Notification contrat", "contract-event", variables);
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
