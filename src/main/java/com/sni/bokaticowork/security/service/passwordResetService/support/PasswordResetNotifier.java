package com.sni.bokaticowork.security.service.passwordResetService.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetNotifier {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Brazzaville"));

    @Async
    public void sendPasswordChangedConfirmation(String userEmail) {
        try {
            Context ctx = new Context();
            ctx.setVariable("userEmail", userEmail);
            ctx.setVariable("changedAt", DATE_FMT.format(ZonedDateTime.now()));
            String html = templateEngine.process("email/password-changed-confirmation", ctx);
            emailSender.sendHtmlEmail(userEmail, "Votre mot de passe a été modifié", html);
            log.info("Password-changed confirmation email sent to {}", userEmail);
        } catch (MessagingException ex) {
            log.warn("Could not send password-changed confirmation to {}: {}", userEmail, ex.getMessage());
        }
    }

    @Async
    public void notifyAdminResetCompleted(String adminEmail, String userEmail) {
        if (!StringUtils.hasText(adminEmail)) {
            return;
        }
        try {
            notificationService.send(new SendNotificationRequest(
                    "PASSWORD_RESET_COMPLETED",
                    "USER",
                    userEmail,
                    NotificationChannel.IN_APP,
                    NotificationRecipientType.ADMIN,
                    null,
                    adminEmail,
                    null,
                    "Réinitialisation de mot de passe effectuée",
                    null,
                    null,
                    Map.of(
                            "userEmail", userEmail,
                            "message", "L'utilisateur " + userEmail + " a réinitialisé son mot de passe avec succès."
                    ),
                    null
            ));
            log.info("IN_APP notification sent to admin {} — user {} completed password reset", adminEmail, userEmail);
        } catch (Exception ex) {
            log.warn("Could not send IN_APP notification to admin {}: {}", adminEmail, ex.getMessage());
        }
    }
}
