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

//    @Value("${frontend.base.url}")
    private String frontendBaseURl;

    @Async
    @Override
    public CompletableFuture<Boolean> sendPasswordResetMail(Users user, PasswordResetToken resetToken) {
        String firstname = user.getEmail();
        String link = frontendBaseURl + "/reset-password?token=" + resetToken.getPasswordToken();

        Context context = new Context();
        context.setVariable("firstname", firstname);
        context.setVariable("resetLink", link);
        context.setVariable("expiryMinute", resetToken.getExpiration()/(1000*60));

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
