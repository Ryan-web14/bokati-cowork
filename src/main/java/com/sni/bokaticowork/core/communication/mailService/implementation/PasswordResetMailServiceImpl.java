package com.sni.bokaticowork.core.communication.mailService.implementation;


import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.interfaces.PasswordResetMailService;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.model.PasswordResetToken;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class PasswordResetMailServiceImpl implements PasswordResetMailService {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine emailTemplteEngine;

    @Value("${app.api-base-url:}")
    private String apiBaseUrl;

    private static final String FORM_PATH = "/sni/api/v1/auth/password-reset/form?token=";

    @Async
    @Override
    public CompletableFuture<Boolean> sendPasswordResetMail(Users user, PasswordResetToken resetToken) {
        String firstname = user.getEmail();
        String link = apiBaseUrl + FORM_PATH + resetToken.getPasswordToken();

        long totalMinutes = resetToken.getExpiration() / (1000 * 60);
        String expiryLabel = totalMinutes >= 60
                ? (totalMinutes / 60) + " heure" + (totalMinutes / 60 > 1 ? "s" : "")
                : totalMinutes + " min";

        Context context = new Context();
        context.setVariable("firstname", firstname);
        context.setVariable("resetLink", link);
        context.setVariable("expiryMinute", expiryLabel);

        String template = emailTemplteEngine.process("password_reset", context);

        try{
            String subject = "Réinitialisation de mot de passe";
            log.info("Password reset mail sent");
            return emailSender.sendHtmlEmail(user.getEmail(), subject, template);
        }catch(MessagingException ex){
            log.error("Could not send password reset mail to {}", user.getEmail());
            return CompletableFuture.completedFuture(false);
        }
    }
}
