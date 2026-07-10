package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

/**
 * Sends cash-register supervisor/cashier notification emails using the shared
 * "generic-notification" Thymeleaf template · mirrors the lightweight notifier
 * pattern used across other features (KYC, contract, support, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserRepository userRepository;

    @Value("${bokati.payment.cash-session.supervisor-email:${bokati.support.manager-email:supportela@elleaose.com}}")
    private String supervisorFallbackEmail;

    @Value("${app.admin-access-url:https://admin.elleaose.com}")
    private String adminAccessUrl;

    public void notifyUser(String username, String subject, String message, String reference, String actionPath) {
        send(resolveEmail(username), recipientName(username), subject, message, reference, actionPath);
    }

    public void notifySupervisor(String subject, String message, String reference, String actionPath) {
        send(supervisorFallbackEmail, "responsable", subject, message, reference, actionPath);
    }

    public void notifyRegisterManager(CashRegister register, String subject, String message, String reference, String actionPath) {
        String to = StringUtils.hasText(register.getManagerEmail()) ? register.getManagerEmail() : supervisorFallbackEmail;
        String recipientName = StringUtils.hasText(register.getManagerEmail()) ? register.getName() : "responsable";
        send(to, recipientName, subject, message, reference, actionPath);
    }

    private void send(String to, String recipientName, String subject, String message, String reference, String actionPath) {
        if (!StringUtils.hasText(to)) {
            return;
        }
        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("subject", subject);
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("message", message);
            ctx.setVariable("reference", reference);
            ctx.setVariable("actionUrl", StringUtils.hasText(actionPath) ? adminAccessUrl.stripTrailing() + actionPath : adminAccessUrl);
            String html = templateEngine.process("email/generic-notification", ctx);
            emailSender.sendHtmlEmail(to, subject, html);
        } catch (MessagingException ex) {
            log.warn("Failed to queue cash register notification email '{}' to {}", subject, to, ex);
        }
    }

    private String resolveEmail(String username) {
        if (!StringUtils.hasText(username)) {
            return supervisorFallbackEmail;
        }
        return userRepository.findByUserId(username.trim())
                .map(com.sni.bokaticowork.security.admin.user.model.Users::getEmail)
                .filter(StringUtils::hasText)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(username.trim())
                        .map(com.sni.bokaticowork.security.admin.user.model.Users::getEmail)
                        .orElse(supervisorFallbackEmail));
    }

    private String recipientName(String username) {
        if (!StringUtils.hasText(username)) {
            return "responsable";
        }
        return username.trim();
    }
}
